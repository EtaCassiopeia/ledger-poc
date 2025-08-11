package com.ledger.service

import com.ledger.Instrument
import com.ledger.runtime.PolyglotRuntime
import org.graalvm.polyglot.Value
import org.graalvm.polyglot.proxy.ProxyArray
import zio._

import java.io.File

final case class InstrumentService(
    runtime: PolyglotRuntime,
    cache: Ref.Synchronized[Map[(String, String), InstrumentService.Executor]]
) {
  import InstrumentService._

  private def jarPath(bl: String, v: String): String = new File(
    s"./jars/${bl.toLowerCase}-$v.jar"
  ).getAbsolutePath

  private def load(bl: String, v: String): Task[Executor] = for {
    context    <- runtime.createSecuredContext(jarPath(bl, v))
    instrument <- ZIO
      .attemptBlocking {
        val javaBindings: Value = context.getBindings("java")
        val classClass: Value   = javaBindings.getMember("java.lang.Class")
        val klass: Value        = classClass.invokeMember(
          "forName",
          s"com.ledger.${bl.capitalize}Instrument"
        )
        val ctor: Value     = klass.invokeMember("getDeclaredConstructor")
        val instance: Value = ctor.invokeMember("newInstance")
        val name            = s"${bl}Instrument"
        context.getPolyglotBindings.putMember(name, instance)
        val wrapper = new Instrument {
          override def process(avroInput: Array[Byte]): Array[Byte] = {
            val boxed = new Array[AnyRef](avroInput.length)
            var i     = 0
            while (i < avroInput.length) {
              boxed(i) = java.lang.Byte.valueOf(avroInput(i)); i += 1
            }
            val proxy  = ProxyArray.fromArray(boxed: _*)
            val result = instance.invokeMember("process", proxy)
            result.as(classOf[Array[Byte]])
          }
        }
        Executor(context, wrapper)
      }
      .mapError(e =>
        new RuntimeException(
          s"Failed to load instrument $bl-$v: ${e.getMessage}",
          e
        )
      )
  } yield instrument

  def get(bl: String, v: String): Task[Executor] = {
    val key = (bl.toLowerCase, v)
    cache.modifyZIO { current =>
      current.get(key) match {
        case Some(exec) => ZIO.succeed((exec, current))
        case None       =>
          load(bl, v).map { exec =>
            (exec, current.updated(key, exec))
          }
      }
    }
  }

  def process(
      bl: String,
      v: String,
      avroInput: Array[Byte]
  ): Task[Array[Byte]] = for {
    exec <- get(bl, v)
    out  <- ZIO.attemptBlocking(exec.instrument.process(avroInput))
  } yield out
}

object InstrumentService {
  final case class Executor(
      context: org.graalvm.polyglot.Context,
      instrument: Instrument
  )

  val live: ZLayer[PolyglotRuntime, Nothing, InstrumentService] =
    ZLayer.fromZIO {
      for {
        rt  <- ZIO.service[PolyglotRuntime]
        ref <- Ref.Synchronized.make(Map.empty[(String, String), Executor])
      } yield InstrumentService(rt, ref)
    }
}
