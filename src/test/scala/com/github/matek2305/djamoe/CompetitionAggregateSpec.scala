package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.matek2305.djamoe.CompetitionAggregate._
import com.github.matek2305.djamoe.CompetitionAggregateSpec.Test
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object CompetitionAggregateSpec {

  abstract class Test(implicit val system: ActorSystem) {
    protected val competitionId: String = UUID.randomUUID().toString
    protected val competitionActor: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
  }
}

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

    "create match and preserve state after restart" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]
      assert(created.details == matchDetails)

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails)))
    }

    "finish match and preserve state after restart" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! FinishMatch(created.id, score)
      expectMsg(MatchFinished(created.id, score))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails, score, Map.empty, MatchState.FINISHED)))
    }

    "add bet and preserve state after restart" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! MakeBet(created.id, bet)
      expectMsg(BetMade(created.id, bet))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails, null, Map(bet.who -> BetState(bet.score)), MatchState.CREATED)))
    }

    "update bet and preserve state after" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! MakeBet(created.id, bet)
      expectMsg(BetMade(created.id, bet))

      val newBet = Bet("Shady", MatchScore(1, 1))
      competitionActor ! MakeBet(created.id, newBet)
      expectMsg(BetMade(created.id, newBet))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails, null, Map(newBet.who -> BetState(newBet.score)), MatchState.CREATED)))
    }

    "return empty points map" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      expectMsgType[MatchCreated]

      competitionActor ! GetPoints()
      expectMsg(Map.empty)
    }

    "return empty points map when no matches" in new Test {
      competitionActor ! GetPoints()
      expectMsg(Map.empty)
    }

    "calculate points after match finish" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]

      val foo = Bet("Foo", MatchScore(0, 3))
      competitionActor ! MakeBet(created.id, foo)
      expectMsgType[BetMade]

      val bar = Bet("Bar", MatchScore(1, 2))
      competitionActor ! MakeBet(created.id, bar)
      expectMsgType[BetMade]

      val baz = Bet("Baz", MatchScore(2, 0))
      competitionActor ! MakeBet(created.id, baz)
      expectMsgType[BetMade]

      competitionActor ! GetPoints()
      expectMsg(Map(
        "Foo" -> 0,
        "Bar" -> 0,
        "Baz" -> 0
      ))

      val score = MatchScore(0 ,3)
      competitionActor ! FinishMatch(created.id, score)
      expectMsgType[MatchFinished]

      competitionActor ! GetPoints()
      expectMsg(Map(
        "Foo" -> 5,
        "Bar" -> 2,
        "Baz" -> 0
      ))
    }

    "sum points from match bets" in new Test {
      val fraVsArg = Match("France", "Argentina", LocalDateTime.of(2018, Month.JUNE, 30, 16, 0))
      competitionActor ! CreateMatch(fraVsArg)
      val fraVsArgCreated: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! MakeBet(fraVsArgCreated.id, Bet("Foo", MatchScore(3, 1)))
      expectMsgType[BetMade]

      competitionActor ! GetPoints()
      expectMsg(Map("Foo" -> 0))

      competitionActor ! FinishMatch(fraVsArgCreated.id, MatchScore(4, 3))
      expectMsgType[MatchFinished]

      competitionActor ! GetPoints()
      expectMsg(Map("Foo" -> 2))

      val urgVsPor = Match("Urugway", "Portugal", LocalDateTime.of(2018, Month.JUNE, 30, 20, 0))
      competitionActor ! CreateMatch(urgVsPor)
      val urgVsPorCreated: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! MakeBet(urgVsPorCreated.id, Bet("Foo", MatchScore(2, 1)))
      expectMsgType[BetMade]

      competitionActor ! FinishMatch(urgVsPorCreated.id, MatchScore(2, 1))
      expectMsgType[MatchFinished]

      competitionActor ! GetPoints()
      expectMsg(Map("Foo" -> 7))
    }

    "lock betting and preserve state after restart" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails)))

      competitionActor ! LockBetting(created.id)
      expectMsg(BettingLocked(created.id))

      competitionActor ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails, null, Map.empty, MatchState.LOCKED)))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails, null, Map.empty, MatchState.LOCKED)))
    }

    "prevent betting after match is locked" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! LockBetting(created.id)
      expectMsg(BettingLocked(created.id))

      competitionActor ! MakeBet(created.id, bet)
      expectMsg(BettingAlreadyLocked(created.id))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails, null, Map.empty, MatchState.LOCKED)))
    }

    "prevent betting after match is finished" in new Test {
      competitionActor ! CreateMatch(matchDetails)
      val created: MatchCreated = expectMsgType[MatchCreated]

      competitionActor ! FinishMatch(created.id, score)
      expectMsg(MatchFinished(created.id, score))

      competitionActor ! MakeBet(created.id, bet)
      expectMsg(MatchHaveFinished(created.id))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionAggregate.props(competitionId))
      restored ! GetAllMatches()
      expectMsg(Map(created.id -> MatchState(matchDetails, score, Map.empty, MatchState.FINISHED)))
    }
  }

}
