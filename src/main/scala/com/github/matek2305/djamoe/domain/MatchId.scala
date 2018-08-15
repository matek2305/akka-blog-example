package com.github.matek2305.djamoe.domain

import java.util.UUID

class MatchId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}


