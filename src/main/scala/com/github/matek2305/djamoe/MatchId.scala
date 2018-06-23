package com.github.matek2305.djamoe

import java.util.UUID

import io.circe.{Decoder, Encoder}

class MatchId(val id: UUID) extends AnyVal {
  override def toString: String = id.toString
}

object MatchId {
  def apply(): MatchId = new MatchId(UUID.randomUUID())
  def apply(id: UUID): MatchId = new MatchId(id)

  implicit val matchIdDecoder: Decoder[MatchId] = Decoder.decodeUUID.map(MatchId(_))
  implicit val matchIdEncoder: Encoder[MatchId] = Encoder.encodeUUID.contramap(_.id)
}
