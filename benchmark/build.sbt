val scala2Version = "2.13.15"
enablePlugins(GatlingPlugin)

val gatlingVersion = "3.12.0"

import Dependencies.*

scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:postfixOps"
)

lazy val root = project
  .in(file("."))
  .settings(
    name := "benchmark",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala2Version,
    libraryDependencies ++= (Seq(
      "io.circe" %% "circe-config" % "0.10.1",
      "io.circe" %% "circe-generic"% "0.14.9",
      "redis.clients" % "jedis" % "5.1.5",
      "org.scalameta" %% "munit" % "1.0.0" % Test,
      "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % "test,it",
      "io.gatling" % "gatling-test-framework" % gatlingVersion % "test,it"
    ) ++ logLib ++ persistence)
  )
