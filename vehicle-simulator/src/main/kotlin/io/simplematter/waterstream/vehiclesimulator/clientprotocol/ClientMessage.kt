package io.simplematter.waterstream.vehiclesimulator.clientprotocol

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

object ClientMessage {

  object In {
    val Connected = "ClientMessage.Connected"
  }

  object Out {
    val FleetComposition = "ClientMessage.FleetComposition"
  }
}

fun toVehicleUpdatedMessage(vehicle: JsonObject): JsonObject {
  val message = JsonObject()
  message.put("action", "update")
  message.put("value", vehicle)
  return message
}

fun toVehicleRemoveMessage(vehicle: JsonObject): JsonObject {
  val message = JsonObject()
  message.put("action", "remove")
  message.put("value", vehicle)
  return message
}

fun toFleetCompositionMessage(list: JsonArray): JsonObject {
  val message = JsonObject()
  message.put("action", "setup")
  message.put("value", list)
  return message
}
