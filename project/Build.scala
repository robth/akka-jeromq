import sbt._
import sbt.Keys._

object AkkaJeroMqBuild extends Build {

  val requestedScalaVersion = System.getProperty("akka.jeromq.build.scalaVersion", "2.11.5")

  lazy val buildSettings = Seq(
    organization := "nl.robtenhove.akka.jeromq",
    version      := "0.1.0-SNAPSHOT",
    scalaVersion := requestedScalaVersion
  )

  lazy val akkaJeroMq = Project(
    id = "akka-jeromq",
    base = file("."),
    settings = buildSettings ++ Seq(libraryDependencies ++= Dependencies.akkaJeroMq)
  )
}

object Dependencies {

  object Versions {
    val akkaVersion      = System.getProperty("akka.jeromq.build.akkaVersion", "2.3.9")
    val scalaTestVersion = System.getProperty("akka.jeromq.build.scalaTestVersion", "2.2.4")
  }

  object Compile {
    import Versions._

    val akkaActor = "com.typesafe.akka"   %% "akka-actor"    % akkaVersion // ApacheV2
    val protobuf  = "com.google.protobuf" %  "protobuf-java" % "2.5.0" // New BSD
    val jeroMq    = "org.zeromq"          %  "jeromq"        % "0.3.4" // LGPL with special exception

    object Test {
      val scalatest   = "org.scalatest"     %% "scalatest"    % scalaTestVersion % "test" // ApacheV2
      val akkaTestKit = "com.typesafe.akka" %% "akka-testkit" % akkaVersion      % "test" // ApacheV2
    }

  }

  import Compile._

  val akkaJeroMq = Seq(akkaActor, protobuf, jeroMq, Test.scalatest, Test.akkaTestKit)

}
