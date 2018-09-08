name := "djamoe-betting-engine"
organization := "com.github.matek2305"
version := "1.0"

mainClass := Some("com.github.matek2305.djamoe.WebServer")

scalaVersion := "2.12.6"

libraryDependencies ++= {
  val akkaVersion = "2.5.2"
  val akkaHttpVersion = "10.1.3"
  Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    "com.jason-goodwin" %% "authentikat-jwt" % "0.4.5",
    "org.iq80.leveldb" % "leveldb" % "0.7",
    "io.circe" %% "circe-generic" % "0.8.0",
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
    "org.mindrot" % "jbcrypt" % "0.4",
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1" % Test,
    "org.scalatest" %% "scalatest" % "3.0.1" % Test,
    "com.tngtech.archunit" % "archunit" % "0.9.1" % Test
  )
}

resolvers += Resolver.jcenterRepo
