package com.github.matek2305.djamoe.app

import java.time.{LocalDateTime, Month}

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.github.matek2305.djamoe.app.CompetitionActorQuery.GetAllMatches
import com.github.matek2305.djamoe.domain.{Match, MatchId}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}

class RestApiSpec extends FlatSpec
  with RestApi
  with ScalatestRouteTest
  with Matchers
  with Eventually {

  val probe = TestProbe()

  override def config: Config = ConfigFactory.load()
  override def competitionActor: ActorRef = probe.ref

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(1, Seconds)),
    interval = scaled(Span(50, Millis))
  )

  "rest api" should "return list of all matches when GET /matches" in {
    Get("/matches") ~> routes ~> check {
      probe.expectMsg(GetAllMatches)

      val matchId = MatchId()
      val matchDetails = Match("France", "Belgium", LocalDateTime.of(2018, Month.JULY, 10, 20, 0))

      probe.reply(Map(matchId -> matchDetails))

      eventually { status shouldEqual StatusCodes.OK }

      responseAs[GetMatchesResponse] shouldEqual GetMatchesResponse(
        List(MatchResponse(matchId, "CREATED", matchDetails.homeTeamName, matchDetails.awayTeamName, matchDetails.startDate))
      )
    }
  }
}
