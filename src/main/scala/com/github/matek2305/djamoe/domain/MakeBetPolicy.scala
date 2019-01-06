package com.github.matek2305.djamoe.domain

trait MakeBetPolicy {
  def check(game: Match): Boolean
}

object MakeBetPolicy {
  final class LockBettingBeforeMatchStart(howManyMinutesBefore: Int)(implicit timeProvider: TimeProvider) extends MakeBetPolicy {
    override def check(game: Match): Boolean = {
      game.startDate.minusMinutes(howManyMinutesBefore).isAfter(timeProvider.getCurrentTime)
    }
  }
}


