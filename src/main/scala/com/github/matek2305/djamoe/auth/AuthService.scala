package com.github.matek2305.djamoe.auth

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.github.matek2305.djamoe.auth.AuthActorQuery.{GetAccessToken, ValidateAccessToken}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait AuthService {

  implicit val system: ActorSystem
  implicit val materializer: Materializer
  implicit val timeout: Timeout

  implicit def executor: ExecutionContextExecutor

  def authActor: ActorRef

  def getAccessToken(username: String, password: String): Future[GetAccessTokenResponse] =
    (authActor ? GetAccessToken(username, password)).mapTo[GetAccessTokenResponse]

  def validateAccessToken(jwt: String): Future[ValidateAccessTokenResponse] =
    (authActor ? ValidateAccessToken(jwt)).mapTo[ValidateAccessTokenResponse]

}
