package com.timzaak.one

import com.timzaak.Helper
import com.timzaak.entity.PlaceOrderRequest
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.gatling.core.Predef._
import io.gatling.http.Predef._

//sbt 'Gatling/testOnly com.timzaak.one.Order1RetryBenchmark'
class Order1RetryBenchmark extends Simulation {
  val users = Integer.getInteger("users", 100).toInt
  val repeat = Integer.getInteger("repeat", 500).toInt
  val (_userSession, initScript, dataViewer) = Helper.prepare(users, repeat)
  val userSession = _userSession.toMap

  initScript.updateStock((1 to 10).map(_ -> users * repeat * 3).toMap) //
  val snapProductPriceMap = dataViewer.getProductSnapPrice(1 to 10 toList)

  private val jsonPrinter: Printer =
    Printer.noSpaces.copy(dropNullValues = true)

  val httpProtocol = http
    .baseUrl("http://127.0.0.1:8080") // Here is the root for all relative URLs
    // .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader(
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0"
    )

  val dataList = (1 to repeat).map { _ =>
    val req = PlaceOrderRequest.mockSimple(
      dataViewer.getProductSnapPrice((1 to 10).toList)
    )
    jsonPrinter.print(req.asJson)
  }

  val scn = scenario(s"place order bench:${users}").foreach(dataList, "body") {

    exec(
      http("place order 1")
        .post("/order1_retry")
        .header("Authorization", session => userSession(session.userId.toInt))
        .header("Content-Type", "application/json")
        .body(StringBody("#{body}"))
    )

  }
  setUp(
    scn.inject(atOnceUsers(users))
  ).protocols(httpProtocol)
}
