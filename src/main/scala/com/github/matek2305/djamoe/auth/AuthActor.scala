package com.github.matek2305.djamoe.auth

import java.util.concurrent.TimeUnit

import akka.actor.{DiagnosticActorLogging, Props}
import akka.persistence.{PersistentActor, RecoveryCompleted}
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import com.github.matek2305.djamoe.auth.AuthActorCommand.Register
import com.github.matek2305.djamoe.auth.AuthActorQuery.{GetAccessToken, ValidateAccessToken}
import com.github.matek2305.djamoe.auth.GetAccessTokenResponse.{AccessToken, InvalidCredentials}
import com.github.matek2305.djamoe.auth.RegisterResponse.{UserRegistered, UsernameTaken}
import com.github.matek2305.djamoe.auth.ValidateAccessTokenResponse.{TokenExpired, TokenIsValid, ValidationFailed}
import com.typesafe.config.ConfigFactory
import org.mindrot.jbcrypt.BCrypt.{checkpw, gensalt, hashpw}

class AuthActor(id: String) extends PersistentActor with DiagnosticActorLogging {

  private val config = ConfigFactory.load()
  private val header = JwtHeader("HS256")

  private var users = Map.empty[String, String]

  override def persistenceId: String = id

  override def receiveCommand: Receive = {
    case Register(username, _) if users.contains(username) =>
      sender() ! UsernameTaken(username)

    case Register(username, password) =>
      persist(UserRegistered(username, hashpw(password, gensalt()))) { registered =>
        users += (username -> registered.password)
        sender() ! registered
      }

    case GetAccessToken(username, password) if validCredentials(username, password) =>
      sender() ! AccessToken(JsonWebToken(header, setClaims(username), config.getString("auth.jwt-secret")))

    case GetAccessToken(_, _) =>
      sender() ! InvalidCredentials

    case ValidateAccessToken(jwt) if isTokenExpired(jwt) =>
      sender() ! TokenExpired

    case ValidateAccessToken(jwt) if JsonWebToken.validate(jwt, config.getString("auth.jwt-secret")) =>
      sender() ! TokenIsValid(getClaims(jwt).getOrElse(Map.empty[String, Any]))

    case ValidateAccessToken(_) =>
      sender() ! ValidationFailed
  }

  override def receiveRecover: Receive = {
    case registered: UserRegistered =>
      users += (registered.username -> registered.password)
    case RecoveryCompleted => log.info("Recovery completed!")
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
      "expiredAt" -> (System.currentTimeMillis() + TimeUnit.DAYS.toMillis(
        config.getInt("auth.token-expiry-period-in-days")
      ))
    )
  )
}

object AuthActor {
  def props(id: String) = Props(new AuthActor(id))
}
