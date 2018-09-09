package com.github.matek2305.djamoe.restapi

import java.time.{LocalDateTime, Month}

import akka.actor.ActorRef
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.github.matek2305.djamoe.app.CompetitionActorQuery.{GetAllMatches, GetPoints}
import com.github.matek2305.djamoe.app.CompetitionActorResponse.CommandProcessed
import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch, MakeBet}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchAdded, MatchFinished}
import com.github.matek2305.djamoe.domain.{Match, MatchId, Score}
import com.github.matek2305.djamoe.restapi.CompetitionRestApiResponse.{GetMatchesResponse, GetPointsResponse, MatchResponse, PlayerPoints}
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class CompetitionRestApiSpec extends FlatSpec
  with CompetitionRestApi
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
    Get("/matches") ~> RawHeader("X-Logged-User", "Test") ~> routes ~> check {
      probe.expectMsg(GetAllMatches)

      val matchId = MatchId()
      val matchDetails = Match("France", "Belgium", LocalDateTime.of(2018, Month.JULY, 10, 20, 0))

      probe.reply(Map(matchId -> matchDetails))

      eventually { status shouldEqual StatusCodes.OK }

      responseAs[GetMatchesResponse] shouldEqual GetMatchesResponse(
        List(
          MatchResponse(
            matchId,
            "CREATED",
            matchDetails.homeTeamName,
            matchDetails.awayTeamName,
            matchDetails.startDate,
            None,
            None,
            0
          )
        )
      )
    }
  }

  it should "return list of player's points when GET /points" in {
    Get("/points") ~> routes ~> check {
      probe.expectMsg(GetPoints)
      probe.reply(Map(
        "Foo" -> 5,
        "Bar" -> 2,
        "Baz" -> 0
      ))

      eventually { status shouldEqual StatusCodes.OK }

      responseAs[GetPointsResponse] shouldEqual GetPointsResponse(
        List(
          PlayerPoints("Foo", 5),
          PlayerPoints("Bar", 2),
          PlayerPoints("Baz", 0)
        )
      )
    }
  }

  it should "add match to competition when POST to /matches" in {
    val addMatch = AddMatch("France", "Belgium", LocalDateTime.of(2018, Month.JULY, 10, 20, 0))

    Post("/matches", HttpEntity(ContentTypes.`application/json`, addMatch.toJson.toString)) ~> RawHeader("X-Logged-User", "Test") ~> routes ~> check {
      probe.expectMsg(addMatch)

      val matchId = MatchId()
      probe.reply(CommandProcessed(addMatch.toMatchAdded(matchId)))

      eventually { status shouldEqual StatusCodes.Created }

      responseAs[MatchAdded] shouldEqual addMatch.toMatchAdded(matchId)
    }
  }

  it should "finish match with given result when POST to /matches/:matchId/results" in {
    val matchId = MatchId()
    val score = Score(2, 2)

    Post(s"/matches/$matchId/results", HttpEntity(ContentTypes.`application/json`, score.toJson.toString)) ~> RawHeader("X-Logged-User", "Test") ~> routes ~> check {
      probe.expectMsg(FinishMatch(matchId, score))
      probe.reply(CommandProcessed(MatchFinished(matchId, score)))

      eventually { status shouldEqual StatusCodes.OK }

      responseAs[MatchFinished] shouldEqual MatchFinished(matchId, score)
    }
  }

  it should "add current user's bet when POST to /match/:matchId/bets" in {
    val matchId = MatchId()
    val bet = Score(2, 2)

    val loggedUser = "Test"
    Post(s"/matches/$matchId/bets", HttpEntity(ContentTypes.`application/json`, bet.toJson.toString)) ~> RawHeader("X-Logged-User", loggedUser) ~> routes ~> check {
      probe.expectMsg(MakeBet(matchId, loggedUser, bet))
      probe.reply(CommandProcessed(BetMade(matchId, loggedUser, bet)))

      eventually { status shouldEqual StatusCodes.OK }

      responseAs[BetMade] shouldEqual BetMade(matchId, loggedUser, bet)
    }
  }
}
