package com.github.matek2305.djamoe

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import com.github.matek2305.djamoe.CompetitionAggregate._
import com.github.matek2305.djamoe.CompetitionRestService.{GetMatchesResponse, GetPointsResponse, MatchResponse, PlayerPoints}

import scala.concurrent.Future
import scala.concurrent.duration._

class CompetitionRestService(val competitionAggregate: ActorRef) extends Directives with JsonSupport {

  private implicit val timeout: Timeout = Timeout(5.seconds)

  val route: Route = {
    logRequestResult("competition-api") {
      pathPrefix("matches") {
        (get & pathEndOrSingleSlash) {
          onSuccess(getMatches) { matchesMap =>
            val matches = matchesMap
                .map { case (k, v) => MatchResponse(k, v.details) }
                .toList

            complete((StatusCodes.OK, GetMatchesResponse(matches)))
          }
        } ~
          post {
            (pathEndOrSingleSlash & entity(as[Match])) { details =>
              onSuccess(createMatch(details)) { created =>
                complete((StatusCodes.Created, created))
              }
            } ~
              pathPrefix(JavaUUID.map(MatchId(_))) { matchId =>
                (pathPrefix("bets") & entity(as[Bet])) { bet =>
                  onSuccess(makeBet(matchId, bet)) { created =>
                    complete((StatusCodes.Created, created))
                  }
                } ~
                  (pathPrefix("score") & entity(as[MatchScore])) { score =>
                    onSuccess(finishMatch(matchId, score)) { created =>
                      complete((StatusCodes.Created, created))
                    }
                  }
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

  private def finishMatch(id: MatchId, score: MatchScore): Future[MatchFinished] =
    (competitionAggregate ? FinishMatch(id, score)).mapTo[MatchFinished]

  private def makeBet(id: MatchId, bet: Bet): Future[BetMade] =
    (competitionAggregate ? MakeBet(id, bet)).mapTo[BetMade]

  private def createMatch(matchDetails: Match): Future[MatchCreated] =
    (competitionAggregate ? CreateMatch(matchDetails)).mapTo[MatchCreated]

  private def getMatches: Future[Map[MatchId, MatchState]] =
    (competitionAggregate ? GetAllMatches()).mapTo[Map[MatchId, MatchState]]

  private def getPoints: Future[Map[String, Int]] =
    (competitionAggregate ? GetPoints()).mapTo[Map[String, Int]]
}

object CompetitionRestService {

  final case class GetPointsResponse(players: List[PlayerPoints])
  final case class GetMatchesResponse(matches: List[MatchResponse])
  final case class MatchResponse(id: MatchId, details: Match)
  final case class PlayerPoints(name: String, points: Int)

}