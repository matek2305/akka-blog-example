package com.github.matek2305.djamoe.restapi

import java.time.LocalDateTime

import com.github.matek2305.djamoe.domain.{MatchId, Score}

sealed trait CompetitionRestApiResponse

object CompetitionRestApiResponse {

  final case class MatchResponse(id: MatchId, status: String, homeTeamName: String, awayTeamName: String, startDate: LocalDateTime, result: Option[Score])
    extends CompetitionRestApiResponse

  final case class GetMatchesResponse(matches: List[MatchResponse])
    extends CompetitionRestApiResponse

  final case class PlayerPoints(name: String, points: Int)
    extends CompetitionRestApiResponse

  final case class GetPointsResponse(players: List[PlayerPoints])
    extends CompetitionRestApiResponse

}
