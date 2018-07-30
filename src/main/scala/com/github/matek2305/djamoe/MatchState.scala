package com.github.matek2305.djamoe

final case class MatchState(details: Match, score: MatchScore, bets: Map[String, BetState], status: MatchState.Status) {

  def finish(score: MatchScore): MatchState = copy(
    score = score,
    bets = bets map { case (who, bet) =>
      (who, BetState(bet.score, calculatePoints(bet.score, score)))
    },
    status = MatchState.FINISHED
  )

  def addBet(bet: Bet): MatchState = copy(bets = bets.updated(bet.who, BetState(bet.score)))

  def lockBetting: MatchState = copy(status = MatchState.LOCKED)

  def extractPoints: Map[String, Int] = bets map { case (k, v) => k -> v.points }

  private def calculatePoints(bet: MatchScore, score: MatchScore): Int = (bet, score) match {
    case (bet, score) if bet == score => Points.EXACT_BET
    case (bet, score) if bet.isDraw && score.isDraw => Points.DRAW
    case (bet, score) if bet.homeTeamWin && score.homeTeamWin => Points.HOME_TEAM_WIN
    case (bet, score) if bet.awayTeamWin && score.awayTeamWin => Points.AWAY_TEAM_WIN
    case _ => Points.MISSED_BET
  }

  private object Points {
    val EXACT_BET = 5
    val DRAW = 2
    val HOME_TEAM_WIN = 2
    val AWAY_TEAM_WIN = 2
    val MISSED_BET = 0
  }
}

object MatchState extends Enumeration {
  type Status = Value
  val CREATED, LOCKED, FINISHED = Value
  def apply(details: Match): MatchState = MatchState(details, null, Map.empty, CREATED)
}
