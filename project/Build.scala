import sbt._

object AkkaJeroMqBuild extends Build {
lazy val akka = Project(
    id = "akka-jeromq",
    base = file(".")
)
}

