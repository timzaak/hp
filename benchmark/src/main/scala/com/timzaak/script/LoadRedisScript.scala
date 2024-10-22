package com.timzaak.script

import com.timzaak.DI

import java.nio.charset.StandardCharsets
import java.nio.file.{ Files, Path }

object LoadRedisScript {

  def main(args: Array[String]): Unit = {
    loadFunction()
    DI.jedis.functionList().forEach { v =>
      println(s"lib name:${v.getLibraryName}")
      v.getFunctions.forEach { f =>
        println(s"function name: ${f.get("name")}")
      }
    }
  }
  def loadFunction() {
    DI.jedis.functionLoadReplace(
      Files.readString(
        Path.of("../backend/src/main/lua/stock.lua"),
        StandardCharsets.UTF_8
      )
    )
  }

}
