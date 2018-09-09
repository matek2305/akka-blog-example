package com.github.matek2305.djamoe.app

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.github.matek2305.djamoe.app.CompetitionActorQuery.{GetAllMatches, GetPoints}
import com.github.matek2305.djamoe.app.CompetitionActorResponse.CommandProcessed
import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, MakeBet}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchAdded, MatchFinished}
import com.github.matek2305.djamoe.domain._

import scala.concurrent.{ExecutionContextExecutor, Future}

trait CompetitionService {

  implicit val system: ActorSystem
  implicit val materializer: Materializer
  implicit val timeout: Timeout

  implicit def executor: ExecutionContextExecutor

  def competitionActor: ActorRef

  def allMatches: Future[Map[MatchId, Match]] = {
    (competitionActor ? GetAllMatches).mapTo[Map[MatchId, Match]]
  }

  def points: Future[Map[String, Int]] = {
    (competitionActor ? GetPoints).mapTo[Map[String, Int]]
  }

  def addMatch(addMatch: AddMatch): Future[MatchAdded] = {
    sendCommand(addMatch).mapTo[MatchAdded]
  }

  def finishMatch(matchId: MatchId, score: Score): Future[MatchFinished] = {
    sendCommand(FinishMatch(matchId, score)).mapTo[MatchFinished]
  }

  def makeBet(matchId: MatchId, who: String, score: Score): Future[BetMade] = {
    sendCommand(MakeBet(matchId, who, score)).mapTo[BetMade]
  }

  private def sendCommand(command: CompetitionCommand): Future[CompetitionEvent] = {
    (competitionActor ? command).mapTo[CommandProcessed].map(_.event)
  }
}
