package com.github.matek2305.djamoe

import akka.actor.{ActorLogging, Props}
import akka.pattern.pipe
import akka.persistence.{PersistentActor, RecoveryCompleted}

import scala.concurrent.{Future, Promise}

/**
  * @author Mateusz Urbański <matek2305@gmail.com>.
  */
class CompetitionAggregate(id: String) extends PersistentActor with ActorLogging {

  import CompetitionAggregate._
  import context._

  private var state = CompetitionState(Map.empty)

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case GetAllMatches =>
      sender() ! state()
    case CreateMatch(details) =>
      handleEvent(MatchCreated(MatchId(), details)) pipeTo sender()
      ()
    case FinishMatch(id, score) =>
      handleEvent(MatchFinished(id, score)) pipeTo sender()
      ()
  }

  override def receiveRecover: Receive = {
    case event: MatchEvent => state = applyEvent(event)
    case RecoveryCompleted => log.info("Recovery completed!")
  }

  private def handleEvent[E <: MatchEvent](e: => E): Future[E] = {
    val promise = Promise[E]
    persist(e) { event =>
      promise.success(event)
      state = applyEvent(event)
      system.eventStream.publish(event)
    }
    promise.future
  }

  private def applyEvent(event: MatchEvent): CompetitionState = event match {
    case created: MatchCreated => state.add(created.id, created.details)
    case finished: MatchFinished => state.finishMatch(finished.id, finished.score)
  }
}

object CompetitionAggregate {

  def props(id: String) = Props(new CompetitionAggregate(id))

  sealed trait MatchCommand

  final case class GetAllMatches() extends MatchCommand
  final case class CreateMatch(details: MatchDetails) extends MatchCommand
  final case class FinishMatch(id: MatchId, score: MatchScore) extends MatchCommand

  sealed trait MatchEvent {
    val id: MatchId
  }

  final case class MatchCreated(id: MatchId, details: MatchDetails) extends MatchEvent
  final case class MatchFinished(id: MatchId, score: MatchScore) extends MatchEvent

  final case class CompetitionState(matches: Map[MatchId, MatchState]) {
    def apply(): List[MatchState] = matches.values.toList
    def add(id: MatchId, details: MatchDetails): CompetitionState =
      CompetitionState(matches.updated(id, MatchState(details)))
    def finishMatch(id: MatchId, score: MatchScore): CompetitionState =
      CompetitionState(matches.updated(id, matches(id).finish(score)))
  }
  
  final case class MatchState(details: MatchDetails, score: MatchScore) {
    def finish(score: MatchScore): MatchState = copy(score = score)
  }

  object MatchState {
    def apply(details: MatchDetails): MatchState = MatchState(details, null)
  }

}
