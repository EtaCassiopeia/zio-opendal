// Build configuration imports
import Dependencies._
import BuildSettings._

// ==============================
// Project Version & Build Setup
// ==============================

ThisBuild / version            := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion       := BuildSettings.ScalaVersions.default
ThisBuild / crossScalaVersions := BuildSettings.ScalaVersions.all

// Apply global settings
BuildSettings.globalSettings

// ==============================
// Main Library Project
// ==============================

/**
 * Main library project - zio-opendal
 *
 * This is the core zio-opendal library that provides ZIO-friendly bindings for
 * Apache OpenDAL across multiple storage backends.
 */
lazy val root = (project in file("."))
  .configs(sbt.IntegrationTest)
  .settings(BuildSettings.commonSettings)
  .settings(BuildSettings.publishSettings)
  .settings(Defaults.itSettings)
  .settings(
    name        := "zio-opendal",
    description := "ZIO-friendly Scala wrapper for Apache OpenDAL",

    // Core dependencies
    libraryDependencies ++= Dependencies.Groups.core,
    libraryDependencies += Dependencies.Platform.opendalJava,
    libraryDependencies ++= Dependencies.Groups.testing,

    // Integration test dependencies
    libraryDependencies ++= Seq(
      Dependencies.IntegrationTest.zioTest,
      Dependencies.IntegrationTest.zioTestSbt,
      Dependencies.IntegrationTest.zioTestMagnolia,
      Dependencies.IntegrationTest.testcontainers,
      Dependencies.IntegrationTest.localstack,
      Dependencies.IntegrationTest.awsS3,
      Dependencies.IntegrationTest.awsCore,
      Dependencies.IntegrationTest.awsAuth,
      Dependencies.IntegrationTest.logbackClassic
    ),

    // Integration test settings
    sbt.IntegrationTest / fork := true,
    sbt.IntegrationTest / envVars := Map(
      "ENABLE_INTEGRATION_TESTS" -> "true"
    ),
    sbt.IntegrationTest / javaOptions ++= BuildSettings.JavaOptions.runtime,
    sbt.IntegrationTest / testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework")),

    // Additional library settings
    exportJars := true,

    // MiMa settings for binary compatibility
    mimaPreviousArtifacts := Set.empty, // No previous versions yet
    mimaCheckDirection    := "backward",

    // Scaladoc settings
    Compile / doc / scalacOptions ++= Seq(
      "-doc-title",
      "ZIO OpenDAL",
      "-doc-version",
      version.value,
      "-sourcepath",
      baseDirectory.value.getAbsolutePath
    )
  )

// ==============================
// Sub-projects
// ==============================

/**
 * Examples and documentation project
 *
 * Contains usage examples and documentation code. This project is not published
 * to Maven Central.
 */
lazy val examples = (project in file("examples"))
  .dependsOn(root)
  .settings(BuildSettings.commonSettings)
  .settings(BuildSettings.noPublishSettings)
  .settings(
    name        := "zio-opendal-examples",
    description := "Examples and documentation for ZIO OpenDAL",

    // Example dependencies
    libraryDependencies ++= Dependencies.Groups.examples,

    // Run settings
    Compile / run / fork := true,
    Compile / run / javaOptions ++= BuildSettings.JavaOptions.runtime
  )

// ==============================
// Build Aliases and Commands
// ==============================

// Formatting commands
addCommandAlias("fmt", "scalafmtAll; scalafmtSbt")
addCommandAlias("check", "scalafmtCheckAll; scalafmtSbtCheck")

// Testing commands
addCommandAlias("testAll", "test; examples/compile")
addCommandAlias("testAllWithIt", "test; it:test; examples/compile")
addCommandAlias("compileAll", "compile; examples/compile")
addCommandAlias("itTest", "it:test")

// Publishing commands
addCommandAlias("publishAll", "publishSigned")

// CI-friendly task definitions
addCommandAlias("ci-test", "testAll")
addCommandAlias("ci-test-full", "testAllWithIt")
addCommandAlias("ci-docs", "doc")
addCommandAlias("ci-publish", "publishAll")
addCommandAlias("ci-check", "check; mimaReportBinaryIssues")
