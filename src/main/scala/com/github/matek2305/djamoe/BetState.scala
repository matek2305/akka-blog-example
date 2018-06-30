package com.github.matek2305.djamoe

final case class BetState(score: MatchScore, points: Int)

object BetState {
  def apply(score: MatchScore): BetState = new BetState(score, 0)
}
