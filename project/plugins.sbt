// Code quality and formatting
addSbtPlugin("org.scalameta"     % "sbt-scalafmt"     % "2.5.4")
addSbtPlugin("ch.epfl.scala"     % "sbt-scalafix"     % "0.13.0")

// Publishing and release
addSbtPlugin("com.github.sbt"    % "sbt-ci-release"   % "1.9.3")
addSbtPlugin("org.xerial.sbt"    % "sbt-sonatype"     % "3.12.2")
addSbtPlugin("com.github.sbt"    % "sbt-pgp"          % "2.3.1")

// Binary compatibility and dependency management
addSbtPlugin("com.typesafe"      % "sbt-mima-plugin"  % "1.1.4")
addSbtPlugin("com.timushev.sbt"  % "sbt-updates"      % "0.6.4")
addSbtPlugin("net.virtual-void"  % "sbt-dependency-graph" % "0.10.0-RC1")
