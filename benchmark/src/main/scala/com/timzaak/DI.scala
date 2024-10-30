package com.timzaak

import com.typesafe.config.ConfigFactory
import redis.clients.jedis.JedisPooled
import scalikejdbc.{ AutoSession, DBSession }
import scalikejdbc.config.DBs

object DI {
  lazy val config = ConfigFactory.load()

  lazy val baseUrl = config.getString("apiServer")

  object jedis extends JedisPooled()

  DBs.setup()

  implicit val session: DBSession = AutoSession
}
