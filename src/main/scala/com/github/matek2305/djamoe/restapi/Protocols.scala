package com.github.matek2305.djamoe.restapi

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

import com.github.matek2305.djamoe.domain.CompetitionCommand.{AddMatch, FinishMatch}
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchAdded, MatchFinished}
import com.github.matek2305.djamoe.domain.{MatchId, Score}
import com.github.matek2305.djamoe.restapi.RestApiRequest.LoginRequest
import com.github.matek2305.djamoe.restapi.RestApiResponse._
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait Protocols extends DefaultJsonProtocol {

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

  implicit val addMatchFormat: RootJsonFormat[AddMatch] = jsonFormat3(AddMatch)
  implicit val matchAddedFormat: RootJsonFormat[MatchAdded] = jsonFormat4(MatchAdded)

  implicit val scoreFormat: RootJsonFormat[Score] = jsonFormat2(Score)
  implicit val finishMatchFormat: RootJsonFormat[FinishMatch] = jsonFormat2(FinishMatch)
  implicit val matchFinishedFormat: RootJsonFormat[MatchFinished] = jsonFormat2(MatchFinished)

  implicit val betMadeFormat: RootJsonFormat[BetMade] = jsonFormat3(BetMade)

  implicit val playerPointsFormat: RootJsonFormat[PlayerPoints] = jsonFormat2(PlayerPoints)
  implicit val getPointsResponseFormat: RootJsonFormat[GetPointsResponse] = jsonFormat1(GetPointsResponse)

  implicit val matchResponseFormat: RootJsonFormat[MatchResponse] = jsonFormat8(MatchResponse)
  implicit val getMatchesResponseFormat: RootJsonFormat[GetMatchesResponse] = jsonFormat1(GetMatchesResponse)

  implicit val loginRequestFormat: RootJsonFormat[LoginRequest] = jsonFormat2(LoginRequest)
  implicit val loginResponseFormat: RootJsonFormat[LoginResponse] = jsonFormat1(LoginResponse)
}
