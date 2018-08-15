package com.github.matek2305.djamoe.app

sealed trait CompetitionActorQuery

object CompetitionActorQuery {
  final case class GetAllMatches() extends CompetitionActorQuery
  final case class GetPoints() extends CompetitionActorQuery
}
