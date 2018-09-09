package com.github.matek2305.djamoe.domain

import java.util.UUID

class MatchId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

object MatchId {
  def apply(): MatchId = new MatchId(UUID.randomUUID())
  def apply(id: UUID): MatchId = new MatchId(id)
}


