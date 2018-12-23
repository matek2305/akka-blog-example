package com.github.matek2305.djamoe

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.util.UUID

import akka.serialization.SerializerWithStringManifest
import com.github.matek2305.djamoe.auth.RegisterResponse.UserRegistered
import com.github.matek2305.djamoe.domain.CompetitionEvent.{BetMade, MatchAdded, MatchFinished}
import com.github.matek2305.djamoe.domain.MatchId
import com.sksamuel.avro4s._
import org.apache.avro.Schema

class AvroSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 100

  override def manifest(o: AnyRef): String = o.getClass.getName

  final val UserRegisteredManifest = classOf[UserRegistered].getName
  final val MatchAddedManifest = classOf[MatchAdded].getName
  final val MatchFinishedManifest = classOf[MatchFinished].getName
  final val BetMadeManifest = classOf[BetMade].getName

  implicit object MatchIdFromValue extends FromValue[MatchId] {
    override def apply(value: Any, field: Schema.Field): MatchId = {
      MatchId(UUID.fromString(value.toString))
    }
  }

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case event: UserRegistered => toBinaryByClass(event)
    case event: MatchAdded => toBinaryByClass(event)
    case event: MatchFinished => toBinaryByClass(event)
    case event: BetMade => toBinaryByClass(event)
    case _ => throw new IllegalArgumentException(s"Unknown event to serialize: $o")
  }

  def toBinaryByClass[T: SchemaFor : ToRecord](event: T): Array[Byte] = {
    val os = new ByteArrayOutputStream()
    val output = AvroOutputStream.binary[T](os)

    output.write(event)
    output.flush()
    output.close()

    os.toByteArray
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case UserRegisteredManifest => fromBinaryByClass[UserRegistered](bytes)
      case MatchAddedManifest => fromBinaryByClass[MatchAdded](bytes)
      case MatchFinishedManifest => fromBinaryByClass[MatchFinished](bytes)
      case BetMadeManifest => fromBinaryByClass[BetMade](bytes)
      case _ => throw new IllegalArgumentException(s"Unable to handle manifest: $manifest")
    }
  }

  def fromBinaryByClass[T: SchemaFor : FromRecord](bytes: Array[Byte]): T = {
    val is = new ByteArrayInputStream(bytes)
    val input = AvroInputStream.binary[T](is)
    is.close()
    input.iterator.next()
  }
}
