package io.simplematter.waterstream.vehiclesimulator.domain

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class Vehicle(
    val plate: String,
    val current: Point,
    val waypoint: Point,
    val updateTimestamp: Long,
    val speed: Double,
    val visible: Boolean,
    val id: String
) {

    fun metersToWaypoint(): Double {
        return current.distance(waypoint) * 1000
    }

    fun milliSecondsToWaypoint(): Double = metersToWaypoint() / (speed / 3.6) * 1000

    fun updatePosition(elapsed: Long): Vehicle {
        val fractionDone = elapsed / milliSecondsToWaypoint()
        val newPosition = waypoint.multiplyBy(fractionDone).add(current.multiplyBy(1 - fractionDone))
        return copy(current = newPosition, updateTimestamp = Instant.now().toEpochMilli())
    }

    fun isArrived(): Boolean {
        return metersToWaypoint() < 10.0
    }

    fun setWaypoint(newWaypoint: Point): Vehicle {
        return copy(current = waypoint, waypoint = newWaypoint, updateTimestamp = Instant.now().toEpochMilli())
    }

    companion object {
        val log = LoggerFactory.getLogger(Vehicle::class.java)
    }
}

/**
 * Custom serialization which adds `distance` field
 */
fun Vehicle.toJson(): JsonObject {
    val json = JsonObject()
    json.put("plate", plate)
    json.put("current", current.toJson())
    json.put("waypoint", waypoint.toJson())
    json.put("speed", speed)
    json.put("visible", visible)
    json.put("distance", metersToWaypoint())
    json.put("updateTimestamp", updateTimestamp)
    json.put("id", id)
    return json
}


