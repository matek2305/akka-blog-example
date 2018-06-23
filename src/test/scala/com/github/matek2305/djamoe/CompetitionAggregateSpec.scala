package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.github.matek2305.djamoe.CompetitionAggregate.{CreateMatch, MatchCreated}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
  * @author Mateusz Urbański <matek2305@gmail.com>.
  */
class CompetitionAggregateSpec
  extends TestKit(ActorSystem("testSystem"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Competition aggregate" should {
    val matchDetails = MatchDetails(
      "Poland",
      "Colombia",
      LocalDateTime.of(2018, Month.JUNE, 24, 20, 0)
    )

    "create match and preserve state after restart" in {
      val competitionId = "c1"
      val competitionActor = system.actorOf(CompetitionAggregate.props(competitionId))

      competitionActor ! CreateMatch(matchDetails)
      val created = expectMsgType[MatchCreated]
      assert(created.details == matchDetails)
    }
  }

}
