package com.github.matek2305.djamoe.app

import java.time.{LocalDateTime, Month}
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.matek2305.djamoe.app.CompetitionActorQuery.{GetAllMatches, GetMatch, GetPoints}
import com.github.matek2305.djamoe.app.CompetitionActorResponse.{CommandProcessed, CommandProcessingFailed}
import com.github.matek2305.djamoe.app.CompetitionActorSpec.Test
import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, MakeBet}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchFinished}
import com.github.matek2305.djamoe.domain._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object CompetitionActorSpec {

  abstract class Test(implicit val system: ActorSystem) {
    protected implicit val makeBetPolicy: MakeBetPolicy = (_: Match) => true
    protected val competitionId: String = UUID.randomUUID().toString
    protected val competitionActor: ActorRef = system.actorOf(CompetitionActor.props(competitionId))
  }

}

class CompetitionActorSpec
  extends TestKit(ActorSystem("testSystem"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "CompetitionActor" should {
    val sampleAddMatchCommand = AddMatch(
      "Liverpool FC",
      "Paris Saint-Germain",
      LocalDateTime.of(2018, Month.SEPTEMBER, 18, 21, 0)
    )

    "add match to competition and preserve state after restart" in new Test {
      competitionActor ! sampleAddMatchCommand
      val matchId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionActor.props(competitionId))
      restored ! GetAllMatches
      expectMsg(Map(
        matchId -> Match(
          sampleAddMatchCommand.homeTeamName,
          sampleAddMatchCommand.awayTeamName,
          sampleAddMatchCommand.startDate
        )
      ))
    }

    "add bet and preserve state after restart" in new Test {
      competitionActor ! sampleAddMatchCommand
      val matchId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! MakeBet(matchId, "Foo", Score(2, 0))
      expectMsg(CommandProcessed(BetMade(matchId, "Foo", Score(2, 0))))

      competitionActor ! MakeBet(matchId, "Bar", Score(1, 1))
      expectMsg(CommandProcessed(BetMade(matchId, "Bar", Score(1, 1))))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionActor.props(competitionId))
      restored ! GetAllMatches
      expectMsg(Map(
        matchId -> Match(
          sampleAddMatchCommand.homeTeamName,
          sampleAddMatchCommand.awayTeamName,
          sampleAddMatchCommand.startDate,
          bets = Map(
            "Foo" -> Bet(Score(2, 0)),
            "Bar" -> Bet(Score(1, 1))
          )
        )
      ))
    }

    "update bet and preserve state after" in new Test {
      competitionActor ! sampleAddMatchCommand
      val matchId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! MakeBet(matchId, "Foo", Score(2, 0))
      expectMsg(CommandProcessed(BetMade(matchId, "Foo", Score(2, 0))))

      competitionActor ! MakeBet(matchId, "Bar", Score(1, 1))
      expectMsg(CommandProcessed(BetMade(matchId, "Bar", Score(1, 1))))

      competitionActor ! MakeBet(matchId, "Foo", Score(2, 1))
      expectMsg(CommandProcessed(BetMade(matchId, "Foo", Score(2, 1))))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionActor.props(competitionId))
      restored ! GetAllMatches
      expectMsg(Map(
        matchId -> Match(
          sampleAddMatchCommand.homeTeamName,
          sampleAddMatchCommand.awayTeamName,
          sampleAddMatchCommand.startDate,
          bets = Map(
            "Foo" -> Bet(Score(2, 1)),
            "Bar" -> Bet(Score(1, 1))
          )
        )
      ))
    }

    "finish match and preserve state after restart" in new Test {
      competitionActor ! sampleAddMatchCommand
      val matchId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! FinishMatch(matchId, Score(3, 1))
      expectMsg(CommandProcessed(MatchFinished(matchId, Score(3, 1))))

      competitionActor ! PoisonPill

      val restored: ActorRef = system.actorOf(CompetitionActor.props(competitionId))
      restored ! GetAllMatches
      expectMsg(Map(
        matchId -> Match(
          sampleAddMatchCommand.homeTeamName,
          sampleAddMatchCommand.awayTeamName,
          sampleAddMatchCommand.startDate,
          Match.FINISHED,
          Some(Score(3, 1))
        )
      ))
    }

    "return empty points map" in new Test {
      competitionActor ! GetPoints
      expectMsg(Map.empty)

      competitionActor ! sampleAddMatchCommand
      expectMsgType[CommandProcessed]

      competitionActor ! GetPoints
      expectMsg(Map.empty)
    }

    "calculate points after match finish" in new Test {
      competitionActor ! sampleAddMatchCommand
      val matchId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! MakeBet(matchId, "Foo", Score(0, 3))
      expectMsgType[CommandProcessed]

      competitionActor ! MakeBet(matchId, "Bar", Score(1, 2))
      expectMsgType[CommandProcessed]

      competitionActor ! MakeBet(matchId, "Baz", Score(2, 0))
      expectMsgType[CommandProcessed]

      competitionActor ! GetPoints
      expectMsg(Map("Foo" -> 0, "Bar" -> 0, "Baz" -> 0))

      competitionActor ! FinishMatch(matchId, Score(0, 3))
      expectMsgType[CommandProcessed]

      competitionActor ! GetPoints
      expectMsg(Map("Foo" -> 5, "Bar" -> 2, "Baz" -> 0))
    }

    "sum points from all finished matches" in new Test {
      competitionActor ! AddMatch("Chelsea", "Bournemouth", LocalDateTime.of(2018, Month.SEPTEMBER, 1, 16, 0))
      val cheVsBouId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! AddMatch("Parma", "Juventus", LocalDateTime.of(2018, Month.SEPTEMBER, 1, 20, 30))
      val parVsJuvId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! AddMatch("Cardiff", "Arsenal", LocalDateTime.of(2018, Month.SEPTEMBER, 2, 14, 30))
      val carVsArsId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! GetPoints
      expectMsg(Map.empty)

      competitionActor ! MakeBet(cheVsBouId, "Foo", Score(2, 0))
      expectMsgType[CommandProcessed]

      competitionActor ! MakeBet(cheVsBouId, "Bar", Score(3, 0))
      expectMsgType[CommandProcessed]

      competitionActor ! MakeBet(parVsJuvId, "Foo", Score(0, 2))
      expectMsgType[CommandProcessed]

      competitionActor ! MakeBet(carVsArsId, "Foo", Score(1, 3))
      expectMsgType[CommandProcessed]

      competitionActor ! GetPoints
      expectMsg(Map("Foo" -> 0, "Bar" -> 0))

      competitionActor ! FinishMatch(cheVsBouId, Score(2, 0))
      expectMsgType[CommandProcessed]

      competitionActor ! GetPoints
      expectMsg(Map("Foo" -> 5, "Bar" -> 2))

      competitionActor ! FinishMatch(parVsJuvId, Score(1, 2))
      expectMsgType[CommandProcessed]

      competitionActor ! GetPoints
      expectMsg(Map("Foo" -> 7, "Bar" -> 2))
    }

    "prevent betting after match is finished" in new Test {
      competitionActor ! sampleAddMatchCommand
      val matchId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! FinishMatch(matchId, Score(2, 2))
      expectMsg(CommandProcessed(MatchFinished(matchId, Score(2, 2))))

      competitionActor ! MakeBet(matchId, "Foo", Score(2, 2))
      expectMsgType[CommandProcessingFailed]

      competitionActor ! GetAllMatches
      expectMsg(Map(
        matchId -> Match(
          sampleAddMatchCommand.homeTeamName,
          sampleAddMatchCommand.awayTeamName,
          sampleAddMatchCommand.startDate,
          Match.FINISHED,
          Some(Score(2, 2))
        )
      ))
    }

    "return match by its id" in new Test {
      competitionActor ! sampleAddMatchCommand
      val matchId: MatchId = expectMsgType[CommandProcessed].event.matchId

      competitionActor ! GetMatch(matchId)
      expectMsg(Some(Match(
        sampleAddMatchCommand.homeTeamName,
        sampleAddMatchCommand.awayTeamName,
        sampleAddMatchCommand.startDate
      )))
    }

    "return None when match with given id does not exist" in new Test {
      competitionActor ! GetMatch(MatchId())
      expectMsg(None)
    }
  }
}
