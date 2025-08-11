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
      "dev.zio"        %% "zio-json"        % "0.7.44",
      "org.apache.avro" % "avro"            % "1.12.0"
    ),
    Compile / avroSourceDirectories := Seq(
      (Compile / sourceDirectory).value / "resources" / "avro"
    ),
    Compile / avroSpecificSourceDirectories := Seq(
      (Compile / sourceDirectory).value / "resources" / "avro"
    ),
    Compile / sourceGenerators += (Compile / avroScalaGenerate).taskValue,
    Compile / sourceGenerators += (Compile / avroScalaGenerateSpecific).taskValue
  )

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
    libraryDependencies ++= Seq(
      "org.graalvm.sdk" % "graal-sdk" % "24.2.2",
      "org.apache.avro" % "avro"      % "1.12.0"
    ),
    assembly / assemblyJarName       := "card-1.0.jar",
    assembly / assemblyOutputPath    := file("jars/card-1.0.jar"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", xs @ _*) => MergeStrategy.discard
      case _                             => MergeStrategy.first
    }
  )

libraryDependencies ++= Seq(
  "org.scala-lang"      %% "scala3-library"    % scalaVersion.value,
  "org.graalvm.sdk"      % "graal-sdk"         % "24.2.2",
  "org.graalvm.truffle"  % "truffle-api"       % "24.2.2",
  "org.graalvm.polyglot" % "polyglot"          % "24.2.2",
  "org.graalvm.polyglot" % "java-community"    % "24.2.2",
  "org.graalvm.espresso" % "espresso-language" % "24.2.2",
  "org.graalvm.espresso" % "espresso-runtime-resources-jdk21" % "24.2.2",
  "dev.zio"             %% "zio"                              % "2.1.20",
  "dev.zio"             %% "zio-http"                         % "3.4.0",
  "dev.zio"             %% "zio-logging"                      % "2.5.1",
  "dev.zio"             %% "zio-logging-slf4j2"               % "2.5.1",
  "org.slf4j"            % "slf4j-api"                        % "2.0.17",
  "ch.qos.logback"       % "logback-classic"                  % "1.5.18",
  "org.apache.avro"      % "avro"                             % "1.12.0",
  "com.softwaremill.sttp.tapir" %% "tapir-zio"             % "1.11.42",
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % "1.11.42",
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio"        % "1.11.42"
)

fork := true
