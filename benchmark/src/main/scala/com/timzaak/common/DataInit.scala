package com.timzaak.common

import com.timzaak.DI
import com.timzaak.entity.CouponRule
import com.typesafe.config.Config
import redis.clients.jedis.UnifiedJedis
import scalikejdbc._
import scalikejdbc.config.DBs
import io.circe.generic.auto._
import io.circe.config.syntax._
import org.flywaydb.core.Flyway

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }
import java.time.OffsetDateTime

case class DataInitConfig(
  sqlFilePath: String
)

class DataInit(jedis: UnifiedJedis, config: Config)(implicit
  session: DBSession
) {

  def productCacheKey(id: Int) = s"p_$id"

  def recreateTable = {
    val dbConf = config.getConfig("db.default")
    val url = dbConf.getString("url")
    val user = dbConf.getString("user")
    val password = dbConf.getString("password")

    val _conf = config.as[DataInitConfig]("dataInit").toOption.get

    val flyway = Flyway
      .configure()
      .dataSource(url, user, password)
      .baselineOnMigrate(true)
      .initSql(
        Files.readString(Path.of(_conf.sqlFilePath), StandardCharsets.UTF_8)
      )
      .cleanDisabled(false)
      .load()
    flyway.clean()
    flyway.migrate()
  }

  def initData(
    usersCount: Int,
    userCoupon: Seq[(Int, CouponRule, Int)] = Seq.empty,
    productStocks: Map[Int, Int] = Map.empty
  ) = {
    recreateTable

    updateStock(productStocks)

    updateUserCoupon(userCoupon)

    updateUserBonus(usersCount)

    new UserSessionGenerate(jedis).generateUsers(usersCount)

  }

  def updateUserBonus(usersCount: Int): Unit = {
    sql"insert into bonus(user_id, amount) values (?,?)"
      .batch(
        ((1 to usersCount).map(index => Seq(index, 1000000)): Seq[Seq[Any]]): _*
      )
      .apply()
  }
  def updateStock(productStocks: Map[Int, Int]): Unit = {
    for ((id, stock) <- productStocks) {
      sql"update product set stock=${stock}, updated_at=${OffsetDateTime
          .now()} where id=${id}".execute
        .apply()
      jedis.set(productCacheKey(id), stock.toString)
    }
  }

  def updateUserCoupon(userCoupon: Seq[(Int, CouponRule, Int)]): Unit = {
    sql"insert into user_coupon(user_id, rule) values (?, jsonb_build_object('type', ?, 'num', ?))"
      .batch(
        (userCoupon.flatMap { case (userId, rule, count) =>
          (1 to count).map(_ => Seq(userId, rule.`type`, rule.num))
        }: Seq[Seq[Any]]): _*
      )
      .apply()
  }

}
