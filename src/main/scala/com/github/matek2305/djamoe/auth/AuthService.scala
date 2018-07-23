package com.github.matek2305.djamoe.auth

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.github.matek2305.djamoe.auth.AuthService.{AccessToken, GetAccessToken, InvalidCredentials}
import org.mindrot.jbcrypt.BCrypt
import org.mindrot.jbcrypt.BCrypt.{checkpw, hashpw}

class AuthService extends Actor {

  private val tokenExpiryPeriodInDays = 1
  private val secretKey = "super_secret_key"
  private val header = JwtHeader("HS256")

  private val users = Map(
    "user1" -> hashpw("user1", BCrypt.gensalt()),
    "user2" -> hashpw("user2", BCrypt.gensalt()),
    "user3" -> hashpw("user3", BCrypt.gensalt()),
    "user4" -> hashpw("user4", BCrypt.gensalt()),
    "user5" -> hashpw("user5", BCrypt.gensalt()),
  )

  override def receive: Receive = {
    case GetAccessToken(username, password) if validCredentials(username, password) =>
      sender() ! AccessToken(JsonWebToken(header, setClaims(username), secretKey))

    case GetAccessToken(_, _) =>
      sender() ! InvalidCredentials()
  }

  private def validCredentials(username: String, password: String) = users.get(username) match {
    case Some(hashed) if checkpw(password, hashed) => true
    case _ => false
  }

  private def setClaims(username: String) = JwtClaimsSet(
    Map(
      "user" -> username,
      "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(tokenExpiryPeriodInDays))
    )
  )
}

object AuthService {

  sealed trait Request
  final case class GetAccessToken(username: String, password: String)

  sealed trait GetAccessTokenResponse
  final case class AccessToken(token: String) extends GetAccessTokenResponse
  final case class InvalidCredentials() extends GetAccessTokenResponse

  def props() = Props(new AuthService)
}
