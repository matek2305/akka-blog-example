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
    case (bet, score) if bet == score => Points.ExactBet
    case (bet, score) if bet.isDraw && score.isDraw => Points.Draw
    case (bet, score) if bet.homeTeamWin && score.homeTeamWin => Points.HomeTeamWon
    case (bet, score) if bet.awayTeamWin && score.awayTeamWin => Points.AwayTeamWon
    case _ => Points.MissedBet
  }

  private object Points {
    val ExactBet = 5
    val Draw = 2
    val HomeTeamWon = 2
    val AwayTeamWon = 2
    val MissedBet = 0
  }
}

object Match extends Enumeration {
  type Status = Value
  val CREATED, LOCKED, FINISHED = Value
  def from(matchAdded: MatchAdded): Match = {
    Match(matchAdded.homeTeamName, matchAdded.awayTeamName, matchAdded.startDate)
  }
}


