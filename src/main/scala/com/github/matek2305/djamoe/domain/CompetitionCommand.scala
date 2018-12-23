package com.github.matek2305.djamoe.domain

import java.time.LocalDateTime

import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchAdded, MatchFinished}

sealed trait CompetitionCommand

object CompetitionCommand {

  final case class AddMatch(homeTeamName: String, awayTeamName: String, startDate: LocalDateTime) extends CompetitionCommand {
    def toMatchAdded(matchId: MatchId): MatchAdded = MatchAdded(matchId, homeTeamName, awayTeamName, startDate)
  }

  final case class FinishMatch(matchId: MatchId, result: Score) extends CompetitionCommand {
    def toMatchFinished(): MatchFinished = MatchFinished(matchId, result)
  }

  final case class MakeBet(matchId: MatchId, who: String, bet: Score) extends CompetitionCommand {
    def toBetMade(): BetMade = BetMade(matchId, who, bet)
  }

}
