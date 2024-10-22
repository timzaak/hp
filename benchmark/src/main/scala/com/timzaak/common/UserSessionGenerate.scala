package com.timzaak.common

import redis.clients.jedis.UnifiedJedis

import java.util.UUID

class UserSessionGenerate(jedis: UnifiedJedis) {

  val userPrefix = "U_"

  def generate(values: (Int, String)*): Unit = {
    values.foreach { case (userId, session) =>
      jedis.set(s"$userPrefix$session", userId.toString)
    }
  }

  def generateUsers(num: Int) = {
    val ids = (1 to num).map { userId =>
      if (userId == 1) {
        userId -> "t"
      } else {
        userId -> UUID.randomUUID().toString
      }
    }.toList
    generate(ids: _*)
    ids
  }
}
