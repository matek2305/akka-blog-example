package com.github.matek2305.djamoe.domain

final case class Score(homeTeam: Int, awayTeam: Int) {

  def isDraw: Boolean = homeTeam == awayTeam
  def homeTeamWin: Boolean = homeTeam > awayTeam
  def awayTeamWin: Boolean = awayTeam > homeTeam

}
