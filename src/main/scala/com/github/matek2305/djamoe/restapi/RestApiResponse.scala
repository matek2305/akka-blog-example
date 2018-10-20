package com.github.matek2305.djamoe.restapi

import java.time.LocalDateTime

import com.github.matek2305.djamoe.domain.{MatchId, Score}

sealed trait RestApiResponse

object RestApiResponse {

  final case class BetEntry(player: String, bet: Score, points: Int)
    extends RestApiResponse

  final case class LoginResponse(accessToken: String)
    extends RestApiResponse

  final case class MatchResponse(
      id: MatchId,
      status: String,
      homeTeamName: String,
      awayTeamName: String,
      startDate: LocalDateTime,
      result: Option[Score] = None,
      bet: Option[Score] = None,
      points: Int = 0,
      otherBets: List[BetEntry] = List.empty
  )
    extends RestApiResponse

  final case class GetMatchResponse(data: MatchResponse)
    extends RestApiResponse

  final case class GetMatchesResponse(data: List[MatchResponse])
    extends RestApiResponse

  final case class PlayerPoints(name: String, points: Int)
    extends RestApiResponse

  final case class GetPointsResponse(data: List[PlayerPoints])
    extends RestApiResponse

}
