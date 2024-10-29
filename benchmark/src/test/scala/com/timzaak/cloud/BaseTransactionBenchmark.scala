package com.timzaak.cloud

import com.timzaak.DI
import com.timzaak.common.UserSessionGenerate
import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
 * This sample is based on our official tutorials:
 *
 *   - [[https://gatling.io/docs/gatling/tutorials/quickstart Gatling quickstart tutorial]]
 *   - [[https://gatling.io/docs/gatling/tutorials/advanced Gatling advanced tutorial]]
 */

//sbt 'Gatling/testOnly com.timzaak.cloud.BaseTransactionBenchmark' -Dusers=200 -Drepeat=100
class BaseTransactionBenchmark extends Simulation {
  DI.jedis.flushDB()
  new UserSessionGenerate(DI.jedis).generate(1 -> "t")
  DI.jedis.close()

  val users = Integer.getInteger("users", 100).toInt
  val repeat = Integer.getInteger("repeat", 500).toInt

  val httpProtocol = http
    .baseUrl("http://127.0.0.1:8080") // Here is the root for all relative URLs
    // .acceptHeader("text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8") // Here are the common headers
    .acceptEncodingHeader("gzip, deflate")
    .acceptLanguageHeader("en-US,en;q=0.5")
    .userAgentHeader(
      "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.8; rv:16.0) Gecko/20100101 Firefox/16.0"
    )

  val scn = scenario(s"base bench:${users}").repeat(repeat) {
    exec(
      http("ping")
        .get("/transaction")
        .header("Authorization", "t")
    )
  }
  setUp(
    scn.inject(atOnceUsers(users))
  ).protocols(httpProtocol)
}
