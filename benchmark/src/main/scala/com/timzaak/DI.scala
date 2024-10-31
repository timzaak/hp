package com.timzaak

import com.typesafe.config.ConfigFactory
import redis.clients.jedis.JedisPooled
import scalikejdbc.{ AutoSession, DBSession }
import scalikejdbc.config.DBs

object DI {
  lazy val config = ConfigFactory.load()

  lazy val baseUrl = config.getString("apiServer")
  lazy  val redisHost = config.getString("redis.host")

  object jedis extends JedisPooled(redisHost)

  DBs.setup()

  implicit val session: DBSession = AutoSession
}
