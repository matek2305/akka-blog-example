package com.github.matek2305.djamoe.restapi

import com.github.matek2305.djamoe.domain.{Match, MatchId}
import com.github.matek2305.djamoe.restapi.RestApiResponse.{BetEntry, MatchResponse}

object MatchResponseFactory {

  def create(matchId: MatchId, from: Match, username: String): MatchResponse = from match {
    case created: Match if created.status == Match.CREATED => withoutBets(matchId, created, username)
    case other: Match => withoutBets(matchId, other, username)
      .copy(
        otherBets = other.bets
          .filterKeys(_ != username)
          .map {
            case (player, bet) => BetEntry(player, bet.score, bet.points)
          }
          .toList
      )
  }

  def withoutBets(matchId: MatchId, from: Match, username: String): MatchResponse = {
    MatchResponse(
      matchId,
      from.status.toString,
      from.homeTeamName,
      from.awayTeamName,
      from.startDate,
      from.result,
      from.bets.get(username).map(_.score),
      from.bets.get(username).map(_.points).getOrElse(0)
    )
  }
}
