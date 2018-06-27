package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}
import java.util.UUID

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.matek2305.djamoe.CompetitionAggregate._
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

    val score = MatchScore(0, 3)

    "create match and preserve state after restart" in {
      val competitionId = UUID.randomUUID().toString
      val competitionActor = system.actorOf(CompetitionAggregate.props(competitionId))

      competitionActor ! CreateMatch(matchDetails)
      val created = expectMsgType[MatchCreated]
      assert(created.details == matchDetails)

      competitionActor ! PoisonPill

      val restored = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches
      expectMsg(List(MatchState(matchDetails)))
    }

    "finish match and preserve state after restart" in {
      val competitionId = UUID.randomUUID().toString
      val competitionActor = system.actorOf(CompetitionAggregate.props(competitionId))

      competitionActor ! CreateMatch(matchDetails)
      val created = expectMsgType[MatchCreated]

      competitionActor ! FinishMatch(created.id, score)
      val finished = expectMsgType[MatchFinished]

      assert(finished.id == created.id)
      assert(finished.score == score)

      competitionActor ! PoisonPill

      val restored = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches
      expectMsg(List(MatchState(matchDetails, score)))
    }
  }

}
