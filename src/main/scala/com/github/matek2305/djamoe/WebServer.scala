package com.github.matek2305.djamoe

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, Materializer}
import com.github.matek2305.djamoe.app.CompetitionActor
import com.github.matek2305.djamoe.restapi.CompetitionRestApi
import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.ExecutionContextExecutor

object WebServer extends App with CompetitionRestApi {

  override implicit val system: ActorSystem = ActorSystem()
  override implicit val materializer: Materializer = ActorMaterializer()
  override implicit val executor: ExecutionContextExecutor = system.dispatcher

  override def config: Config = ConfigFactory.load()

  override def competitionActor: ActorRef = system.actorOf(CompetitionActor.props("competition-id"))

  val interface = config.getString("http.interface")
  val port = config.getInt("http.port")

  Http().bindAndHandle(routes, interface, port)
  println(s"Server online at http://$interface:$port/")
}
