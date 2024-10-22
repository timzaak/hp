package com.timzaak

import com.timzaak.common.{DataInit, DataViewer}
import com.timzaak.entity.CouponRule
import scalikejdbc.config.DBs

object Helper {
  //
  def prepare(userCount: Int, repeat: Int) = {
    DI.jedis.flushDB()
    val init = new DataInit(DI.jedis, DI.config)(DI.session)
    val dataViewer = new DataViewer()(DI.session)


    val userIdIter = (1 to userCount).toList
    val userCoupon = userIdIter.flatMap { userId =>
      List(
        (userId, CouponRule(1, BigDecimal(3)), repeat / 2),
        (userId, CouponRule(2, BigDecimal(0.7)), repeat / 2)
      )
    }
    (init.initData(usersCount = userCount, userCoupon = userCoupon), init, dataViewer)
  }

  def createPlaceOrderRequest() = {


  }


  def closeAllResource = {
    DI.jedis.close()
    DBs.close()
  }



}
