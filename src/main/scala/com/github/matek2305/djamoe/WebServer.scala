package com.github.matek2305.djamoe

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object WebServer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val competitionAggregate = system.actorOf(CompetitionAggregate.props("competition-id"))
    val service = new CompetitionRestService(competitionAggregate)
    Http().bindAndHandle(service.route, "localhost", 8080)
    println("Server online at http://localhost:8080/")
  }
}

