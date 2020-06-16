package io.simplematter.waterstream.vehiclesimulator.domain

import io.vertx.core.json.JsonObject

data class Point(val lat: Double, val lng: Double) {

  fun multiplyBy(factor: Double): Point {
    return Point(lat * factor, lng * factor)
  }

  fun add(other: Point): Point {
    return Point(lat + other.lat, lng + other.lng)
  }
}

fun Point.toJson(): JsonObject {
  val json = JsonObject()
  json.put("lat", this.lat)
  json.put("lng", this.lng)
  return json
}

fun Point.toQueryParam(): String = "$lng,$lat"

// this is in KM
fun Point.distance(other: Point): Double {

  fun haversin(value: Double): Double {
    return Math.pow(Math.sin(value / 2), 2.0)
  }

  val EARTH_RADIUS = 6371 // Approx Earth radius in KM

  var startLat = this.lat
  var endLat = other.lat

  val dLat = Math.toRadians(endLat - startLat)
  val dLong = Math.toRadians(other.lng - this.lng)

  startLat = Math.toRadians(startLat)
  endLat = Math.toRadians(endLat)

  val a = haversin(dLat) + Math.cos(startLat) * Math.cos(endLat) * haversin(dLong)
  val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

  return EARTH_RADIUS * c // <-- d
}
