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
    val matchDetails = Match(
      "Poland",
      "Colombia",
      LocalDateTime.of(2018, Month.JUNE, 24, 20, 0)
    )

    val score = MatchScore(0, 3)
    val bet = Bet("Shady", MatchScore(2, 1))

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
      expectMsg(MatchFinished(created.id, score))

      competitionActor ! PoisonPill

      val restored = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches
      expectMsg(List(MatchState(matchDetails, score, Map.empty)))
    }

    "add bet and preserve state after restart" in {
      val competitionId = UUID.randomUUID().toString
      val competitionActor = system.actorOf(CompetitionAggregate.props(competitionId))

      competitionActor ! CreateMatch(matchDetails)
      val created = expectMsgType[MatchCreated]

      competitionActor ! MakeBet(created.id, bet)
      expectMsg(BetMade(created.id, bet))

      competitionActor ! PoisonPill

      val restored = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches
      expectMsg(List(MatchState(matchDetails, null, Map(bet.who -> BetState(bet.score)))))
    }

    "update bet and preserve state after" in {
      val competitionId = UUID.randomUUID().toString
      val competitionActor = system.actorOf(CompetitionAggregate.props(competitionId))

      competitionActor ! CreateMatch(matchDetails)
      val created = expectMsgType[MatchCreated]

      competitionActor ! MakeBet(created.id, bet)
      expectMsg(BetMade(created.id, bet))

      val newBet = Bet("Shady", MatchScore(1, 1))
      competitionActor ! MakeBet(created.id, newBet)
      expectMsg(BetMade(created.id, newBet))

      competitionActor ! PoisonPill

      val restored = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches
      expectMsg(List(MatchState(matchDetails, null, Map(newBet.who -> BetState(newBet.score)))))
    }

    "should return empty points map" in {
      val competitionId = UUID.randomUUID().toString
      val competitionActor = system.actorOf(CompetitionAggregate.props(competitionId))

      competitionActor ! CreateMatch(matchDetails)
      expectMsgType[MatchCreated]

      competitionActor ! GetPoints
      expectMsg(Map.empty)
    }

    "should calculate points after match finish" in {
      val competitionId = UUID.randomUUID().toString
      val competitionActor = system.actorOf(CompetitionAggregate.props(competitionId))

      competitionActor ! CreateMatch(matchDetails)
      val created = expectMsgType[MatchCreated]

      val foo = Bet("Foo", MatchScore(0, 3))
      competitionActor ! MakeBet(created.id, foo)
      expectMsgType[BetMade]

      val bar = Bet("Bar", MatchScore(1, 2))
      competitionActor ! MakeBet(created.id, bar)
      expectMsgType[BetMade]

      val baz = Bet("Baz", MatchScore(2, 0))
      competitionActor ! MakeBet(created.id, baz)
      expectMsgType[BetMade]

      val score = MatchScore(0 ,3)
      competitionActor ! FinishMatch(created.id, score)
      expectMsgType[MatchFinished]

      competitionActor ! GetPoints
      expectMsg(Map(
        "Foo" -> 5,
        "Bar" -> 2,
        "Baz" -> 0
      ))
    }
  }

}
