package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}

import akka.actor.ActorSystem
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.TestProbe
import com.github.matek2305.djamoe.CompetitionAggregate._
import com.github.matek2305.djamoe.CompetitionRestService._
import com.github.matek2305.djamoe.auth.AuthService.{AccessToken, GetAccessToken, InvalidCredentials}
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import spray.json._

object CompetitionRestServiceSpec {

  abstract class Test(implicit val system: ActorSystem) {
    protected val competitionAggregate = TestProbe()
    protected val authService = TestProbe()
    protected val route: Route = new CompetitionRestService(
      competitionAggregate.ref,
      authService.ref
    ).route
  }
}

class CompetitionRestServiceSpec extends WordSpec
  with ScalatestRouteTest
  with JsonSupport
  with Matchers
  with Eventually {

  import CompetitionRestServiceSpec._

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(1, Seconds)),
    interval = scaled(Span(50, Millis))
  )

  "Competition rest service" should {
    "return list of all matches for GET requests to the /matches path" in new Test {
      Get("/matches") ~> route ~> check {
        competitionAggregate.expectMsg(GetAllMatches())

        val matchId = MatchId()
        val matchDetails = Match("France", "Belgium", LocalDateTime.of(2018, Month.JULY, 10, 20, 0))

        competitionAggregate.reply(Map(matchId -> MatchState(matchDetails)))

        eventually { status shouldEqual StatusCodes.OK }

        responseAs[GetMatchesResponse] shouldEqual GetMatchesResponse(
          List(MatchResponse(matchId, "CREATED", matchDetails))
        )
      }
    }

    "return players points for GET requests to the /points path" in new Test {
      Get("/points") ~> route ~> check {
        competitionAggregate.expectMsg(GetPoints())
        competitionAggregate.reply(Map(
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

    "create match in competition for POST requests to the /matches path" in new Test {
      val matchDetails = Match("France", "Belgium", LocalDateTime.of(2018, Month.JULY, 10, 20, 0))
      val content: String = matchDetails.toJson.toString()

      Post("/matches", HttpEntity(ContentTypes.`application/json`, content)) ~> route ~> check {
        competitionAggregate.expectMsg(CreateMatch(matchDetails))

        val matchId = MatchId()
        competitionAggregate.reply(MatchCreated(matchId, matchDetails))

        eventually { status shouldEqual StatusCodes.Created }

        responseAs[MatchCreated] shouldEqual MatchCreated(matchId, matchDetails)
      }
    }

    "create bet for match for POST requests to the /matches/:id/bets path" in new Test {
      val matchId = MatchId()
      val bet = Bet("Foo", MatchScore(1, 2))

      val content: String = bet.toJson.toString()
      Post(s"/matches/$matchId/bets", HttpEntity(ContentTypes.`application/json`, content)) ~> route ~> check {
        competitionAggregate.expectMsg(MakeBet(matchId, bet))
        competitionAggregate.reply(BetMade(matchId, bet))

        eventually { status shouldEqual StatusCodes.Created }

        responseAs[BetMade] shouldEqual BetMade(matchId, bet)
      }
    }

    "finish match for POST requests to the /matches/:id/results path" in new Test {
      val matchId = MatchId()
      val score = MatchScore(1, 1)
      val content: String = score.toJson.toString()

      Post(s"/matches/$matchId/results", HttpEntity(ContentTypes.`application/json`, content)) ~> route ~> check {
        competitionAggregate.expectMsg(FinishMatch(matchId, score))
        competitionAggregate.reply(MatchFinished(matchId, score))

        eventually { status shouldEqual StatusCodes.Created }

        responseAs[MatchFinished] shouldEqual MatchFinished(matchId, score)
      }
    }

    "return access token for POST requests with valid credentials to the /login path" in new Test {
      val loginRequest = LoginRequest("admin", "admin")
      val credentials: String = loginRequest.toJson.toString()

      Post("/login", HttpEntity(ContentTypes.`application/json`, credentials)) ~> route ~> check {
        authService.expectMsg(GetAccessToken("admin", "admin"))
        authService.reply(AccessToken("jwt"))

        eventually { status shouldEqual StatusCodes.OK }

        header("Access-Token").map(_.value()) shouldBe Some("jwt")
      }
    }

    "return Unauthorized for POST requests with invalid credentials to the /login path" in new Test {
      val loginRequest = LoginRequest("invalid", "invalid")
      val credentials: String = loginRequest.toJson.toString()

      Post("/login", HttpEntity(ContentTypes.`application/json`, credentials)) ~> route ~> check {
        authService.expectMsg(GetAccessToken("invalid", "invalid"))
        authService.reply(InvalidCredentials())

        eventually { status shouldEqual StatusCodes.Unauthorized }
      }
    }
  }
}
