package com.github.matek2305.djamoe

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import com.github.matek2305.djamoe.CompetitionAggregate.{BetMade, MatchCreated, MatchFinished}
import com.github.matek2305.djamoe.CompetitionRestService.{GetPointsResponse, MatchResponse, PlayerPoints}
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait JsonSupport extends SprayJsonSupport with DefaultJsonProtocol {

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

  implicit val matchFormat: RootJsonFormat[Match] = jsonFormat3(Match)
  implicit val matchCreatedFormat: RootJsonFormat[MatchCreated] = jsonFormat2(MatchCreated)
  implicit val matchScoreFormat: RootJsonFormat[MatchScore] = jsonFormat2(MatchScore)
  implicit val betFormat: RootJsonFormat[Bet] = jsonFormat2(Bet)
  implicit val betMadeFormat: RootJsonFormat[BetMade] = jsonFormat2(BetMade)
  implicit val matchFinishedFormat: RootJsonFormat[MatchFinished] = jsonFormat2(MatchFinished)

  implicit val playerPointsFormat: RootJsonFormat[PlayerPoints] = jsonFormat2(PlayerPoints)
  implicit val getPointsResponseFormat: RootJsonFormat[GetPointsResponse] = jsonFormat1(GetPointsResponse)
  implicit val matchResponseForm: RootJsonFormat[MatchResponse] = jsonFormat2(MatchResponse)
}
