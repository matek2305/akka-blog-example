package com.github.matek2305.djamoe.app

sealed trait CompetitionActorQuery

object CompetitionActorQuery {
  final case object GetAllMatches extends CompetitionActorQuery
  final case object GetPoints extends CompetitionActorQuery
}
