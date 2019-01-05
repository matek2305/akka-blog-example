package com.github.matek2305.djamoe.domain

import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, MakeBet}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchAdded, MatchFinished}

import scala.util.{Failure, Success, Try}

final case class Competition(matches: Map[MatchId, Match], makeBetPolicy: MakeBetPolicy) {

  def pointsMap: Map[String, Int] = {
    matches.values
      .map(m => m.getPointsMap())
      .reduceOption((a, b) => Competition.combinePoints(a, b))
      .getOrElse(Map.empty)
  }

  def process(competitionCommand: CompetitionCommand): Try[CompetitionEvent] = competitionCommand match {
    case command: AddMatch => addMatch(command)
    case command: MakeBet => makeBet(command)
    case command: FinishMatch => finishMatch(command)
  }

  def apply(event: CompetitionEvent): Try[Competition] = event match {
    case matchAddedEvent: MatchAdded =>
      Success(Competition(matches.updated(matchAddedEvent.matchId, Match.from(matchAddedEvent)), makeBetPolicy))

    case BetMade(matchId, who, bet) =>
      Success(Competition(matches.updated(matchId, matches(matchId).addBet(who, bet)), makeBetPolicy))

    case MatchFinished(matchId, result) =>
      Success(Competition(matches.updated(matchId, matches(matchId).finish(result)), makeBetPolicy))
  }

  private def addMatch(command: AddMatch): Try[CompetitionEvent] = {
    Success(command.toMatchAdded(MatchId()))
  }

  private def makeBet(command: MakeBet): Try[CompetitionEvent] = matches(command.matchId).status match {
    case Match.FINISHED => Failure(new IllegalStateException(s"Match with id=${command.matchId} have already finished."))
    case Match.CREATED if makeBetPolicy.check(command) => Success(command.toBetMade())
    case Match.CREATED => Failure(new IllegalStateException(s"Betting policy violated for match with id=${command.matchId}"))
  }

  private def finishMatch(command: FinishMatch): Try[CompetitionEvent] = {
    Success(command.toMatchFinished())
  }
}

object Competition {

  type PointsMap = Map[String, Int]

  def apply(makeBetPolicy: MakeBetPolicy): Competition = Competition(Map.empty, makeBetPolicy)

  def combinePoints(x: PointsMap, y: PointsMap): PointsMap = {
    val keys = x.keys.toSet.union(y.keys.toSet)

    val xDef = x.withDefaultValue(0)
    val yDef = y.withDefaultValue(0)

    keys
      .map { k => k -> (xDef(k) + yDef(k)) }
      .toMap
  }
}


