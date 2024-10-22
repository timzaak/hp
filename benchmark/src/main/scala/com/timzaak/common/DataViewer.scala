package com.timzaak.common

import com.timzaak.entity.CouponRule
import scalikejdbc._
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._

class DataViewer(implicit session: DBSession) {

  def getProductSnapPrice(snapId: List[Int]) =
    sql"select product_id, data->'price' as price from product_snapshot where id in (${snapId})"
      .map { v =>
        v.get[Int]("product_id") -> v.get[BigDecimal]("price")
      }
      .list
      .apply()
      .toMap

  def getAllUserCoupon() = {
    sql"select id, user_id, rule from user_coupon"
      .map { v =>
        val rule = decode[CouponRule](v.get[String]("rule")).toOption.get
        (v.get[Int]("user_id"), v.get[Long]("id"), rule)
      }
      .list
      .apply()
      .groupMap(_._1) { case (_, id, rule) => (id, rule) }
  }

}
