package com.github.matek2305.djamoe.restapi

import java.time.LocalDateTime.parse

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import akka.util.Timeout
import com.github.matek2305.djamoe.app.CompetitionActorQuery.GetAllMatches
import com.github.matek2305.djamoe.app.CompetitionActorResponse.CommandProcessed
import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, LockBetting}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BettingLocked, MatchAdded, MatchFinished}
import com.github.matek2305.djamoe.domain.{Bet, Match, MatchId, Score}
import com.github.matek2305.djamoe.restapi.RestApiRequest.LoginRequest
import com.github.matek2305.djamoe.restapi.RestApiResponse.{GetMatchesResponse, MatchResponse}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

import scala.concurrent.duration._

class AdminRestApiSpec extends FlatSpec
  with AdminRestApi
  with ScalatestRouteTest
  with Matchers
  with Eventually {

  val probe = TestProbe()

  override implicit val timeout: Timeout = Timeout(5.seconds)

  override def config: Config = ConfigFactory.load()
  override def competitionActor: ActorRef = probe.ref

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(1, Seconds)),
    interval = scaled(Span(50, Millis))
  )

  val sampleMatch = Match(
    "France",
    "Belgium",
    parse("2018-07-10T20:00"),
    bets = Map(
      "foo" -> Bet(Score(2, 2)),
      "bar" -> Bet(Score(2, 1)),
      "baz" -> Bet(Score(0, 2)),
    )
  )

  val adminCredentials = BasicHttpCredentials("admin", config.getString("auth.admin-password"))

  "admin rest api" should "return list of all matches when GET /admin/matches" in {
    Get("/admin/matches") ~> addCredentials(adminCredentials) ~> adminRoutes ~> check {
      probe.expectMsg(GetAllMatches)

      val matchId = MatchId()
      probe.reply(Map(matchId -> sampleMatch))

      eventually { status shouldEqual StatusCodes.OK }

      responseAs[GetMatchesResponse] shouldEqual GetMatchesResponse(
        List(
          MatchResponse(
            matchId,
            sampleMatch.status.toString,
            sampleMatch.homeTeamName,
            sampleMatch.awayTeamName,
            sampleMatch.startDate
          )
        )
      )
    }
  }

  it should "add match to competition when POST to /admin/matches" in {
    val addMatch = AddMatch("France", "Belgium", parse("2018-07-10T20:00"))
    Post("/admin/matches", HttpEntity(ContentTypes.`application/json`, addMatch.toJson.toString)) ~>
      addCredentials(adminCredentials) ~> adminRoutes ~> check {

      probe.expectMsg(addMatch)

      val matchId = MatchId()
      probe.reply(CommandProcessed(addMatch.toMatchAdded(matchId)))

      eventually { status shouldEqual StatusCodes.Created }

      responseAs[MatchAdded] shouldEqual addMatch.toMatchAdded(matchId)
    }
  }

  it should "finish match with given result when POST to /admin/matches/:matchId/results" in {
    val matchId = MatchId()
    val score = Score(2, 2)

    Post(s"/admin/matches/$matchId/results", HttpEntity(ContentTypes.`application/json`, score.toJson.toString)) ~>
      addCredentials(adminCredentials) ~> adminRoutes ~> check {

      probe.expectMsg(FinishMatch(matchId, score))
      probe.reply(CommandProcessed(MatchFinished(matchId, score)))

      eventually { status shouldEqual StatusCodes.OK }

      responseAs[MatchFinished] shouldEqual MatchFinished(matchId, score)
    }
  }

  it should "lock betting for match when POST to /admin/matches/:matchId/locks" in {
    val matchId = MatchId()

    Post(s"/admin/matches/$matchId/locks") ~> addCredentials(adminCredentials) ~> adminRoutes ~> check {
      probe.expectMsg(LockBetting(matchId))
      probe.reply(CommandProcessed(BettingLocked(matchId)))

      eventually {
        status shouldEqual StatusCodes.OK
      }
    }
  }

  it should "authenticate admin when POST to /admin/login with valid credentials" in {
    val request = LoginRequest(adminCredentials.username, adminCredentials.password)
    Post("/admin/login", HttpEntity(ContentTypes.`application/json`, request.toJson.toString)) ~>
      adminRoutes ~> check { status shouldEqual StatusCodes.OK }
  }

  it should "not authenticate admin when POST to /admin/login with invalid password" in {
    val request = LoginRequest(adminCredentials.username, s"${adminCredentials.password}invalid")
    Post("/admin/login", HttpEntity(ContentTypes.`application/json`, request.toJson.toString)) ~>
      adminRoutes ~> check { status shouldEqual StatusCodes.Unauthorized }
  }
}
