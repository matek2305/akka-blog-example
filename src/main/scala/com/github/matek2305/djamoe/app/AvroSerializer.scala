package com.github.matek2305.djamoe.app

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

import akka.serialization.SerializerWithStringManifest
import com.github.matek2305.djamoe.domain.CompetitionEvent.MatchFinished
import com.github.matek2305.djamoe.domain.Score
import com.sksamuel.avro4s.{AvroInputStream, AvroOutputStream}

class AvroSerializer extends SerializerWithStringManifest {

  override def identifier: Int = 100

  override def manifest(o: AnyRef): String = o.getClass.getName

  final val MatchFinishedManifest = classOf[Score].getName

  override def toBinary(o: AnyRef): Array[Byte] = o match {
    case finished: Score =>
      val baos = new ByteArrayOutputStream()
      val output = AvroOutputStream.binary[Score](baos)
      output.write(finished)
      output.close()
      baos.toByteArray
    case _ => throw new IllegalArgumentException(s"Unknown event to serialize: $o")
  }

  override def fromBinary(bytes: Array[Byte], manifest: String): AnyRef = {
    manifest match {
      case MatchFinishedManifest =>
        val is = new ByteArrayInputStream(bytes)
        val input = AvroInputStream.binary[Score](is)
        is.close()
        input.iterator.toSeq.head
      case _ => throw new IllegalArgumentException(s"Unable to handle manifest: $manifest")
    }
  }
}
