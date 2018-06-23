package com.github.matek2305.djamoe

import akka.actor.{ActorLogging, Props}
import akka.pattern.pipe
import akka.persistence.{PersistentActor, RecoveryCompleted}

import scala.concurrent.{Future, Promise}

/**
  * @author Mateusz Urbański <matek2305@gmail.com>.
  */
class CompetitionAggregate extends PersistentActor with ActorLogging {

  import CompetitionAggregate._
  import context._

  private var state = CompetitionState(Map.empty)

  override def persistenceId: String = "competition-aggregate"

  override def receiveCommand: Receive = {
    case GetAllMatches =>
      sender() ! state()
    case CreateMatch(details) =>
      handleEvent(MatchCreated(MatchId(), details)) pipeTo sender()
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
  }
}

object CompetitionAggregate {

  def props = Props(new CompetitionAggregate())

  sealed trait MatchCommand

  final case class GetAllMatches() extends MatchCommand
  final case class CreateMatch(details: MatchDetails) extends MatchCommand

  sealed trait MatchEvent {
    val id: MatchId
  }

  final case class MatchCreated(id: MatchId, details: MatchDetails) extends MatchEvent

  final case class CompetitionState(matches: Map[MatchId, MatchDetails]) {
    def apply(): List[MatchDetails] = matches.values.toList
    def add(id: MatchId, details: MatchDetails): CompetitionState =
      CompetitionState(matches.updated(id, details))
  }

}
