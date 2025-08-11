package com.ledger

import org.graalvm.polyglot.*
import org.graalvm.polyglot.io.IOAccess
import zio.*
import zio.http.*

import java.util.concurrent.ConcurrentHashMap
import java.net.{URL, URLClassLoader}
import java.io.File

// Import shared domain model
import com.ledger.model._
import com.ledger.model.Serialization.ZIOJson
import com.ledger.avro.AvroSerialization

object LedgerApp extends ZIOAppDefault {

  case class InstrumentExecutor(
      context: Context,
      instrument: com.ledger.Instrument
  )

  private val cache =
    new ConcurrentHashMap[(String, String), InstrumentExecutor]()

  // Create a secured GraalVM context
  private def createSecuredContext(): Context = {
    Context
      .newBuilder("java")
      .allowHostAccess(HostAccess.NONE)
      .allowIO(IOAccess.NONE)
      .allowNativeAccess(false)
      .allowCreateProcess(false)
      .allowAllAccess(false)
      .allowExperimentalOptions(true)
      .option("engine.WarnInterpreterOnly", "false")
      .build()
  }

  private def getJarPath(businessLine: String, version: String): String = {
    s"./jars/${businessLine.toLowerCase}-$version.jar"
  }

  // Load instrument class from JAR and create instance in secured context
  private def loadInstrumentFromJar(
      businessLine: String,
      version: String
  ): InstrumentExecutor = {
    val jarPath = getJarPath(businessLine, version)
    val jarFile = new File(jarPath)

    if (!jarFile.exists()) {
      throw new RuntimeException(s"JAR file not found at path: $jarPath")
    }

    val context = createSecuredContext()

    try {
      // Load the instrument class from JAR
      val jarUrl      = jarFile.toURI.toURL
      val classLoader = new URLClassLoader(
        Array(jarUrl),
        this.getClass.getClassLoader
      )

      val className       = s"com.ledger.${businessLine.capitalize}Instrument"
      val instrumentClass = classLoader.loadClass(className)
      val instrumentInstance = instrumentClass
        .getDeclaredConstructor()
        .newInstance()
        .asInstanceOf[com.ledger.Instrument]

      // Put the instance into polyglot bindings
      val memberName = s"${businessLine.capitalize}Instrument"
      context.getPolyglotBindings.putMember(memberName, instrumentInstance)
      val sandboxedInstrument = context.getPolyglotBindings
        .getMember(memberName)
        .as(classOf[com.ledger.Instrument])

      InstrumentExecutor(context, sandboxedInstrument)

    } catch {
      case e: Exception =>
        try {
          context.close()
        } catch {
          case _: Exception => // Ignore close errors
        }
        throw new RuntimeException(
          s"Failed to load instrument $businessLine-$version: ${e.getMessage}",
          e
        )
    }
  }

  private def getOrLoad(
      businessLine: String,
      version: String
  ): ZIO[Any, Throwable, com.ledger.Instrument] = ZIO.attemptBlocking {
    val key      = (businessLine.toLowerCase, version)
    val executor = cache.computeIfAbsent(
      key,
      _ => loadInstrumentFromJar(businessLine, version)
    )
    executor.instrument
  }

  // Mock effects: In real app, use ZIO services
  private def performActions(result: Result): ZIO[Any, Throwable, Unit] =
    ZIO.foreachDiscard(result.actions) { action =>
      ZIO.logInfo(
        s"Performing action: ${action.actionType} with data ${action.data}"
      )
    }

  // ZIO HTTP endpoint
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

        // Create message using shared domain model
        message <- ZIOJson.fromJsonZIO[Message](body)

        // Convert to Avro and serialize to binary
        avroMessage <- ZIO.attempt(AvroSerialization.jsonToAvroMessage(message))
        avroInput   <- ZIO.attempt(AvroSerialization.serialize(avroMessage))

        // Process using sandboxed instrument with Avro binary data
        instrument <- getOrLoad(businessLine, version)
        avroOutput <- ZIO.attemptBlocking(instrument.process(avroInput))

        // Deserialize Avro result and convert back to JSON model
        avroResult <- ZIO.attempt(
          AvroSerialization.deserialize(
            avroOutput,
            com.ledger.avro.Result.SCHEMA$,
            classOf[com.ledger.avro.Result]
          )
        )
        result <- ZIO.attempt(AvroSerialization.avroToJsonResult(avroResult))

        // Perform actions
        _ <- performActions(result)
      } yield Response.text("Processed")).catchAll {
        case e: Throwable =>
          ZIO.succeed(
            Response
              .text(s"Error: ${e.getMessage}")
              .status(Status.InternalServerError)
          )
        case errorMsg: String =>
          ZIO.succeed(Response.text(errorMsg).status(Status.BadRequest))
      }
  }

  override def run: ZIO[Any & ZIOAppArgs & Scope, Any, Any] = {
    val app = Routes(processRoute)
    Server.serve(app).provide(Server.defaultWithPort(8080))
  }
}
