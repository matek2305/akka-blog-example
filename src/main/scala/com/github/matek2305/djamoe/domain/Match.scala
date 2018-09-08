package com.github.matek2305.djamoe.domain

import java.time.LocalDateTime

import com.github.matek2305.djamoe.domain.CompetitionEvent.MatchAdded

final case class Match(
  homeTeamName: String,
  awayTeamName: String,
  startDate: LocalDateTime,
  status: Match.Status = Match.CREATED,
  result: Option[Score] = None,
  bets: Map[String, Bet] = Map.empty
) {

  def getPointsMap(): Map[String, Int] = {
    bets map { case (k, v) => k -> v.points }
  }

  def addBet(who: String, bet: Score): Match = copy(bets = bets.updated(who, Bet(bet)))

  def lockBetting(): Match = copy(status = Match.LOCKED)

  def finish(result: Score): Match = copy(
    result = Some(result),
    status = Match.FINISHED,
    bets = bets map {
      case (who, bet) => (who, bet.copy(points = calculatePoints(bet.score, result)))
    }
  )

  private def calculatePoints(bet: Score, score: Score): Int = (bet, score) match {
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

object Match extends Enumeration {
  type Status = Value
  val CREATED, LOCKED, FINISHED = Value
  def from(matchAdded: MatchAdded): Match = {
    Match(matchAdded.homeTeamName, matchAdded.awayTeamName, matchAdded.startDate)
  }
}


