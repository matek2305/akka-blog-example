package com.github.matek2305.djamoe

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.github.matek2305.djamoe.auth.AuthService

object WebServer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val competitionAggregate = system.actorOf(CompetitionAggregate.props("competition-id"))
    val authService = system.actorOf(AuthService.props())
    val service = new CompetitionRestService(competitionAggregate, authService)
    Http().bindAndHandle(service.route, "localhost", 8080)
    println("Server online at http://localhost:8080/")
  }
}

