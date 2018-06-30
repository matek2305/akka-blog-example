package com.github.matek2305.djamoe

final case class MatchState(details: Match, score: MatchScore, bets: Map[String, BetState]) {

  def finish(score: MatchScore): MatchState = copy(
    score = score,
    bets = bets map { case (who, bet) =>
      (who, BetState(bet.score, calculatePoints(bet.score, score)))
    }
  )

  def addBet(bet: Bet): MatchState = copy(bets = bets.updated(bet.who, BetState(bet.score)))

  def extractPoints: Map[String, Int] = bets map { case (k, v) => k -> v.points }

  private def calculatePoints(bet: MatchScore, score: MatchScore): Int = {
    if (bet == score) {
      Points.EXACT_BET
    } else if (bet.isDraw && score.isDraw) {
      Points.DRAW
    } else if (bet.homeTeamWin && score.homeTeamWin) {
      Points.HOME_TEAM_WIN
    } else if (bet.awayTeamWin && score.awayTeamWin) {
      Points.AWAY_TEAM_WIN
    } else {
      Points.MISSED_BET
    }
  }

  private object Points {
    val EXACT_BET = 5
    val DRAW = 2
    val HOME_TEAM_WIN = 2
    val AWAY_TEAM_WIN = 2
    val MISSED_BET = 0
  }
}

object MatchState {
  def apply(details: Match): MatchState = MatchState(details, null, Map.empty)
}
