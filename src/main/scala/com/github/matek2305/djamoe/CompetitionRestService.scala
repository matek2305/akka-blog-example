package com.github.matek2305.djamoe

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import com.github.matek2305.djamoe.CompetitionAggregate.{CreateMatch, GetAllMatches, GetPoints, MatchCreated}
import com.github.matek2305.djamoe.CompetitionRestService.{GetPointsResponse, PlayerPoints}

import scala.concurrent.Future
import scala.concurrent.duration._

class CompetitionRestService(val competitionAggregate: ActorRef) extends Directives with JsonSupport {

  private implicit val timeout: Timeout = Timeout(5.seconds)

  val route: Route =
    path("matches") {
      get {
        onSuccess(getMatches) { matches =>
          complete((StatusCodes.OK, matches.map(_.details)))
        }
      } ~
      post {
        entity(as[Match]) { matchDetails =>
          onSuccess(createMatch(matchDetails)) { created =>
            complete((StatusCodes.Created, created))
          }
        }
      }
    } ~
    path("points") {
      get {
        onSuccess(getPoints) { points =>
          complete((StatusCodes.OK, GetPointsResponse(toPlayerPoints(points))))
        }
      }
    }

  private def createMatch(matchDetails: Match): Future[MatchCreated] =
    (competitionAggregate ? CreateMatch(matchDetails)).mapTo[MatchCreated]

  private def getMatches: Future[List[MatchState]] =
    (competitionAggregate ? GetAllMatches()).mapTo[List[MatchState]]

  private def getPoints: Future[Map[String, Int]] =
    (competitionAggregate ? GetPoints()).mapTo[Map[String, Int]]

  private def toPlayerPoints(points: Map[String, Int]): List[PlayerPoints] = {
    points.map { case (k, v) => PlayerPoints(k, v) } toList
  }
}

object CompetitionRestService {
  final case class GetPointsResponse(data: List[PlayerPoints])
  final case class PlayerPoints(playerName: String, points: Int)
}