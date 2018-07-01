package com.github.matek2305.djamoe

final case class CompetitionState(matches: Map[MatchId, MatchState]) {

  def apply(): List[MatchState] = matches.values.toList

  def table: Map[String, Int] = matches.values
    .map(m => m.extractPoints)
    .reduce((a, b) => CombinePointMaps.combine(a, b))

  def add(id: MatchId, details: Match): CompetitionState =
    CompetitionState(matches.updated(id, MatchState(details)))

  def finishMatch(id: MatchId, score: MatchScore): CompetitionState =
    CompetitionState(matches.updated(id, matches(id).finish(score)))

  def addBet(id: MatchId, bet: Bet): CompetitionState =
    CompetitionState(matches.updated(id, matches(id).addBet(bet)))

  def lockBetting(id: MatchId): CompetitionState =
    CompetitionState(matches.updated(id, matches(id).lockBetting))

}

object CompetitionState {
  def apply(): CompetitionState = CompetitionState(Map.empty)
}
