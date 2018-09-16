package com.github.matek2305.djamoe.auth

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import authentikat.jwt.JsonWebToken
import com.github.matek2305.djamoe.auth.AuthActorCommand.Register
import com.github.matek2305.djamoe.auth.AuthActorQuery.GetAccessToken
import com.github.matek2305.djamoe.auth.AuthActorSpec.Test
import com.github.matek2305.djamoe.auth.GetAccessTokenResponse.{AccessToken, InvalidCredentials}
import com.github.matek2305.djamoe.auth.RegisterResponse.{UserRegistered, UsernameTaken}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

object AuthActorSpec {

  abstract class Test(implicit val system: ActorSystem) {
    protected val authActorId: String = UUID.randomUUID().toString
    protected val authActor: ActorRef = system.actorOf(AuthActor.props(authActorId))
  }
}

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

    "return valid access token for registered user" in new Test {
      authActor ! Register("user", "password")
      expectMsgType[UserRegistered]

      authActor ! GetAccessToken("user", "password")

      val response: AccessToken = expectMsgType[AccessToken]
      assert(JsonWebToken.validate(response.token, config.getString("auth.jwt-secret")))

      authActor ! PoisonPill

      val restored: ActorRef = system.actorOf(AuthActor.props(authActorId))
      restored ! GetAccessToken("user", "password")
      expectMsgType[AccessToken]
    }

    "return invalid credentials message for bad username/password" in new Test {
      authActor ! Register("user", "password")
      expectMsgType[UserRegistered]

      authActor ! GetAccessToken("user", "bad")
      expectMsg(InvalidCredentials)
    }

    "return invalid credentials message for not registered user" in new Test {
      authActor ! GetAccessToken("user", "password")
      expectMsg(InvalidCredentials)
    }

    "prevent to register more than one user with same username" in new Test {
      authActor ! Register("user", "password")
      expectMsgType[UserRegistered]

      authActor ! Register("user", "pass")
      expectMsg(UsernameTaken("user"))
    }
  }
}
