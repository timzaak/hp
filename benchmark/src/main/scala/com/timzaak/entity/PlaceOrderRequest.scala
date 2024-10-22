package com.timzaak.entity

import scala.math.BigDecimal.RoundingMode
import scala.util.Random

// 1 扣减
// 2 折扣
case class CouponRule(`type`: Int, num: BigDecimal)
object CouponRule {
  val FullReductionType = 1
  val DiscountReductionType = 2
}
case class Coupon(id: Long, rule: CouponRule)
case class Product(id: Int, snapId: Int, count: Int, price: BigDecimal)
case class PlaceOrderRequest(
  products: List[Product],
  totalAmount: BigDecimal = BigDecimal(0),
  coupon: Option[Coupon] = None,
  bonus: Option[Int] = None,
) {}

object PlaceOrderRequest {
  def mockSimple(productSnapPriceMap: Map[Int, BigDecimal], randomProductCount:Int = 3) = {
    val products = (1  to (Random.nextInt(randomProductCount) + 1)).map{_ =>
      val snapId = Random.nextInt(9)+1
      Product(snapId, snapId, Random.nextInt(3) + 1, productSnapPriceMap(snapId))
    }
    val bonus = if(Random.nextBoolean()) {Some(Random.nextInt(13) + 1)} else None
    val req = PlaceOrderRequest(
      products = products.toList,
      bonus= bonus,
    )
    calcTotalAmount(req)
  }
  def mockOneProduct(snapId:Int, price: BigDecimal) ={
    val products = List(Product(snapId, snapId, Random.nextInt(3) + 1, price))
    val bonus = if(Random.nextBoolean()) {Some(Random.nextInt(13) + 1)} else None
    val req = PlaceOrderRequest(
      products = products,
      bonus= bonus,
    )
    calcTotalAmount(req)
  }


  def calcTotalAmount(request: PlaceOrderRequest) = {
    import request._

    var s =
      products.map(p => (p.price * p.count).setScale(2, RoundingMode.DOWN)).sum
    coupon.foreach { c =>
      c.rule.`type` match {
        case CouponRule.DiscountReductionType => s *= c.rule.num
        case CouponRule.FullReductionType     => s -= c.rule.num
        case _                                =>
      }
    }
    bonus.foreach { b =>
      s = s - (BigDecimal.decimal(b).setScale(2, RoundingMode.DOWN) / 100)
    }
    request.copy(totalAmount = s)
  }
}
