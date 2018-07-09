package com.github.matek2305.djamoe

import java.time.{LocalDateTime, Month}

import akka.actor.ActorRef
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.testkit.{TestActor, TestProbe}
import com.github.matek2305.djamoe.CompetitionAggregate.{GetAllMatches, GetPoints}
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
          "Foo" -> JsNumber(5),
          "Bar" -> JsNumber(2),
          "Baz" -> JsNumber(0)
        )
      }
    }
  }
}
