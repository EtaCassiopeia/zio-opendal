import sbt._

object Dependencies {

  // Version definitions
  object Versions {
    // Core versions
    val scala213 = "2.13.16"
    val scala33  = "3.3.4" // LTS
    val scala34  = "3.4.3"
    val scala35  = "3.5.2"

    // Primary dependencies
    val zio         = "2.1.20"
    val opendal     = "0.47.0"
    val opendalJava = "0.46.4"

    // Test dependencies
    val scalatest      = "3.2.19"
    val testcontainers = "0.41.5"
  }

  // Core ZIO ecosystem dependencies
  object ZIO {
    val core         = "dev.zio" %% "zio"               % Versions.zio
    val streams      = "dev.zio" %% "zio-streams"       % Versions.zio
    val test         = "dev.zio" %% "zio-test"          % Versions.zio
    val testSbt      = "dev.zio" %% "zio-test-sbt"      % Versions.zio
    val testMagnolia = "dev.zio" %% "zio-test-magnolia" % Versions.zio
    val json         = "dev.zio" %% "zio-json"          % "0.7.3"
    val config       = "dev.zio" %% "zio-config"        % "4.0.2"
    val logging      = "dev.zio" %% "zio-logging"       % "2.4.0"
  }

  // Apache OpenDAL dependencies
  object OpenDAL {
    val core = "org.apache.opendal" % "opendal" % Versions.opendal

    // Platform-specific native libraries
    def java(classifier: String) =
      "org.apache.opendal" % "opendal-java" % Versions.opendalJava classifier classifier
  }

  // Test dependencies
  object Test {
    val zioTest         = ZIO.test         % sbt.Test
    val zioTestSbt      = ZIO.testSbt      % sbt.Test
    val zioTestMagnolia = ZIO.testMagnolia % sbt.Test
    val scalatest       = "org.scalatest" %% "scalatest"                      % Versions.scalatest      % sbt.Test
    val testcontainers  = "com.dimafeng"  %% "testcontainers-scala-scalatest" % Versions.testcontainers % sbt.Test
  }

  // Integration test dependencies
  object IntegrationTest {
    val zioTest         = ZIO.test         % "it"
    val zioTestSbt      = ZIO.testSbt      % "it"
    val zioTestMagnolia = ZIO.testMagnolia % "it"
    val testcontainers  = "com.dimafeng"  %% "testcontainers-scala-scalatest" % Versions.testcontainers % "it"
    val localstack      = "com.dimafeng"  %% "testcontainers-scala-localstack" % Versions.testcontainers % "it"
    val awsS3           = "software.amazon.awssdk" % "s3"              % "2.29.35" % "it"
    val awsCore         = "software.amazon.awssdk" % "sdk-core"        % "2.29.35" % "it"
    val awsAuth         = "software.amazon.awssdk" % "auth"            % "2.29.35" % "it"
    val logbackClassic  = "ch.qos.logback"         % "logback-classic" % "1.5.15" % "it"
  }

  // Example/documentation dependencies
  object Examples {
    val zioCore    = ZIO.core
    val zioJson    = ZIO.json
    val zioConfig  = ZIO.config
    val zioLogging = ZIO.logging
  }

  // Compiler plugins and tools
  object CompilerPlugins {
    val betterMonadicFor = "com.olegpy"   %% "better-monadic-for" % "0.3.1"
    val semanticdb       = "org.scalameta" % "semanticdb-scalac"  % "4.9.9" cross CrossVersion.full
  }

  // Grouped dependency sets for easy inclusion
  object Groups {
    val core: Seq[ModuleID] = Seq(
      ZIO.core,
      ZIO.streams,
      OpenDAL.core
    )

    val testing: Seq[ModuleID] = Seq(
      Test.zioTest,
      Test.zioTestSbt,
      Test.zioTestMagnolia
    )

    val examples: Seq[ModuleID] = Seq(
      Examples.zioCore,
      Examples.zioJson,
      Examples.zioConfig,
      Examples.zioLogging
    )
  }

  // Platform detection utility
  object Platform {
    lazy val classifier: String = {
      val osName = System.getProperty("os.name").toLowerCase
      val osArch = System.getProperty("os.arch").toLowerCase

      (osName, osArch) match {
        case (os, arch) if os.contains("mac") && (arch == "aarch64" || arch == "arm64")   =>
          "osx-aarch_64"
        case (os, _) if os.contains("mac")                                                =>
          "osx-x86_64"
        case (os, arch) if os.contains("linux") && (arch == "aarch64" || arch == "arm64") =>
          "linux-aarch_64"
        case (os, _) if os.contains("linux")                                              =>
          "linux-x86_64"
        case (os, _) if os.contains("windows")                                            =>
          "windows-x86_64"
        case _                                                                            =>
          "linux-x86_64" // fallback
      }
    }

    def opendalJava: ModuleID = OpenDAL.java(classifier)
  }
}
