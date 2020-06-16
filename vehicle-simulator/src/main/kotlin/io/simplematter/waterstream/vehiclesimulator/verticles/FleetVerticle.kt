package io.simplematter.waterstream.vehiclesimulator.verticles

import io.netty.handler.codec.mqtt.MqttQoS
import io.simplematter.waterstream.vehiclesimulator.clientprotocol.ClientMessage
import io.simplematter.waterstream.vehiclesimulator.config.VehicleSimulatorConfig
import io.simplematter.waterstream.vehiclesimulator.domain.*
import io.simplematter.waterstream.vehiclesimulator.events.FleetEvent
import io.simplematter.waterstream.vehiclesimulator.monitoring.VehicleSimCounters
import io.simplematter.waterstream.vehiclesimulator.tools.JsonUtils
import io.simplematter.waterstream.vehiclesimulator.tools.MqttConnect
import io.vertx.core.DeploymentOptions
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import kotlinx.coroutines.launch
import kotlin.random.Random
import org.slf4j.LoggerFactory

class FleetVerticle : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(FleetVerticle::class.java.name)!!
    }

    private val serviceConfig by lazy { JsonUtils.fromVertxJson(config, VehicleSimulatorConfig::class.java) }

    private lateinit var vehiclesRegister: VehiclesRegister

    private val deployTick = "fleetVerticle.deployTick"

    private var visibleVehiclesCount: Int = 0

    override suspend fun start() {

        vehiclesRegister = VehiclesRegister(serviceConfig.vehicles.visibleNumber)

        vertx.eventBus().consumer(FleetEvent.VehicleCreated, Handler<Message<JsonObject>> { event ->
            val v = extractVehicle(event)
            vehiclesRegister.add(vehicle = v, isNew = true)
        })

        vertx.eventBus().consumer(FleetEvent.VehicleUpdated, Handler<Message<JsonObject>> { event ->
            val v = extractVehicle(event)
            vehiclesRegister.add(vehicle = v, isNew = false)
        })

        vertx.eventBus().consumer(FleetEvent.VehicleRemoved, Handler<Message<JsonObject>> { event ->
            val v = extractVehicle(event)
            if (v.visible)
                visibleVehiclesCount = visibleVehiclesCount - 1
            vehiclesRegister.remove(vehicle = v)
            log.info("Start a new vehicle")
            deployVehicleVerticles(serviceConfig, 1)
        })

        vertx.eventBus().consumer(ClientMessage.In.Connected, Handler<Message<String>> { _ ->
            val list = vehiclesRegister.visibleVehicles()
            vertx.eventBus().publish(ClientMessage.Out.FleetComposition, JsonArray(list.map { it.toJson() }))
        })

        vertx.setPeriodic(1000) {
            vertx.eventBus().publish(deployTick, serviceConfig.vehicles.totalNumber - vehiclesRegister.size())
        }

        vertx.eventBus().consumer(deployTick, Handler<Message<Int>> { _ ->
            val missingVehicles = serviceConfig.vehicles.totalNumber - vehiclesRegister.size()
            if (missingVehicles > 0) {
                val nVehicles = Random.nextInt(Math.min(100, missingVehicles))
                log.debug("Deploying {} vehicles or {} missing", nVehicles, missingVehicles)
                deployVehicleVerticles(serviceConfig, nVehicles)
            }
        })

    }

    private fun deployVehicleVerticles(config: VehicleSimulatorConfig, number: Int) {
        // specifying number of instances in DeploymentOptions would make the verticles share the same deploymentId, thus making individual instance undeploy impossible
        for (i in 0..number) {
            val visible = visibleVehiclesCount < config.vehicles.visibleNumber
            if (visible)
                visibleVehiclesCount = visibleVehiclesCount + 1
            val vehicleVerticleConfig = VehicleVerticle.Companion.VehicleConfig(
                id = Random.nextInt().toString(),
                plate = PlateIssuer.default.issuePlate(),
                visible = visible,
                mqttConfig = config.mqtt,
                routingUrl = config.routing.url
            )

            val deployOptions = DeploymentOptions()
            deployOptions.setConfig(JsonObject.mapFrom(vehicleVerticleConfig))
            deployOptions.isWorker = true
            deployOptions.workerPoolSize = 40

            vertx.deployVerticle(VehicleVerticle::class.qualifiedName, deployOptions)
        }
    }

    private fun extractVehicle(event: Message<JsonObject>?): Vehicle {
        val value = event!!.body()
//        return JsonUtils.fromVertxJson(value, Vehicle::class.java)

        val plate = value.getString("plate")
        val current = value.getJsonObject("current")
        val currentPoint = Point(current.getDouble("lat"), current.getDouble("lng"))
        val waypoint = value.getJsonObject("waypoint")
        val waypointPoint = Point(waypoint.getDouble("lat"), waypoint.getDouble("lng"))
        val speed = value.getDouble("speed")
        val updateTimestamp = value.getLong("updateTimestamp")
        val visible = value.getBoolean("visible")
        val id = value.getString("id")
        return Vehicle(plate, currentPoint, waypointPoint, updateTimestamp, speed, visible, id)
    }
}


