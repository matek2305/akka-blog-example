package com.github.matek2305.djamoe.domain

import com.github.matek2305.djamoe.CombinePointMaps
import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, LockBetting, MakeBet}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, BettingLocked, MatchAdded, MatchFinished}

import scala.util.{Failure, Success, Try}

final case class Competition(matches: Map[MatchId, Match]) {

  def pointsMap: Map[String, Int] = {
    matches.values
      .map(m => m.getPointsMap())
      .reduceOption((a, b) => CombinePointMaps.combine(a, b))
      .getOrElse(Map.empty)
  }

  def process(competitionCommand: CompetitionCommand): Try[CompetitionEvent] = competitionCommand match {
    case command: AddMatch => addMatch(command)
    case command: MakeBet => makeBet(command)
    case command: LockBetting => lockBetting(command)
    case command: FinishMatch => finishMatch(command)
  }

  def apply(event: CompetitionEvent): Try[Competition] = event match {
    case matchAddedEvent: MatchAdded =>
      Success(Competition(matches.updated(matchAddedEvent.matchId, Match.from(matchAddedEvent))))

    case BetMade(matchId, who, bet) =>
      Success(Competition(matches.updated(matchId, matches(matchId).addBet(who, bet))))

    case BettingLocked(matchId) =>
      Success(Competition(matches.updated(matchId, matches(matchId).lockBetting())))

    case MatchFinished(matchId, result) =>
      Success(Competition(matches.updated(matchId, matches(matchId).finish(result))))
  }

  private def addMatch(command: AddMatch): Try[CompetitionEvent] = {
    Success(command.toMatchAdded(MatchId()))
  }

  private def makeBet(command: MakeBet): Try[CompetitionEvent] = matches(command.matchId).status match {
    case Match.LOCKED => Failure(new IllegalStateException(s"Betting for match with id=${command.matchId} already locked."))
    case Match.FINISHED => Failure(new IllegalStateException(s"Match with id=${command.matchId} have already finished."))
    case Match.CREATED => Success(command.toBetMade())
  }

  private def lockBetting(command: LockBetting): Try[CompetitionEvent] = {
    Success(command.toBettingLocked())
  }

  private def finishMatch(command: FinishMatch): Try[CompetitionEvent] = {
    Success(command.toMatchFinished())
  }
}

object Competition {
  def apply(): Competition = Competition(Map.empty)
}


