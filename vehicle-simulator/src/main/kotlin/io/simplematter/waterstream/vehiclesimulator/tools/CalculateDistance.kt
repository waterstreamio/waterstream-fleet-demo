package io.simplematter.waterstream.vehiclesimulator.tools

import io.simplematter.waterstream.vehiclesimulator.domain.Point
import io.simplematter.waterstream.vehiclesimulator.domain.distance

object CalculateDistance {
  @JvmStatic
  fun main(args: Array<String>) {
    val a = Point(45.9660701, 12.6096274)
    val b = Point(45.9662799, 12.6118184)
    print("DIST " + a.distance(b))
  }
}
