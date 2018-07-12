package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}
import java.util.UUID

import akka.actor.ActorRef
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActor, TestProbe}
import com.github.matek2305.djamoe.CompetitionAggregate.{CreateMatch, GetAllMatches, GetPoints, MatchCreated}
import org.scalatest.{Matchers, WordSpec}
import spray.json._

class CompetitionRestServiceSpec extends WordSpec with Matchers with ScalatestRouteTest {

  "Competition rest service" should {
    val probe = TestProbe()
    val service = new CompetitionRestService(probe.ref)

    "return list of all matches for GET requests to the /matches path" in {

      probe.setAutoPilot((sender: ActorRef, _: Any) => {
        sender ! List(
          MatchState(Match(
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

        responseAs[String].parseJson shouldEqual JsArray(JsObject(
          "homeTeamName" -> JsString("France"),
          "awayTeamName" -> JsString("Belgium"),
          "startDate" -> JsString("2018-07-10T20:00:00")
        ))
      }
    }

    "return players points for GET requests to the /points path" in {
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
          "data" -> JsArray(
            JsObject(
              "playerName" -> JsString("Foo"),
              "points" -> JsNumber(5)
            ),
            JsObject(
              "playerName" -> JsString("Bar"),
              "points" -> JsNumber(2)
            ),
            JsObject(
              "playerName" -> JsString("Baz"),
              "points" -> JsNumber(0)
            )
          )
        )
      }
    }

    "create match in competition for POST requests to the /matches path" in {
      val uuid = UUID.randomUUID()
      probe.setAutoPilot((sender: ActorRef, _: Any) => {
        sender ! MatchCreated(
          MatchId(uuid),
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
          "id" -> JsString(uuid.toString),
          "details" -> JsObject(
            "homeTeamName" -> JsString("France"),
            "awayTeamName" -> JsString("Belgium"),
            "startDate" -> JsString("2018-07-10T20:00:00")
          )
        )
      }
    }

    "return passed id for GET requests to the /matches/:id/bets path" in {
      val id = 123
      Get(s"/matches/$id/bets") ~> service.route ~> check {
        status shouldEqual StatusCodes.OK
        responseAs[String] should include (id.toString)
      }
    }
  }
}
