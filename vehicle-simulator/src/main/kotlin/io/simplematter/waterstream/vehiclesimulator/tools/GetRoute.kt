package io.simplematter.waterstream.vehiclesimulator.tools

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.Request
import com.github.kittinunf.fuel.core.Response
import com.github.kittinunf.result.Result
import com.google.gson.JsonParser
import io.simplematter.waterstream.vehiclesimulator.domain.CoordinateGenerator
import io.simplematter.waterstream.vehiclesimulator.domain.Point
import io.simplematter.waterstream.vehiclesimulator.domain.toQueryParam

object GetRoute {

  @JvmStatic
  fun main(args: Array<String>) {

    val start = CoordinateGenerator.nextPoint()
    val end = CoordinateGenerator.nextPoint()

    // http://46.4.29.167:8080/ors/routes?profile=driving-hgv&coordinates=8.676581,49.418204|8.692803,49.409465

    val parameters = listOf(
      "profile" to "driving-hgv",
      "coordinates" to "${start.toQueryParam()}|${end.toQueryParam()}",
      "geometry_format" to "geojson"
    )

    println(parameters)

    // http://46.4.29.167:8080/ors/routes?profile=driving-hgv&coordinates=11.119743,46.0730683999999|11.2535002,43.7795184

    val (_: Request, _: Response, c: Result<String, FuelError>) = Fuel.get(
      "http://46.4.29.167:8080/ors/directions",
      parameters
    ).responseString(Charsets.UTF_8)

    println(c.component1())

    val points: List<Point> = c.fold(
      success = { it: String ->
        val routes = JsonParser.parseString(it).asJsonObject.getAsJsonArray("routes")

        val coordinates = routes[0].asJsonObject.getAsJsonObject("geometry").getAsJsonArray("coordinates")
        coordinates.map { c ->
          Point(c.asJsonArray[1].asDouble, c.asJsonArray[0].asDouble)
        }
      },

      failure = {
        listOf()
      }
    )

    points.forEach { it -> println(it) }
  }
}
