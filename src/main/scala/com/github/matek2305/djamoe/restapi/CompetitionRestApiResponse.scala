package com.github.matek2305.djamoe.restapi

import java.time.LocalDateTime

import com.github.matek2305.djamoe.domain.MatchId

sealed trait CompetitionRestApiResponse

object CompetitionRestApiResponse {

  final case class MatchResponse(id: MatchId, status: String, homeTeamName: String, awayTeamName: String, startDate: LocalDateTime)
    extends CompetitionRestApiResponse

  final case class GetMatchesResponse(matches: List[MatchResponse])
    extends CompetitionRestApiResponse

  final case class PlayerPoints(name: String, points: Int)
    extends CompetitionRestApiResponse

  final case class GetPointsResponse(players: List[PlayerPoints])
    extends CompetitionRestApiResponse

}
