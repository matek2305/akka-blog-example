package com.github.matek2305.djamoe.domain

import java.time.LocalDateTime

sealed trait CompetitionCommand

object CompetitionCommand {

  final case class AddMatch(homeTeamName: String, awayTeamName: String, startDate: LocalDateTime) extends CompetitionCommand

  final case class FinishMatch(id: MatchId, result: Score) extends CompetitionCommand

  final case class LockBetting(id: MatchId) extends CompetitionCommand

  final case class MakeBet(id: MatchId, who: String, bet: Score) extends CompetitionCommand

}
