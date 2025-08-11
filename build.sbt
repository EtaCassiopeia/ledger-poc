ThisBuild / version      := "0.1"
ThisBuild / scalaVersion := "3.3.1"

// Common domain model shared between Scala and Java
lazy val common = (project in file("common"))
  .settings(
    name := "ledger-common",
    libraryDependencies ++= Seq(
      "dev.zio"        %% "zio"             % "2.1.20",
      "dev.zio"        %% "zio-schema"      % "1.7.4",
      "dev.zio"        %% "zio-schema-json" % "1.7.4",
      "dev.zio"        %% "zio-json"        % "0.7.3",
      "org.apache.avro" % "avro"            % "1.12.0"
    ),
    // Avro settings for sbt-avrohugger
    Compile / avroSourceDirectories         := Seq((Compile / sourceDirectory).value / "resources" / "avro"),
    Compile / avroSpecificSourceDirectories := Seq((Compile / sourceDirectory).value / "resources" / "avro"),
    Compile / sourceGenerators += (Compile / avroScalaGenerate).taskValue,
    Compile / sourceGenerators += (Compile / avroScalaGenerateSpecific).taskValue
  )

// Main application project
lazy val root = (project in file("."))
  .dependsOn(common)
  .settings(
    name := "ledger-sandbox-poc"
  )

// Instrument library project - contains only Java classes for sandbox
lazy val instrumentLib = (project in file("instrument-lib"))
  .enablePlugins(AssemblyPlugin)
  .dependsOn(common)
  .settings(
    name := "instrument-lib",
    // Only include necessary dependencies for instruments
    libraryDependencies ++= Seq(
      "org.graalvm.sdk" % "graal-sdk" % "24.2.2",
      "org.apache.avro" % "avro"      % "1.12.0"
    ),
    // Assembly settings for fat JAR
    assembly / assemblyJarName       := "card-1.0.jar",
    assembly / assemblyOutputPath    := file("jars/card-1.0.jar"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case _                             => MergeStrategy.first
    }
  )

libraryDependencies ++= Seq(
  "org.scala-lang"              %% "scala3-library"        % scalaVersion.value,
  "org.graalvm.sdk"              % "graal-sdk"             % "24.2.2",
  "org.graalvm.truffle"          % "truffle-api"           % "24.2.2",
  "org.graalvm.polyglot"         % "polyglot"              % "24.2.2",
  "org.graalvm.espresso"         % "espresso-language"     % "24.2.2" % "runtime",
  "dev.zio"                     %% "zio"                   % "2.1.20",
  "dev.zio"                     %% "zio-http"              % "3.3.3",
  "org.apache.avro"              % "avro"                  % "1.12.0",
  "software.amazon.awssdk"       % "s3"                    % "2.32.20",
  "com.softwaremill.sttp.tapir" %% "tapir-zio"             % "1.11.41",
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.11.41",
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio"        % "1.11.41"
)

// Enable GraalVM options
fork := true
javaOptions ++= Seq(
  "-XX:+UnlockExperimentalVMOptions",
  "-XX:+EnableJVMCI",
  "--add-opens=java.base/java.lang=ALL-UNNAMED",
  "--add-opens=java.base/java.util=ALL-UNNAMED",
  "--add-opens=java.base/java.text=ALL-UNNAMED",
  "--add-opens=java.base/java.time=ALL-UNNAMED",
  "--add-opens=java.base/java.io=ALL-UNNAMED",
  "--add-opens=java.base/java.nio=ALL-UNNAMED",
  "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED",
  "--add-opens=java.base/sun.misc=ALL-UNNAMED",
  "-Dpolyglot.engine.WarnInterpreterOnly=false",
  "-Djava.awt.headless=true",
  "-Dsun.misc.Signal.supported=false",
  "-XX:+DisableAttachMechanism",
  "-Xrs",         // Reduce signal usage
  "-XX:+UseG1GC", // Use G1 for better memory management
  "-Xmx2g"        // Set reasonable memory limit
)
