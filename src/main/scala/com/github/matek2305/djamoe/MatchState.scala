package com.github.matek2305.djamoe

/**
  * @author Mateusz Urbański <matek2305@gmail.com>.
  */
final case class MatchState(details: Match, score: MatchScore, bets: Map[String, BetState]) {
  def finish(score: MatchScore): MatchState = copy(score = score)
  def addBet(bet: Bet): MatchState = copy(bets = bets.updated(bet.who, BetState(bet.score)))
}

object MatchState {
  def apply(details: Match): MatchState = MatchState(details, null, Map.empty)
}

final case class BetState(score: MatchScore, points: Int)

object BetState {
  def apply(score: MatchScore): BetState = new BetState(score, 0)
}
