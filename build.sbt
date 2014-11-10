name := "ticket-scanner-backend"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  json,
  anorm,
  cache,
  ws
)

libraryDependencies ++= Seq(
  "com.github.sstone" %% "amqp-client" % "1.4",
  "com.typesafe.akka" %% "akka-actor" % "2.3.4",
  "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23",
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23"
)
