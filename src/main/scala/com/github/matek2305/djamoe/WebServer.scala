package com.github.matek2305.djamoe

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.github.matek2305.djamoe.CompetitionAggregate.{CreateMatch, GetAllMatches, MatchCreated}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

import scala.concurrent.Future
import scala.concurrent.duration._

// collect your json format instances into a support trait:
trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object LocalDateTimeJsonFormat extends RootJsonFormat[LocalDateTime] {

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(str) => LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
      case _ => throw DeserializationException("Expected ISO-8601 date time format")
    }

    override def write(localDateTime: LocalDateTime): JsValue =
      JsString(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME))
  }

  implicit object MatchIdJsonFormat extends RootJsonFormat[MatchId] {

    override def read(json: JsValue): MatchId = json match {
      case JsString(str) => MatchId(UUID.fromString(str))
      case _ => throw DeserializationException("Expected UUID string")
    }

    override def write(matchId: MatchId): JsValue = JsString(matchId.toString)
  }

  implicit val matchFormat: RootJsonFormat[Match] = jsonFormat3(Match)
  implicit val matchCreatedFormat: RootJsonFormat[MatchCreated] = jsonFormat2(MatchCreated)
}

// use it wherever json (un)marshalling is needed
class MyJsonService(val system: ActorSystem) extends Directives with JsonSupport {

  private val competitionAggregate = system.actorOf(CompetitionAggregate.props("competition-id"))
  private implicit val timeout: Timeout = Timeout(10.seconds)

  val route: Route =
    get {
      pathSingleSlash {
        onSuccess(getMatches) { matches =>
          complete((StatusCodes.OK, matches.map(_.details)))
        }
      }
    } ~
      post {
        entity(as[Match]) { matchDetails =>
          onSuccess(createMatch(matchDetails)) { created =>
            complete((StatusCodes.Created, created))
          }
        }
      }

  private def createMatch(matchDetails: Match): Future[MatchCreated] =
    (competitionAggregate ? CreateMatch(matchDetails)).mapTo[MatchCreated]

  private def getMatches: Future[List[MatchState]] =
    (competitionAggregate ? GetAllMatches()).mapTo[List[MatchState]]
}

object WebServer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val service = new MyJsonService(system)
    Http().bindAndHandle(service.route, "localhost", 8080)
    println("Server online at http://localhost:8080/")
  }
}

