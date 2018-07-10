package com.github.matek2305.djamoe

final case class MatchScore(homeTeam: Int, awayTeam: Int) {

  def isDraw: Boolean = homeTeam == awayTeam
  def homeTeamWin: Boolean = homeTeam > awayTeam
  def awayTeamWin: Boolean = awayTeam > homeTeam

}
