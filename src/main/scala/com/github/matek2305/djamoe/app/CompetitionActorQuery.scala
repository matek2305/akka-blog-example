package com.github.matek2305.djamoe.app

import com.github.matek2305.djamoe.domain.MatchId

sealed trait CompetitionActorQuery

object CompetitionActorQuery {
  final case class GetMatch(id: MatchId) extends CompetitionActorQuery
  final case object GetAllMatches extends CompetitionActorQuery
  final case object GetPoints extends CompetitionActorQuery
}
