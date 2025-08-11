package com.ledger.runtime

import org.graalvm.polyglot.{Context, Engine, HostAccess, PolyglotAccess}
import org.graalvm.polyglot.io.IOAccess
import zio._

final case class PolyglotRuntime(engine: Engine, guestJdkHome: String) {
  def createSecuredContext(classpath: String): UIO[Context] = ZIO.succeed {
    Context
      .newBuilder("java")
      .allowHostAccess(
        HostAccess
          .newBuilder(HostAccess.NONE)
          .allowImplementationsAnnotatedBy(
            classOf[org.graalvm.polyglot.HostAccess.Implementable]
          )
          .build()
      )
      .allowIO(IOAccess.NONE)
      .allowNativeAccess(true) // Needed for nfi-dlmopen
      .allowCreateProcess(false)
      .allowCreateThread(true)
      .allowExperimentalOptions(true)
      .allowPolyglotAccess(
        PolyglotAccess.newBuilder().allowBindingsAccess("java").build()
      )
      .engine(engine)
      .option("java.JavaHome", guestJdkHome)
      .option("java.Classpath", classpath)
      .option("java.NativeBackend", "nfi-dlmopen")
      .option("java.Properties.jdk.jar.disableSignatureVerification", "true")
      .option("java.Polyglot", "true")
      .option("java.EnableGenericTypeHints", "true")
      .build()
  }
}

object PolyglotRuntime {
  val live: ULayer[PolyglotRuntime] = ZLayer.succeedEnvironment(
    ZEnvironment(
      PolyglotRuntime(
        Engine.newBuilder().allowExperimentalOptions(true).build(),
        "/opt/graalvm"
      )
    )
  )
}
