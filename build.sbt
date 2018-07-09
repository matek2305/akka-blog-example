import CompilerFlags._

name := "akka-blog-microservice"
organization := "com.spr"
version := "1.0"

mainClass := Some("com.spr.blog.Main")

scalaVersion := "2.12.6"

scalacOptions ++= compilerFlags
scalacOptions in(Compile, console) ~= filterExcludedReplOptions

lazy val akkaVersion = "2.5.2"
lazy val akkaHttpVersion = "10.1.3"
lazy val circeVersion = "0.8.0"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.3",
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1",
  "io.circe" %% "circe-generic" % circeVersion,
  "org.iq80.leveldb" % "leveldb" % "0.7",
  "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",
  "org.scalatest" %% "scalatest" % "3.0.1" % Test
)

resolvers += Resolver.jcenterRepo
