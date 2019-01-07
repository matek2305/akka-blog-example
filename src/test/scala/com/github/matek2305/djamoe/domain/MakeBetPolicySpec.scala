package com.github.matek2305.djamoe.domain

import java.time.LocalDateTime.parse

import com.github.matek2305.djamoe.domain.MakeBetPolicy.LockBettingBeforeMatchStart
import org.mockito.{ArgumentMatchersSugar, MockitoSugar}
import org.scalatest.FlatSpec

class MakeBetPolicySpec extends FlatSpec with MockitoSugar with ArgumentMatchersSugar {

  val game = Match("Liverpool", "Bayern", parse("2019-02-19T21:00:00"))
  val lockTimeInMinutes = 60

  implicit val timeProvider: TimeProvider = mock[TimeProvider]

  val makeBetPolicy = new LockBettingBeforeMatchStart(lockTimeInMinutes)

  "make bet policy" should "prevent betting when match starts within lock time" in {
    val currentTime = game.startDate.minusMinutes(lockTimeInMinutes).plusMinutes(5)
    when(timeProvider.getCurrentTime) thenReturn currentTime
    assert(!makeBetPolicy.check(game))
  }

  it should "allow betting when match is not starting within lock time" in {
    val currentTime = game.startDate.minusMinutes(lockTimeInMinutes).minusMinutes(5)
    when(timeProvider.getCurrentTime) thenReturn currentTime
    assert(makeBetPolicy.check(game))
  }

  it should "allow betting when match is starting exactly within lock time" in {
    val currentTime = game.startDate.minusMinutes(lockTimeInMinutes)
    when(timeProvider.getCurrentTime) thenReturn currentTime
    assert(makeBetPolicy.check(game))
  }

}
