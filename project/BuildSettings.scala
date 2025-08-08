import sbt._
import sbt.Keys._
import xerial.sbt.Sonatype.sonatypeCentralHost
import xerial.sbt.Sonatype.SonatypeKeys._

object BuildSettings {

  // Project metadata
  object Meta {
    val organization = "io.github.etacassiopeia"
    val homePage     = "https://github.com/EtaCassiopeia/zio-opendal"
    val scmUrl       = "scm:git@github.com:EtaCassiopeia/zio-opendal.git"
    val issuesUrl    = "https://github.com/EtaCassiopeia/zio-opendal/issues"

    val developer = Developer(
      id = "etacassiopeia",
      name = "Mohsen Zainalpour",
      email = "zainalpour@gmail.com",
      url = url("https://github.com/EtaCassiopeia")
    )
  }

  // Scala versions and cross-compilation
  object ScalaVersions {
    val scala213 = Dependencies.Versions.scala213
    val scala33  = Dependencies.Versions.scala33
    val scala34  = Dependencies.Versions.scala34
    val scala35  = Dependencies.Versions.scala35

    val all     = Seq(scala213, scala33, scala34, scala35)
    val default = scala35
  }

  // Compiler options by Scala version
  object CompilerOptions {
    val scala213Options = Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-language:existentials",
      "-language:postfixOps",
      "-Xlint:_,-type-parameter-shadow",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
      "-Ywarn-unused:imports",
      "-Wconf:cat=unused-imports:error"
    )

    val scala3Options = Seq(
      "-deprecation",
      "-feature",
      "-unchecked",
      "-language:higherKinds",
      "-language:implicitConversions",
      "-Wunused:imports",
      "-Wvalue-discard"
    )

    def forVersion(scalaVersion: String): Seq[String] =
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, 13)) => scala213Options
        case Some((3, _))  => scala3Options
        case _             =>
          Seq(
            "-deprecation",
            "-feature",
            "-unchecked",
            "-language:higherKinds"
          )
      }
  }

  // Java options
  object JavaOptions {
    val compile = Seq(
      "-source",
      "11",
      "-target",
      "11",
      "-Xlint:all",
      "-parameters",
      "-g" // Enable debug information
    )

    val runtime = Seq(
      "--enable-native-access=ALL-UNNAMED",
      "-Djava.library.path=" + System.getProperty("java.library.path"),
      "-Djna.nosys=true"
    ) ++ (if (System.getProperty("os.arch") == "aarch64") Seq("-XX:UseSVE=0") else Seq.empty)
  }

  // Common settings for all projects
  val commonSettings: Seq[Setting[_]] = Seq(
    // Scala and Java configuration
    scalaVersion       := ScalaVersions.default,
    crossScalaVersions := ScalaVersions.all,
    scalacOptions ++= CompilerOptions.forVersion(scalaVersion.value),
    javacOptions ++= JavaOptions.compile,

    // Test configuration
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework"),
    Test / fork := true,
    Test / javaOptions ++= JavaOptions.runtime,
    Test / testOptions += Tests.Argument("-oD"), // Show test durations

    // Build optimization settings (memory settings moved to .jvmopts)
    // Parallel compilation
    Global / concurrentRestrictions += Tags.limitAll(8),

    // Documentation
    Compile / doc / scalacOptions ++= Seq(
      "-no-link-warnings" // Suppress warnings about missing links
    ),
    autoAPIMappings := true,

    // Dependency resolution
    ThisBuild / resolvers ++= Seq(
      Resolver.mavenCentral,
      Resolver.sonatypeCentralSnapshots
    )

    // Build performance (commented out until these keys are available)
    // ThisBuild / turbo := true,
    // ThisBuild / usePipelining := true
  )

  // Publishing settings
  val publishSettings: Seq[Setting[_]] = Seq(
    publishMavenStyle      := true,
    publishTo              := sonatypePublishToBundle.value,
    Test / publishArtifact := false,
    pomIncludeRepository   := { _ => false },

    // Version scheme
    ThisBuild / versionScheme := Some("early-semver"),

    // POM metadata
    homepage   := Some(url(Meta.homePage)),
    licenses   := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(Meta.developer),

    // SCM information
    scmInfo := Some(
      ScmInfo(
        url(Meta.homePage),
        Meta.scmUrl
      )
    ),

    // Enhanced POM
    pomExtra := {
      <url>{Meta.homePage}</url>
      <scm>
        <url>{Meta.scmUrl}</url>
        <connection>{Meta.scmUrl}</connection>
      </scm>
      <issueManagement>
        <system>github</system>
        <url>{Meta.issuesUrl}</url>
      </issueManagement>
    }
  )

  // No-publish settings for examples and test projects
  val noPublishSettings: Seq[Setting[_]] = Seq(
    publish / skip      := true,
    publishLocal / skip := true,
    publishArtifact     := false
  )

  // Global build settings
  val globalSettings: Seq[Setting[_]] = Seq(
    ThisBuild / organization           := Meta.organization,
    ThisBuild / sonatypeCredentialHost := sonatypeCentralHost,
    ThisBuild / sonatypeRepository     := "https://s01.oss.sonatype.org/service/local",

    // Signing configuration (handled by sbt-pgp plugin)
    // pgpPassphrase will be set by the plugin using environment variables

    // Credentials
    ThisBuild / credentials ++= {
      val sonatypeUser = sys.env.get("SONATYPE_USERNAME")
      val sonatypePass = sys.env.get("SONATYPE_PASSWORD")

      (sonatypeUser, sonatypePass) match {
        case (Some(user), Some(pass)) =>
          Seq(
            Credentials(
              "Sonatype Nexus Repository Manager",
              "s01.oss.sonatype.org",
              user,
              pass
            )
          )
        case _                        => Seq.empty
      }
    }
  )
}
