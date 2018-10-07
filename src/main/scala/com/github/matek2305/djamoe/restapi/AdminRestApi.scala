package com.github.matek2305.djamoe.restapi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.directives.Credentials
import com.github.matek2305.djamoe.app.CompetitionService
import com.github.matek2305.djamoe.domain.CompetitionCommand.AddMatch
import com.github.matek2305.djamoe.domain.{MatchId, Score}
import com.github.matek2305.djamoe.restapi.RestApiResponse.{GetMatchesResponse, MatchResponse}
import com.typesafe.config.Config

trait AdminRestApi
  extends CompetitionService
    with SprayJsonSupport
    with Protocols {

  def config: Config

  def adminAuthenticator(credentials: Credentials): Option[String] =
    credentials match {
      case p@Credentials.Provided(id) if p.verify(config.getString("auth.admin-password")) => Some(id)
      case _ => None
    }

  val adminRoutes: Route = {
    authenticateBasic("admin-realm", adminAuthenticator) { _ =>
      pathPrefix("admin" / "matches") {
        get {
          onSuccess(allMatches) { matchesMap =>
            val matches = matchesMap
              .map {
                case (id, entry) => MatchResponse(
                  id,
                  entry.status.toString,
                  entry.homeTeamName,
                  entry.awayTeamName,
                  entry.startDate,
                  entry.result
                )
              }
              .toList

            complete(StatusCodes.OK -> GetMatchesResponse(matches))
          }
        } ~
          post {
            (pathEndOrSingleSlash & entity(as[AddMatch])) { command =>
              onSuccess(addMatch(command)) { added => complete(StatusCodes.Created -> added) }
            } ~
              pathPrefix(JavaUUID.map(MatchId(_))) { matchId =>
                (pathPrefix("results") & entity(as[Score])) { score =>
                  onSuccess(finishMatch(matchId, score)) { finished => complete(StatusCodes.OK -> finished) }
                } ~
                  pathPrefix("locks") {
                    onSuccess(lockBetting(matchId)) { _ => complete(StatusCodes.OK) }
                  }
              }
          }
      }
    }
  }
}
