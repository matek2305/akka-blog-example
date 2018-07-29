package com.github.matek2305.djamoe.auth

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import authentikat.jwt.JsonWebToken
import com.github.matek2305.djamoe.auth.AuthService.{AccessToken, GetAccessToken, InvalidCredentials}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class AuthServiceSpec
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

    "return valid access token for valid credentials" in {
      val authService = system.actorOf(AuthService.props())
      authService ! GetAccessToken("user1", "user1")

      val response = expectMsgType[AccessToken]
      assert(JsonWebToken.validate(response.token, config.getString("auth.jwt-secret")))
    }

    "return invalid credentials message for bad username/password" in {
      val authService = system.actorOf(AuthService.props())
      authService ! GetAccessToken("user1", "invalid")
      expectMsg(InvalidCredentials())
    }
  }
}
