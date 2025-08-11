package com.ledger.model

import zio._
import zio.json._
import zio.schema.codec.JsonCodec

/**
 * JSON serialization utilities using ZIO JSON for REST API endpoints.
 * Internal communication between host and instruments uses Avro binary format.
 */
object Serialization {
  
  /**
   * ZIO JSON based serialization for REST API endpoints
   */
  object ZIOJson {
    
    def toJson[A](value: A)(implicit encoder: JsonEncoder[A]): String = {
      value.toJson
    }
    
    def fromJson[A](json: String)(implicit decoder: JsonDecoder[A]): Either[String, A] = {
      json.fromJson[A]
    }
    
    def fromJsonZIO[A](json: String)(implicit decoder: JsonDecoder[A]): Task[A] = {
      ZIO.fromEither(fromJson(json).left.map(error => new RuntimeException(error)))
    }
  }
  
  /**
   * ZIO Schema based JSON serialization - alternative approach
   */
  object ZIOSchema {
    
    def toJson[A](value: A)(implicit schema: zio.schema.Schema[A]): Task[String] = {
      ZIO.attempt(JsonCodec.jsonEncoder(schema).encodeJson(value).toString)
    }
    
    def fromJson[A](json: String)(implicit schema: zio.schema.Schema[A]): Task[A] = {
      ZIO.fromEither(
        JsonCodec.jsonDecoder(schema).decodeJson(json)
          .left.map(error => new RuntimeException(error))
      )
    }
  }
}
