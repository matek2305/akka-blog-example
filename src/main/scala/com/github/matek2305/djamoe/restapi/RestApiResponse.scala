package com.github.matek2305.djamoe.restapi

import java.time.LocalDateTime

import com.github.matek2305.djamoe.domain.{MatchId, Score}

sealed trait RestApiResponse

object RestApiResponse {

  final case class LoginResponse(accessToken: String)
    extends RestApiResponse

  final case class MatchResponse(
      id: MatchId,
      status: String,
      homeTeamName: String,
      awayTeamName: String,
      startDate: LocalDateTime,
      result: Option[Score],
      bet: Option[Score],
      points: Int
  )
    extends RestApiResponse

  final case class GetMatchResponse(`match`: MatchResponse)
    extends RestApiResponse

  final case class GetMatchesResponse(matches: List[MatchResponse])
    extends RestApiResponse

  final case class PlayerPoints(name: String, points: Int)
    extends RestApiResponse

  final case class GetPointsResponse(players: List[PlayerPoints])
    extends RestApiResponse

}
