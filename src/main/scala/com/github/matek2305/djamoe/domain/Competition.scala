package com.github.matek2305.djamoe.domain

import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, LockBetting, MakeBet}

import scala.util.Try

final case class Competition(matches: Map[MatchId, Match]) {

  def process(competitionCommand: CompetitionCommand): Try[List[CompetitionEvent]] = competitionCommand match {
    case command: AddMatch => addMatch(command)
    case command: MakeBet => makeBet(command)
    case command: LockBetting => lockBetting(command)
    case command: FinishMatch => finishMatch(command)
  }

  def apply(event: CompetitionEvent): Try[Competition] = event match {

  }

  private def addMatch(command: AddMatch): Try[List[CompetitionEvent]] = {
    
  }


  //  def apply(): Map[MatchId, MatchState] = matches
//
//  def table: Map[String, Int] = matches.values
//    .map(m => m.extractPoints)
//    .reduceOption((a, b) => CombinePointMaps.combine(a, b))
//    .getOrElse(Map.empty)
//
//  def add(id: MatchId, details: Match): CompetitionState =
//    CompetitionState(matches.updated(id, MatchState(details)))
//
//  def finishMatch(id: MatchId, score: MatchScore): CompetitionState =
//    CompetitionState(matches.updated(id, matches(id).finish(score)))
//
//  def addBet(id: MatchId, bet: Bet): CompetitionState =
//    CompetitionState(matches.updated(id, matches(id).addBet(bet)))
//
//  def lockBetting(id: MatchId): CompetitionState =
//    CompetitionState(matches.updated(id, matches(id).lockBetting))

}

object Competition {
  def apply(): Competition = Competition(Map.empty)
}


