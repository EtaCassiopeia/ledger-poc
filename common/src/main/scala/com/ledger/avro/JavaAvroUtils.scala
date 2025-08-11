package com.ledger.avro

import org.apache.avro.io.{BinaryDecoder, BinaryEncoder, DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

/**
 * Scala-based Avro utilities that can be called from Java instruments.
 * This bridges the gap between Java and Scala for Avro serialization.
 */
object JavaAvroUtils {

  /**
   * Deserializes binary Avro data to Message
   */
  def deserializeMessage(data: Array[Byte]): Message = {
    val reader = new SpecificDatumReader[Message](Message.SCHEMA$)
    val inputStream = new ByteArrayInputStream(data)
    val decoder = DecoderFactory.get().binaryDecoder(inputStream, null)
    reader.read(null, decoder)
  }

  /**
   * Serializes Result to binary Avro data
   */
  def serializeResult(result: Result): Array[Byte] = {
    val writer = new SpecificDatumWriter[Result](Result.SCHEMA$)
    val outputStream = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(outputStream, null)
    writer.write(result, encoder)
    encoder.flush()
    outputStream.toByteArray
  }

  /**
   * Creates a new Action
   */
  def createAction(actionType: String, data: String): Action = {
    new Action(actionType, data)
  }

  /**
   * Creates a new Result with actions from Java List
   */
  def createResultFromJavaList(actions: java.util.List[Action]): Result = {
    import scala.jdk.CollectionConverters._
    new Result(actions.asScala.toSeq)
  }

  /**
   * Creates a new Result with actions from array
   */
  def createResult(actions: Action*): Result = {
    new Result(actions.toSeq)
  }

  /**
   * Creates a new Result with a single action (Java convenience method)
   */
  def createResult(action: Action): Result = {
    new Result(Seq(action))
  }

  /**
   * Creates a new Result with two actions (Java convenience method)
   */
  def createResult(action1: Action, action2: Action): Result = {
    new Result(Seq(action1, action2))
  }

  /**
   * Creates a new Result with three actions (Java convenience method)
   */
  def createResult(action1: Action, action2: Action, action3: Action): Result = {
    new Result(Seq(action1, action2, action3))
  }

  /**
   * Creates a new Result with four actions (Java convenience method)
   */
  def createResult(action1: Action, action2: Action, action3: Action, action4: Action): Result = {
    new Result(Seq(action1, action2, action3, action4))
  }
}
