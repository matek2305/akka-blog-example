package com.github.matek2305.djamoe.restapi

import java.time.{LocalDateTime, Month}

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.headers.BasicHttpCredentials
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import akka.util.Timeout
import com.github.matek2305.djamoe.app.CompetitionActorQuery.GetAllMatches
import com.github.matek2305.djamoe.domain.{Bet, Match, MatchId, Score}
import com.github.matek2305.djamoe.restapi.RestApiResponse.{GetMatchesResponse, MatchResponse}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

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
    LocalDateTime.of(2018, Month.JULY, 10, 20, 0),
    bets = Map(
      "foo" -> Bet(Score(2, 2)),
      "bar" -> Bet(Score(2, 1)),
      "baz" -> Bet(Score(0, 2)),
    )
  )

  val credentials = BasicHttpCredentials("admin", config.getString("auth.admin-password"))

  "admin rest api" should "return list of all matches when GET /admin/matches" in {
    Get("/admin/matches") ~> addCredentials(credentials) ~> adminRoutes ~> check {
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
}
