package io.simplematter.waterstream.vehiclesimulator.domain

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.get
import kotlin.random.Random

object CoordinateGenerator {

  private val coords: JsonArray

  init {
    val text = object {}.javaClass.getResource("/monuments.json").readText()
    val l: JsonObject = io.vertx.core.json.Json.decodeValue(text) as JsonObject
    coords = l.getJsonArray("monuments")
  }

  fun nextPoint(): Point {
    val coord = coords.get<JsonObject>(
      Random.nextInt(
        coords.size()
      )
    )
    val lat = coord.getString("clatitudine").toDouble()
    val lng = coord.getString("clongitudine").toDouble()
    return Point(lat, lng)
  }
}
