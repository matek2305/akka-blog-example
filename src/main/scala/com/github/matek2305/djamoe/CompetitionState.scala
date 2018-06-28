package com.github.matek2305.djamoe

/**
  * @author Mateusz Urbański <matek2305@gmail.com>.
  */
final case class CompetitionState(matches: Map[MatchId, MatchState]) {
  def apply(): List[MatchState] = matches.values.toList
  def add(id: MatchId, details: Match): CompetitionState =
    CompetitionState(matches.updated(id, MatchState(details)))
  def finishMatch(id: MatchId, score: MatchScore): CompetitionState =
    CompetitionState(matches.updated(id, matches(id).finish(score)))
  def addBet(id: MatchId, bet: Bet): CompetitionState =
    CompetitionState(matches.updated(id, matches(id).addBet(bet)))
}

object CompetitionState {
  def apply(): CompetitionState = CompetitionState(Map.empty)
}
