package com.github.matek2305.djamoe.auth

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import authentikat.jwt.JsonWebToken
import com.github.matek2305.djamoe.auth.AuthActorCommand.Register
import com.github.matek2305.djamoe.auth.AuthActorQuery.GetAccessToken
import com.github.matek2305.djamoe.auth.GetAccessTokenResponse.{AccessToken, InvalidCredentials}
import com.github.matek2305.djamoe.auth.RegisterResponse.UserRegistered
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AuthActorSpec
  extends TestKit(ActorSystem("testSystem"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with ImplicitSender {

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "Auth service actor" should {
    val config = ConfigFactory.load()

    "return valid access token for registered user" in {
      val authService = system.actorOf(AuthActor.props())
      authService ! Register("user", "password")
      expectMsgType[UserRegistered]

      authService ! GetAccessToken("user", "password")

      val response = expectMsgType[AccessToken]
      assert(JsonWebToken.validate(response.token, config.getString("auth.jwt-secret")))
    }

    "return invalid credentials message for bad username/password" in {
      val authService = system.actorOf(AuthActor.props())
      authService ! Register("user", "password")
      expectMsgType[UserRegistered]

      authService ! GetAccessToken("user", "bad")
      expectMsg(InvalidCredentials)
    }

    "return invalid credentials message for not registered user" in {
      val authService = system.actorOf(AuthActor.props())

      authService ! GetAccessToken("user", "password")
      expectMsg(InvalidCredentials)
    }
  }
}
