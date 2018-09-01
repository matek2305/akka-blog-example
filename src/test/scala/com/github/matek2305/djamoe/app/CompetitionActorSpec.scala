package com.github.matek2305.djamoe.app

import java.time.{LocalDateTime, Month}
import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import com.github.matek2305.djamoe.app.CompetitionActorQuery.GetAllMatches
import com.github.matek2305.djamoe.app.CompetitionActorResponse.CommandProcessed
import com.github.matek2305.djamoe.app.CompetitionActorSpec.Test
import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, MakeBet}
import com.github.matek2305.djamoe.domain.CompetitionEvent.BetMade
import com.github.matek2305.djamoe.domain.{Bet, Match, MatchId, Score}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object CompetitionActorSpec {

  abstract class Test(implicit val system: ActorSystem) {
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
  }
}
