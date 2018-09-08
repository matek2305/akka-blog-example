package com.github.matek2305.djamoe.app

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.{ActorMaterializer, Materializer}
import akka.util.Timeout
import com.github.matek2305.djamoe.app.CompetitionActorQuery.{GetAllMatches, GetPoints}
import com.github.matek2305.djamoe.domain.{Match, MatchId}
import com.typesafe.config.{Config, ConfigFactory}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

import scala.concurrent.{ExecutionContextExecutor, Future}

final case class MatchResponse(id: MatchId, status: String, homeTeamName: String, awayTeamName: String, startDate: LocalDateTime)

final case class GetMatchesResponse(matches: List[MatchResponse])

final case class PlayerPoints(name: String, points: Int)

final case class GetPointsResponse(players: List[PlayerPoints])

trait Protocols extends SprayJsonSupport with DefaultJsonProtocol {

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

  implicit val matchResponseFormat: RootJsonFormat[MatchResponse] = jsonFormat5(MatchResponse)
  implicit val getMatchesResponseFormat: RootJsonFormat[GetMatchesResponse] = jsonFormat1(GetMatchesResponse)
  implicit val playerPointsFormat: RootJsonFormat[PlayerPoints] = jsonFormat2(PlayerPoints)
  implicit val getPointsResponseFormat: RootJsonFormat[GetPointsResponse] = jsonFormat1(GetPointsResponse)

}

trait Service {

  import akka.pattern.ask
  import scala.concurrent.duration._

  implicit val system: ActorSystem
  implicit val materializer: Materializer

  implicit def executor: ExecutionContextExecutor

  implicit def timeout: Timeout = Timeout(5.seconds)

  def competitionActor: ActorRef

  def allMatches: Future[Map[MatchId, Match]] = {
    (competitionActor ? GetAllMatches).mapTo[Map[MatchId, Match]]
  }

  def points: Future[Map[String, Int]] = {
    (competitionActor ? GetPoints).mapTo[Map[String, Int]]
  }
}

trait RestApi extends Service with Protocols {

  def config: Config

  val routes: Route = {
    logRequestResult("competition-api") {
      pathPrefix("matches") {
        (get & pathEndOrSingleSlash) {
          onSuccess(allMatches) { matchesMap =>
            val matches = matchesMap
              .map { case (k, v) => MatchResponse(k, v.status.toString, v.homeTeamName, v.awayTeamName, v.startDate) }
              .toList

            complete(StatusCodes.OK -> GetMatchesResponse(matches))
          }
        }
      } ~
        pathPrefix("points") {
          (get & pathEndOrSingleSlash) {
            onSuccess(points) { pointsMap =>
              val points = pointsMap
                .map { case (k, v) => PlayerPoints(k, v) }
                .toList

              complete(StatusCodes.OK -> GetPointsResponse(points))
            }
          }
        }
    }
  }

}

object DjamoeBettingWebServer extends App with RestApi {
  override implicit val system: ActorSystem = ActorSystem()
  override implicit val materializer: Materializer = ActorMaterializer()
  override implicit val executor: ExecutionContextExecutor = system.dispatcher

  override def config: Config = ConfigFactory.load()

  override def competitionActor: ActorRef = system.actorOf(CompetitionActor.props("competition-id"))

  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  Http().bindAndHandle(routes, interface, port)
  println(s"Server online at http://$interface:$port/")
}
