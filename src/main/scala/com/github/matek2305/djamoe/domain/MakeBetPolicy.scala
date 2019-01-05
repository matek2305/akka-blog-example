package com.github.matek2305.djamoe.domain

import com.github.matek2305.djamoe.domain.CompetitionCommand.MakeBet

sealed trait MakeBetPolicy {
  def check(makeBet: MakeBet): Boolean
}

object MakeBetPolicy {
  final class LockBettingBeforeMatchStart extends MakeBetPolicy {
    override def check(makeBet: MakeBet): Boolean = true
  }
}


