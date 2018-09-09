package com.github.matek2305.djamoe.domain

final case class Bet(score: Score, points: Int = 0)

object Bet {
  def apply(score: Score): Bet = new Bet(score)
}
