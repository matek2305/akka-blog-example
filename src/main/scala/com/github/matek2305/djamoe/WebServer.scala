package com.github.matek2305.djamoe

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer

object WebServer {

  def main(args: Array[String]): Unit = {
    implicit val system: ActorSystem = ActorSystem()
    implicit val materializer: ActorMaterializer = ActorMaterializer()

    val service = new CompetitionRestService(system)
    Http().bindAndHandle(service.route, "localhost", 8080)
    println("Server online at http://localhost:8080/")
  }
}

