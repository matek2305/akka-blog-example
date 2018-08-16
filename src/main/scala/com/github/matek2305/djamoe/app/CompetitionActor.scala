package com.github.matek2305.djamoe.app

import akka.actor.{ActorRef, DiagnosticActorLogging}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import com.github.matek2305.djamoe.app.CompetitionActorQuery.{GetAllMatches, GetPoints}
import com.github.matek2305.djamoe.app.CompetitionActorResponse.{CommandProcessed, CommandProcessingFailed}
import com.github.matek2305.djamoe.domain.{Competition, CompetitionCommand, CompetitionEvent}

import scala.util.{Failure, Success}

class CompetitionActor(id: String) extends PersistentActor with DiagnosticActorLogging {

  private var competition = Competition()

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case command: CompetitionCommand => process(sender(), command)
    case GetAllMatches => sender() ! competition.matches
    case GetPoints => sender() ! competition.pointsMap
  }

  override def receiveRecover: Receive = {
    case event: CompetitionEvent => updateState(event)
    case RecoveryCompleted => log.info("Recovery completed!")
  }

  private def process(sender: ActorRef, command: CompetitionCommand): Unit = {
    competition.process(command) match {
      case Success(event) =>
        persist(event) { persisted =>
          updateState(persisted)
        }
        sender ! CommandProcessed(event)
      case Failure(ex) => sender ! CommandProcessingFailed(ex)
    }
  }

  def updateState(event: CompetitionEvent): Unit = {
    competition = competition.apply(event) match {
      case Success(modified) => modified
      case Failure(exception) => throw exception
    }
  }
}


