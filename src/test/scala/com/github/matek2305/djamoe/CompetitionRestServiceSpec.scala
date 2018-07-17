package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}

import akka.actor.{ActorRef, ActorSystem}
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActor, TestProbe}
import com.github.matek2305.djamoe.CompetitionAggregate._
import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatest.{Matchers, WordSpec}
import spray.json._

object CompetitionRestServiceSpec {

  abstract class Test(implicit val system: ActorSystem) {
    protected val competitionAggregate = TestProbe()
    protected val route: Route = new CompetitionRestService(competitionAggregate.ref).route
  }

}

class CompetitionRestServiceSpec extends WordSpec
  with ScalatestRouteTest
  with Matchers
  with Eventually {

  import CompetitionRestServiceSpec._

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(2, Seconds)),
    interval = scaled(Span(100, Millis))
  )

  "Competition rest service" should {
    "return list of all matches for GET requests to the /matches path" in new Test {
      Get("/matches") ~> route ~> check {
        competitionAggregate.expectMsg(GetAllMatches())

        val matchId = MatchId()
        competitionAggregate.reply(Map(
          matchId -> MatchState(Match("France", "Belgium", LocalDateTime.of(2018, Month.JULY, 10, 20, 0)))
        ))

        eventually {
          status shouldEqual StatusCodes.OK
        }

        responseAs[String].parseJson shouldEqual JsObject(
          "matches" -> JsArray(
            JsObject(
              "id" -> JsString(matchId.toString),
              "details" -> JsObject(
                "homeTeamName" -> JsString("France"),
                "awayTeamName" -> JsString("Belgium"),
                "startDate" -> JsString("2018-07-10T20:00:00")
              )
            )
          )
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

        eventually {
          status shouldEqual StatusCodes.OK
        }

        responseAs[String].parseJson shouldEqual JsObject(
          "players" -> JsArray(
            JsObject(
              "name" -> JsString("Foo"),
              "points" -> JsNumber(5)
            ),
            JsObject(
              "name" -> JsString("Bar"),
              "points" -> JsNumber(2)
            ),
            JsObject(
              "name" -> JsString("Baz"),
              "points" -> JsNumber(0)
            )
          )
        )
      }
    }

    "create match in competition for POST requests to the /matches path" in new Test {
      val content: String = JsObject(
        "homeTeamName" -> JsString("France"),
        "awayTeamName" -> JsString("Belgium"),
        "startDate" -> JsString("2018-07-10T20:00:00")
      ).toString()

      Post("/matches", HttpEntity(ContentTypes.`application/json`, content)) ~> route ~> check {
        competitionAggregate.expectMsg(
          CreateMatch(
            Match(
              "France",
              "Belgium",
              LocalDateTime.of(2018, Month.JULY, 10, 20, 0)
            )
          )
        )

        val matchId = MatchId()
        competitionAggregate.reply(
          MatchCreated(
            matchId,
            Match("France", "Belgium", LocalDateTime.of(2018, Month.JULY, 10, 20, 0))
          )
        )

        eventually { status shouldEqual StatusCodes.Created }

        responseAs[String].parseJson shouldEqual JsObject(
          "id" -> JsString(matchId.toString),
          "details" -> JsObject(
            "homeTeamName" -> JsString("France"),
            "awayTeamName" -> JsString("Belgium"),
            "startDate" -> JsString("2018-07-10T20:00:00")
          )
        )
      }
    }

    "create bet for match for POST requests to the /matches/:id/bets path" in new Test {
      val matchId = MatchId()
      val content: String = JsObject(
        "who" -> JsString("Foo"),
        "score" -> JsObject(
          "homeTeam" -> JsNumber(1),
          "awayTeam" -> JsNumber(2)
        )
      ).toString()

      Post(s"/matches/$matchId/bets", HttpEntity(ContentTypes.`application/json`, content)) ~> route ~> check {
        competitionAggregate.expectMsg(MakeBet(matchId, Bet("Foo", MatchScore(1, 2))))
        competitionAggregate.reply(BetMade(matchId, Bet("Foo", MatchScore(1, 2))))

        eventually { status shouldEqual StatusCodes.Created }

        responseAs[String].parseJson shouldEqual JsObject(
          "id" -> JsString(matchId.toString),
          "bet" -> JsObject(
            "who" -> JsString("Foo"),
            "score" -> JsObject(
              "homeTeam" -> JsNumber(1),
              "awayTeam" -> JsNumber(2)
            )
          )
        )
      }
    }

    "finish match for POST requests to the /matches/:id/score path" in new Test {
      val matchId = MatchId()
      val content: String = JsObject(
        "homeTeam" -> JsNumber(1),
        "awayTeam" -> JsNumber(1)
      ).toString()

      Post(s"/matches/$matchId/score", HttpEntity(ContentTypes.`application/json`, content)) ~> route ~> check {
        competitionAggregate.expectMsg(FinishMatch(matchId, MatchScore(1, 1)))
        competitionAggregate.reply(MatchFinished(matchId, MatchScore(1, 1)))

        eventually { status shouldEqual StatusCodes.Created }

        responseAs[String].parseJson shouldEqual JsObject(
          "id" -> JsString(matchId.toString),
          "score" -> JsObject(
            "homeTeam" -> JsNumber(1),
            "awayTeam" -> JsNumber(1)
          )
        )
      }
    }

    "return access token for POST requests with valid credentials to the /login path" in new Test {
      val credentials: String = JsObject(
        "username" -> JsString("user1"),
        "password" -> JsString("user1")
      ).toString()

      Post("/login", HttpEntity(ContentTypes.`application/json`, credentials)) ~> route ~> check {
        header("Access-Token") shouldBe defined
        status shouldEqual StatusCodes.OK
      }
    }

    "return Unauthorized for POST requests with invalid credentials to the /login path" in new Test {
      val credentials: String = JsObject(
        "username" -> JsString("invalid"),
        "password" -> JsString("invalid")
      ).toString()

      Post("/login", HttpEntity(ContentTypes.`application/json`, credentials)) ~> route ~> check {
        status shouldEqual StatusCodes.Unauthorized
      }
    }
  }
}
