package com.github.matek2305.djamoe.auth

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.stream.Materializer
import akka.util.Timeout
import com.github.matek2305.djamoe.auth.AuthActor.{GetAccessToken, GetAccessTokenResponse, ValidateAccessToken, ValidateAccessTokenResponse}

import scala.concurrent.{ExecutionContextExecutor, Future}

trait AuthService {

  implicit val system: ActorSystem
  implicit val materializer: Materializer

  implicit def executor: ExecutionContextExecutor
  implicit def timeout: Timeout

  def authActor: ActorRef

  def getAccessToken(username: String, password: String): Future[GetAccessTokenResponse] =
    (authActor ? GetAccessToken(username, password)).mapTo[GetAccessTokenResponse]

  def validateAccessToken(jwt: String): Future[ValidateAccessTokenResponse] =
    (authActor ? ValidateAccessToken(jwt)).mapTo[ValidateAccessTokenResponse]

}
