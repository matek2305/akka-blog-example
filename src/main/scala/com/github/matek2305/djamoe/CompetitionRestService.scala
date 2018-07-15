package com.github.matek2305.djamoe

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.github.matek2305.djamoe.CompetitionAggregate._
import com.github.matek2305.djamoe.CompetitionRestService._

import scala.concurrent.Future
import scala.concurrent.duration._

class CompetitionRestService(val competitionAggregate: ActorRef) extends Directives with JsonSupport {

  private implicit val timeout: Timeout = Timeout(5.seconds)

  private val tokenExpiryPeriodInDays = 1
  private val secretKey = "super_secret_key"
  private val header = JwtHeader("HS256")

  val route: Route = {
    logRequestResult("competition-api") {
      pathPrefix("login") {
        (post & pathEndOrSingleSlash & entity(as[LoginRequest])) {
          case lr@LoginRequest("admin", "admin") =>
            respondWithHeader(RawHeader("Access-Token", JsonWebToken(header, setClaims(lr.username), secretKey))) {
              complete(StatusCodes.OK)
            }
          case LoginRequest(_, _) => complete(StatusCodes.Unauthorized)
        }
      } ~
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

  private def setClaims(username: String) = JwtClaimsSet(
    Map(
      "user" -> username,
      "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(tokenExpiryPeriodInDays))
    )
  )

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

  final case class LoginRequest(username: String, password: String)

  final case class GetPointsResponse(players: List[PlayerPoints])

  final case class GetMatchesResponse(matches: List[MatchResponse])

  final case class MatchResponse(id: MatchId, details: Match)

  final case class PlayerPoints(name: String, points: Int)

}