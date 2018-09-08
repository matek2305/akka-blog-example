package com.github.matek2305.djamoe.restapi

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.matek2305.djamoe.domain.CompetitionCommand.AddMatch
import com.github.matek2305.djamoe.domain.CompetitionEvent.MatchAdded
import com.github.matek2305.djamoe.domain.MatchId
import com.github.matek2305.djamoe.restapi.CompetitionRestApiResponse.{GetMatchesResponse, GetPointsResponse, MatchResponse, PlayerPoints}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait SprayJsonConfig extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object LocalDateTimeJsonFormat extends RootJsonFormat[LocalDateTime] {

    override def read(json: JsValue): LocalDateTime = json match {
      case JsString(str) => LocalDateTime.parse(str, DateTimeFormatter.ISO_DATE_TIME)
      case _ => throw DeserializationException("Expected ISO-8601 date time format")
    }

    override def write(localDateTime: LocalDateTime): JsValue =
      JsString(localDateTime.format(DateTimeFormatter.ISO_DATE_TIME))
  }

  implicit object MatchIdJsonFormat extends RootJsonFormat[MatchId] {

    override def read(json: JsValue): MatchId = json match {
      case JsString(str) => MatchId(UUID.fromString(str))
      case _ => throw DeserializationException("Expected UUID string")
    }

    override def write(matchId: MatchId): JsValue = JsString(matchId.toString)
  }

  implicit val matchResponseFormat: RootJsonFormat[MatchResponse] = jsonFormat5(MatchResponse)
  implicit val getMatchesResponseFormat: RootJsonFormat[GetMatchesResponse] = jsonFormat1(GetMatchesResponse)
  implicit val playerPointsFormat: RootJsonFormat[PlayerPoints] = jsonFormat2(PlayerPoints)
  implicit val getPointsResponseFormat: RootJsonFormat[GetPointsResponse] = jsonFormat1(GetPointsResponse)

  implicit val addMatchFormat: RootJsonFormat[AddMatch] = jsonFormat3(AddMatch)
  implicit val matchAddedFormat: RootJsonFormat[MatchAdded] = jsonFormat4(MatchAdded)
}
