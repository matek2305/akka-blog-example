package com.github.matek2305.djamoe.domain

import java.time.LocalDateTime

final case class Match(homeTeamName: String, awayTeamName: String, startDate: LocalDateTime, result: Score, bets: Map[String, Score]) {

//  def finish(score: MatchScore): MatchState = copy(
//    score = score,
//    bets = bets map { case (who, bet) =>
//      (who, BetState(bet.score, calculatePoints(bet.score, score)))
//    },
//    status = MatchState.FINISHED
//  )
//
//  def addBet(bet: Bet): MatchState = copy(bets = bets.updated(bet.who, BetState(bet.score)))
//
//  def lockBetting: MatchState = copy(status = MatchState.LOCKED)
//
//  def extractPoints: Map[String, Int] = bets map { case (k, v) => k -> v.points }
//
//  private def calculatePoints(bet: MatchScore, score: MatchScore): Int = (bet, score) match {
//    case (bet, score) if bet == score => Points.EXACT_BET
//    case (bet, score) if bet.isDraw && score.isDraw => Points.DRAW
//    case (bet, score) if bet.homeTeamWin && score.homeTeamWin => Points.HOME_TEAM_WIN
//    case (bet, score) if bet.awayTeamWin && score.awayTeamWin => Points.AWAY_TEAM_WIN
//    case _ => Points.MISSED_BET
//  }
//
//  private object Points {
//    val EXACT_BET = 5
//    val DRAW = 2
//    val HOME_TEAM_WIN = 2
//    val AWAY_TEAM_WIN = 2
//    val MISSED_BET = 0
//  }
}

//object Match {
//  def from()
//}


