package com.timzaak.one

import com.timzaak.{ Helper, DI }
import com.timzaak.entity.PlaceOrderRequest
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

//sbt 'Gatling/testOnly com.timzaak.one.Order1OneProductBenchmark' -Duser=10 -Drepeat=500
//sbt 'Gatling/testOnly com.timzaak.one.Order1OneProductBenchmark'
class Order1OneProductBenchmark extends Simulation {
  val users = Integer.getInteger("users", 100).toInt
  val repeat = Integer.getInteger("repeat", 500).toInt
  val (_userSession, initScript, dataViewer) = Helper.prepare(users, repeat)
  val userSession = _userSession.toMap

  initScript.updateStock((1 to 10).map(_ -> users * repeat * 3).toMap) //
  val snapProductPriceMap = dataViewer.getProductSnapPrice(1 to 10 toList)

  private val jsonPrinter: Printer =
    Printer.noSpaces.copy(dropNullValues = true)

  val httpProtocol = http
    .baseUrl(DI.baseUrl) // Here is the root for all relative URLs
    // .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader(
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0"
    )

  val priceMap = dataViewer.getProductSnapPrice((1 to 10).toList)
  val dataMap = (1 to 10).map { snapId =>
    val req = PlaceOrderRequest.mockOneProduct(
      snapId,
      priceMap(snapId)
    )
    (snapId - 1) -> jsonPrinter.print(req.asJson)
  }.toMap

  val scn = scenario(s"place order bench:${users}").repeat(repeat) {

    exec(
      http("place order multiple products")
        .post("/order1")
        .header("Authorization", session => userSession(session.userId.toInt))
        .header("Content-Type", "application/json")
        .body(StringBody(session => dataMap((session.userId % 10).toInt)))
    )

  }
  setUp(
    scn.inject(atOnceUsers(users))
  ).protocols(httpProtocol)
}
