package com.github.matek2305.djamoe.app

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.github.matek2305.djamoe.app.CompetitionActorQuery.{GetAllMatches, GetPoints}
import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, MakeBet}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchAdded, MatchFinished}
import com.github.matek2305.djamoe.domain.{Match, MatchId}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContextExecutor, Future}

trait CompetitionService {

  implicit val system: ActorSystem
  implicit val materializer: Materializer

  implicit def executor: ExecutionContextExecutor

  implicit def timeout: Timeout = Timeout(5.seconds)

  def competitionActor: ActorRef

  def allMatches: Future[Map[MatchId, Match]] = {
    (competitionActor ? GetAllMatches).mapTo[Map[MatchId, Match]]
  }

  def points: Future[Map[String, Int]] = {
    (competitionActor ? GetPoints).mapTo[Map[String, Int]]
  }

  def addMatch(addMatch: AddMatch): Future[MatchAdded] = {
    (competitionActor ? addMatch).mapTo[MatchAdded]
  }

  def finishMatch(finishMatch: FinishMatch): Future[MatchFinished] = {
    (competitionActor ? finishMatch).mapTo[MatchFinished]
  }

  def makeBet(makeBet: MakeBet): Future[BetMade] = {
    (competitionActor ? makeBet).mapTo[BetMade]
  }
}