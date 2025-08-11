package com.ledger.model

import zio.schema._
import zio.json._

/**
 * Domain model for the Ledger Processing system.
 * Uses ZIO Schema and ZIO JSON for JSON REST API endpoints.
 * Internal communication uses Avro binary format.
 */

/**
 * Represents an input message to be processed
 */
case class Message(
  messageType: String,
  payload: String
)

object Message {
  implicit val schema: Schema[Message] = DeriveSchema.gen[Message]
  
  // ZIO JSON codecs with custom field mapping
  implicit val jsonEncoder: JsonEncoder[Message] = JsonEncoder.derived[Message].contramap { msg =>
    Message(msg.messageType, msg.payload)
  }
  
  implicit val jsonDecoder: JsonDecoder[Message] = JsonDecoder.derived[Message]
}

/**
 * Represents an action to be performed as a result of processing
 */
case class Action(
  actionType: String,
  data: String
)

object Action {
  implicit val schema: Schema[Action] = DeriveSchema.gen[Action]
  implicit val jsonEncoder: JsonEncoder[Action] = JsonEncoder.derived[Action]
  implicit val jsonDecoder: JsonDecoder[Action] = JsonDecoder.derived[Action]
}

/**
 * Represents the result of processing a message
 */
case class Result(
  actions: List[Action]
)

object Result {
  implicit val schema: Schema[Result] = DeriveSchema.gen[Result]
  implicit val jsonEncoder: JsonEncoder[Result] = JsonEncoder.derived[Result]
  implicit val jsonDecoder: JsonDecoder[Result] = JsonDecoder.derived[Result]
}

/**
 * Processing context information
 */
case class ProcessingContext(
  businessLine: String,
  version: String,
  requestId: Option[String] = None,
  timestamp: Long = System.currentTimeMillis()
)

object ProcessingContext {
  implicit val schema: Schema[ProcessingContext] = DeriveSchema.gen[ProcessingContext]
  implicit val jsonEncoder: JsonEncoder[ProcessingContext] = JsonEncoder.derived[ProcessingContext]
  implicit val jsonDecoder: JsonDecoder[ProcessingContext] = JsonDecoder.derived[ProcessingContext]
}
