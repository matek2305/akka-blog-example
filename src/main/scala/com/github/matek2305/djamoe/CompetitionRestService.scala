package com.github.matek2305.djamoe

import java.util.concurrent.TimeUnit

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.server.{Directive1, Directives, Route}
import akka.pattern.ask
import akka.util.Timeout
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.github.matek2305.djamoe.CompetitionAggregate._
import com.github.matek2305.djamoe.CompetitionRestService._
import org.mindrot.jbcrypt.BCrypt

import scala.concurrent.Future
import scala.concurrent.duration._

class CompetitionRestService(val competitionAggregate: ActorRef) extends Directives with JsonSupport {

  private implicit val timeout: Timeout = Timeout(5.seconds)

  private val tokenExpiryPeriodInDays = 1
  private val secretKey = "super_secret_key"
  private val header = JwtHeader("HS256")

  private val users = Map(
    "user1" -> BCrypt.hashpw("user1", BCrypt.gensalt()),
    "user2" -> BCrypt.hashpw("user2", BCrypt.gensalt()),
    "user3" -> BCrypt.hashpw("user3", BCrypt.gensalt()),
    "user4" -> BCrypt.hashpw("user4", BCrypt.gensalt()),
    "user5" -> BCrypt.hashpw("user5", BCrypt.gensalt()),
  )

  val route: Route = {
    logRequestResult("competition-api") {
      pathPrefix("login") {
        (post & pathEndOrSingleSlash & entity(as[LoginRequest])) { request =>
          if (users.contains(request.username) && BCrypt.checkpw(request.password, users(request.username))) {
            respondWithHeader(RawHeader("Access-Token", JsonWebToken(header, setClaims(request.username), secretKey))) {
              complete(StatusCodes.OK)
            }
          } else {
            complete(StatusCodes.Unauthorized -> "Invalid credentials")
          }
        }
      } ~
        (pathPrefix("secured") & get & pathEndOrSingleSlash & authenticated) { claims =>
            complete(StatusCodes.OK, s"Secured content for ${claims.getOrElse("user", "")}")
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

  private def setClaims(username: String) = JwtClaimsSet(
    Map(
      "user" -> username,
      "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(tokenExpiryPeriodInDays))
    )
  )

  private def authenticated: Directive1[Map[String, Any]] =
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(jwt) if isTokenExpired(jwt) =>
        complete(StatusCodes.Unauthorized -> "Token expired.")
      case Some(jwt) if JsonWebToken.validate(jwt, secretKey) =>
        provide(getClaims(jwt).getOrElse(Map.empty[String, Any]))
      case _ => complete(StatusCodes.Unauthorized)
    }

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get("expiredAt") match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None => false
      }
    case None => false
  }

  private def getClaims(jwt: String) = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _ => None
  }
}

object CompetitionRestService {

  final case class LoginRequest(username: String, password: String)

  final case class GetPointsResponse(players: List[PlayerPoints])

  final case class GetMatchesResponse(matches: List[MatchResponse])

  final case class MatchResponse(id: MatchId, details: Match)

  final case class PlayerPoints(name: String, points: Int)

}