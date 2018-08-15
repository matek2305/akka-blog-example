package com.github.matek2305.djamoe.app

import com.github.matek2305.djamoe.domain.CompetitionEvent

sealed trait CompetitionActorResponse

object CompetitionActorResponse {
  final case class CommandProcessed(event: CompetitionEvent) extends CompetitionActorResponse
  final case class CommandProcessingFailed(ex: Throwable) extends CompetitionActorResponse
}
