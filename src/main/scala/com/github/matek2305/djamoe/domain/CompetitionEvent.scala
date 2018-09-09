package com.github.matek2305.djamoe.domain

import java.time.LocalDateTime

sealed trait CompetitionEvent {
  def matchId: MatchId
}

object CompetitionEvent {

  final case class MatchAdded(matchId: MatchId, homeTeamName: String, awayTeamName: String, startDate: LocalDateTime) extends CompetitionEvent

  final case class MatchFinished(matchId: MatchId, result: Score) extends CompetitionEvent

  final case class BetMade(matchId: MatchId, who: String, bet: Score) extends CompetitionEvent

  final case class BettingLocked(matchId: MatchId) extends CompetitionEvent

}
