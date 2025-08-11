package com.ledger.avro

import org.apache.avro.io.{DecoderFactory, EncoderFactory}
import org.apache.avro.specific.{SpecificDatumReader, SpecificDatumWriter, SpecificRecordBase}
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}

/**
 * Avro serialization utilities for communication between host application and instruments.
 * Provides binary serialization/deserialization for efficient data transfer.
 */
object AvroSerialization {

  /**
   * Serializes an Avro record to binary format
   */
  def serialize[T <: SpecificRecordBase](record: T): Array[Byte] = {
    val writer = new SpecificDatumWriter[T](record.getSchema)
    val outputStream = new ByteArrayOutputStream()
    val encoder = EncoderFactory.get().binaryEncoder(outputStream, null)
    
    writer.write(record, encoder)
    encoder.flush()
    outputStream.close()
    
    outputStream.toByteArray
  }

  /**
   * Deserializes binary data to an Avro record
   */
  def deserialize[T <: SpecificRecordBase](data: Array[Byte], schema: org.apache.avro.Schema, recordClass: Class[T]): T = {
    val reader = new SpecificDatumReader[T](schema)
    val inputStream = new ByteArrayInputStream(data)
    val decoder = DecoderFactory.get().binaryDecoder(inputStream, null)
    
    reader.read(null.asInstanceOf[T], decoder)
  }

  /**
   * Converts JSON Message to Avro Message
   */
  def jsonToAvroMessage(jsonMessage: com.ledger.model.Message): Message = {
    new Message(jsonMessage.messageType, jsonMessage.payload)
  }

  /**
   * Converts Avro Result to JSON Result
   */
  def avroToJsonResult(avroResult: Result): com.ledger.model.Result = {
    val actions = avroResult.actions.map { avroAction =>
      com.ledger.model.Action(avroAction.actionType, avroAction.data)
    }.toList
    com.ledger.model.Result(actions)
  }

  /**
   * Converts JSON Result to Avro Result
   */
  def jsonToAvroResult(jsonResult: com.ledger.model.Result): Result = {
    val avroActions = jsonResult.actions.map { action =>
      new Action(action.actionType, action.data)
    }
    new Result(avroActions)
  }

  /**
   * Converts Avro Message to JSON Message
   */
  def avroToJsonMessage(avroMessage: Message): com.ledger.model.Message = {
    com.ledger.model.Message(avroMessage.messageType, avroMessage.payload)
  }
}

