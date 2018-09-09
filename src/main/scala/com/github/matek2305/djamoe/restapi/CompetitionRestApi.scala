package com.github.matek2305.djamoe.restapi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.github.matek2305.djamoe.app.CompetitionService
import com.github.matek2305.djamoe.domain.CompetitionCommand.AddMatch
import com.github.matek2305.djamoe.domain.{MatchId, Score}
import com.github.matek2305.djamoe.restapi.CompetitionRestApiResponse.{GetMatchesResponse, GetPointsResponse, MatchResponse, PlayerPoints}
import com.typesafe.config.Config

trait CompetitionRestApi
  extends CompetitionService
    with SprayJsonSupport
    with CompetitionProtocols {

  def config: Config

  val routes: Route = {
    logRequestResult("competition-api") {
      (pathPrefix("matches") & headerValueByName("X-Logged-User")) { loggedUser =>
        (get & pathEndOrSingleSlash) {
          onSuccess(allMatches) { matchesMap =>
            val matches = matchesMap
              .map {
                case (id, entry) => MatchResponse(
                  id,
                  entry.status.toString,
                  entry.homeTeamName,
                  entry.awayTeamName,
                  entry.startDate,
                  entry.result,
                  entry.bets.get(loggedUser).map(_.score),
                  entry.bets.get(loggedUser).map(_.points).getOrElse(0)
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
                  (pathPrefix("bets") & entity(as[Score])) { bet =>
                    onSuccess(makeBet(matchId, loggedUser, bet)) { made => complete(StatusCodes.OK -> made) }
                  }
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
