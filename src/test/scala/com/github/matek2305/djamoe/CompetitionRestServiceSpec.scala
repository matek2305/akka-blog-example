package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActor, TestProbe}
import com.github.matek2305.djamoe.CompetitionAggregate._
import org.scalatest.{Matchers, WordSpec}
import spray.json._

class CompetitionRestServiceSpec extends WordSpec with Matchers with ScalatestRouteTest {

  "Competition rest service" should {
    "return list of all matches for GET requests to the /matches path" in {
      val probe = TestProbe()
      val service = new CompetitionRestService(probe.ref)

      val matchId = MatchId()
      probe.setAutoPilot((sender: ActorRef, _: Any) => {
        sender ! Map(
          matchId -> MatchState(Match(
            "France",
            "Belgium",
            LocalDateTime.of(2018, Month.JULY, 10, 20, 0))
          )
        )
        TestActor.KeepRunning
      })

      Get("/matches") ~> service.route ~> check {
        probe.expectMsg(GetAllMatches())

        status shouldEqual StatusCodes.OK

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

    "return players points for GET requests to the /points path" in {
      val probe = TestProbe()
      val service = new CompetitionRestService(probe.ref)

      probe.setAutoPilot((sender: ActorRef, _: Any) => {
        sender ! Map(
          "Foo" -> 5,
          "Bar" -> 2,
          "Baz" -> 0
        )
        TestActor.KeepRunning
      })

      Get("/points") ~> service.route ~> check {
        probe.expectMsg(GetPoints())

        status shouldEqual StatusCodes.OK

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

    "create match in competition for POST requests to the /matches path" in {
      val probe = TestProbe()
      val service = new CompetitionRestService(probe.ref)

      val matchId = MatchId()
      probe.setAutoPilot((sender: ActorRef, _: Any) => {
        sender ! MatchCreated(
          matchId,
          Match(
            "France",
            "Belgium",
            LocalDateTime.of(2018, Month.JULY, 10, 20, 0)
          )
        )
        TestActor.KeepRunning
      })

      val content = JsObject(
        "homeTeamName" -> JsString("France"),
        "awayTeamName" -> JsString("Belgium"),
        "startDate" -> JsString("2018-07-10T20:00:00")
      ).toString()

      Post("/matches", HttpEntity(ContentTypes.`application/json`, content)) ~> service.route ~> check {
        probe.expectMsg(
          CreateMatch(
            Match(
              "France",
              "Belgium",
              LocalDateTime.of(2018, Month.JULY, 10, 20, 0)
            )
          )
        )

        status shouldEqual StatusCodes.Created

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

    "create bet for match for POST requests to the /matches/:id/bets path" in {
      val probe = TestProbe()
      val service = new CompetitionRestService(probe.ref)

      val matchId = MatchId()
      probe.setAutoPilot((sender: ActorRef, _: Any) => {
        sender ! BetMade(matchId, Bet("Foo", MatchScore(1, 2)))
        TestActor.KeepRunning
      })

      val content = JsObject(
        "who" -> JsString("Foo"),
        "score" -> JsObject(
          "homeTeam" -> JsNumber(1),
          "awayTeam" -> JsNumber(2)
        )
      ).toString()

      Post(s"/matches/$matchId/bets", HttpEntity(ContentTypes.`application/json`, content)) ~> service.route ~> check {
        probe.expectMsg(MakeBet(matchId, Bet("Foo", MatchScore(1, 2))))

        status shouldEqual StatusCodes.Created

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

    "finish match for POST requests to the /matches/:id/score path" in {
      val probe = TestProbe()
      val service = new CompetitionRestService(probe.ref)

      val matchId = MatchId()
      probe.setAutoPilot((sender: ActorRef, _: Any) => {
        sender ! MatchFinished(matchId, MatchScore(1, 1))
        TestActor.KeepRunning
      })

      val content = JsObject(
        "homeTeam" -> JsNumber(1),
        "awayTeam" -> JsNumber(1)
      ).toString()

      Post(s"/matches/$matchId/score", HttpEntity(ContentTypes.`application/json`, content)) ~> service.route ~> check {
        probe.expectMsg(FinishMatch(matchId, MatchScore(1, 1)))

        status shouldEqual StatusCodes.Created

        responseAs[String].parseJson shouldEqual JsObject(
          "id" -> JsString(matchId.toString),
          "score" -> JsObject(
            "homeTeam" -> JsNumber(1),
            "awayTeam" -> JsNumber(1)
          )
        )
      }
    }
  }
}
