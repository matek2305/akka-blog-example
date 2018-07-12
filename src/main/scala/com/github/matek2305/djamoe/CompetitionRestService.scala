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

  val route: Route = {
    logRequestResult("competition-api") {
      pathPrefix("matches") {
        (get & pathEndOrSingleSlash) {
          onSuccess(getMatches) { matches =>
            complete((StatusCodes.OK, matches.map(_.details)))
          }
        } ~
          (get & pathPrefix(IntNumber / "bets")) { id =>
            complete(s"bets for match with id=$id")
          } ~
          (post & pathEndOrSingleSlash & entity(as[Match])) { details =>
            onSuccess(createMatch(details)) { created =>
              complete((StatusCodes.Created, created))
            }
          }
      } ~
        pathPrefix("points") {
          (get & pathEndOrSingleSlash) {
            onSuccess(getPoints) { pointsMap =>
              val points = pointsMap
                .map { case (k, v) => PlayerPoints(k, v) }
                .toList

              complete((StatusCodes.OK, GetPointsResponse(points)))
            }
          }
        }
    }
  }

  private def createMatch(matchDetails: Match): Future[MatchCreated] =
    (competitionAggregate ? CreateMatch(matchDetails)).mapTo[MatchCreated]

  private def getMatches: Future[List[MatchState]] =
    (competitionAggregate ? GetAllMatches()).mapTo[List[MatchState]]

  private def getPoints: Future[Map[String, Int]] =
    (competitionAggregate ? GetPoints()).mapTo[Map[String, Int]]
}

object CompetitionRestService {
  final case class GetPointsResponse(data: List[PlayerPoints])
  final case class PlayerPoints(playerName: String, points: Int)
}