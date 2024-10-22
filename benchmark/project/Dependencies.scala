import sbt.*

object Dependencies {

  lazy val logLib = Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "ch.qos.logback" % "logback-classic" % "1.5.9",
  )

  lazy val persistence = Seq(
    "org.scalikejdbc" %% "scalikejdbc-orm" % "4.3.2",
    "org.postgresql" % "postgresql" % "42.7.3",
    "org.flywaydb" % "flyway-core" % "10.20.0",
    "org.flywaydb" % "flyway-database-postgresql" % "10.20.0",
  )

  lazy val enumExtraLib = {
    val version = "1.7.4"
    Seq(
      "com.beachape" %% "enumeratum" % version,
      // "com.beachape" %% "enumeratum-quill" % version,
      "com.beachape" %% "enumeratum-circe" % version,
    )
  }
}
