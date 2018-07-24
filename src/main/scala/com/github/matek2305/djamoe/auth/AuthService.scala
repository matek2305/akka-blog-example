package com.github.matek2305.djamoe.auth

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, Props}
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.github.matek2305.djamoe.auth.AuthService._
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

    case ValidateAccessToken(jwt) if isTokenExpired(jwt) =>
      sender() ! TokenExpired()

    case ValidateAccessToken(jwt) if JsonWebToken.validate(jwt, secretKey) =>
      sender() ! TokenIsValid(getClaims(jwt).getOrElse(Map.empty[String, Any]))

    case ValidateAccessToken(_) =>
      sender() ! ValidationFailed()
  }

  private def validCredentials(username: String, password: String) = users.get(username) match {
    case Some(hashed) if checkpw(password, hashed) => true
    case _ => false
  }

  private def isTokenExpired(jwt: String) = getClaims(jwt) match {
    case Some(claims) =>
      claims.get("expiredAt") match {
        case Some(value) => value.toLong < System.currentTimeMillis()
        case None => false
      }
    case None => false
  }

  private def getClaims(jwt: String) = jwt match {
    case JsonWebToken(_, claims, _) => claims.asSimpleMap.toOption
    case _ => None
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
  final case class ValidateAccessToken(jwt: String)

  sealed trait GetAccessTokenResponse
  final case class AccessToken(token: String) extends GetAccessTokenResponse
  final case class InvalidCredentials() extends GetAccessTokenResponse

  sealed trait ValidateAccessTokenResponse
  final case class TokenIsValid(claims: Map[String, Any]) extends ValidateAccessTokenResponse
  final case class TokenExpired() extends ValidateAccessTokenResponse
  final case class ValidationFailed() extends ValidateAccessTokenResponse

  def props() = Props(new AuthService)
}
