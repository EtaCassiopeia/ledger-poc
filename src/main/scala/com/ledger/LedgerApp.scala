package com.ledger

import zio.*
import zio.http.*
import zio.logging.backend.SLF4J

import com.ledger.model.*
import com.ledger.model.Serialization.ZIOJson
import com.ledger.avro.AvroSerialization
import com.ledger.service.InstrumentService
import com.ledger.runtime.PolyglotRuntime

object LedgerApp extends ZIOAppDefault {

  // Side-effectful actions for demo purposes
  private def performActions(result: Result): ZIO[Any, Throwable, Unit] =
    ZIO.foreachDiscard(result.actions) { action =>
      ZIO.logInfo(
        s"Performing action: ${action.actionType} with data ${action.data}"
      )
    }

  private val processRoute = Method.POST / "process" -> handler {
    (req: Request) =>
      (for {
        businessLine <- ZIO
          .fromOption(req.headers.get("Business-Line"))
          .orElseFail("Missing Business-Line header")
        version <- ZIO
          .fromOption(req.headers.get("Version"))
          .orElseFail("Missing Version header")
        body <- req.body.asString

        // Parse input JSON to domain
        message <- ZIOJson.fromJsonZIO[Message](body)

        // Convert to Avro and serialize to binary
        avroMessage <- ZIO.attempt(AvroSerialization.jsonToAvroMessage(message))
        avroInput   <- ZIO.attempt(AvroSerialization.serialize(avroMessage))

        // Process using sandboxed instrument via ZIO service
        avroOutput <- ZIO.serviceWithZIO[InstrumentService](
          _.process(businessLine, version, avroInput)
        )

        // Deserialize Avro result and convert back to JSON model
        avroResult <- ZIO.attempt(
          AvroSerialization.deserialize(
            avroOutput,
            com.ledger.avro.Result.SCHEMA$,
            classOf[com.ledger.avro.Result]
          )
        )
        result <- ZIO.attempt(AvroSerialization.avroToJsonResult(avroResult))

        _ <- performActions(result)
      } yield Response.text("Processed")).catchAll {
        case e: Throwable =>
          val sw = new java.io.StringWriter()
          val pw = new java.io.PrintWriter(sw)
          e.printStackTrace(pw)
          pw.flush()
          val details = sw.toString
          ZIO.succeed(
            Response
              .text(
                s"Error: ${Option(e.getMessage).getOrElse(e.getClass.getName)}\n\n$details"
              )
              .status(Status.InternalServerError)
          )
        case errorMsg: String =>
          ZIO.succeed(Response.text(errorMsg).status(Status.BadRequest))
      }
  }

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = {
    val app = Routes(processRoute)
    ZIO.logInfo("Starting the server...") *>
      Server
        .serve(app)
        .provide(
          Server.defaultWithPort(8080),
          SLF4J.slf4j,
          PolyglotRuntime.live,
          InstrumentService.live
        ) <*
      ZIO.logInfo("Server started")
  }
}
