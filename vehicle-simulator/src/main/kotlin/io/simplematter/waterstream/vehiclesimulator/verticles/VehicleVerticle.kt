package io.simplematter.waterstream.vehiclesimulator.verticles

import io.netty.handler.codec.mqtt.MqttQoS
import io.simplematter.waterstream.vehiclesimulator.config.MqttConfig
import io.simplematter.waterstream.vehiclesimulator.domain.*
import io.simplematter.waterstream.vehiclesimulator.events.FleetEvent
import io.simplematter.waterstream.vehiclesimulator.monitoring.VehicleSimCounters
import io.simplematter.waterstream.vehiclesimulator.tools.JsonUtils
import io.simplematter.waterstream.vehiclesimulator.tools.MqttConnect
import io.simplematter.waterstream.vehiclesimulator.tools.RouteGenerator
import io.vertx.core.Handler
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.Message
import io.vertx.kotlin.coroutines.CoroutineVerticle
import java.time.Instant
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory

class VehicleVerticle : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(VehicleVerticle::class.java.name)

        data class VehicleConfig(
            val id: String,
            val plate: String,
            val visible: Boolean,
            val mqttConfig: MqttConfig,
            val routingUrl: String
        )
    }

    private val vehicleConfig: VehicleConfig by lazy {
        JsonUtils.fromVertxJson(config, VehicleConfig::class.java)
    }

    private lateinit var vehicle: Vehicle
    private val routingUrl by lazy { vehicleConfig.routingUrl }

    private var timer: Long = 0
    private val refreshTimeMillis: Long = 500
    private var waitCycles = 0

    private val routeGenerator by lazy { RouteGenerator(routingUrl) }
    private var currentRoute = listOf<Point>()

    private var assignedRoute = listOf<Point>()


    private val id by lazy { vehicleConfig.id }
    private val plate by lazy { vehicleConfig.plate }
    private val tickEvent by lazy { "Vehicle.$plate.Ticked" }

    private val topicPrefix by lazy { vehicleConfig.mqttConfig.topicPrefix }

    private var wayBack = false

    private val mqttConnect by lazy {
        MqttConnect(
            vertx,
            vehicleConfig.mqttConfig.host,
            vehicleConfig.mqttConfig.port,
            "$plate $deploymentID",
            vehicleConfig.mqttConfig.username,
            vehicleConfig.mqttConfig.password
        )
    }

    @Volatile
    private var undeployRequested = false

    private val vehicleMutex = Mutex()

    override suspend fun start() {
        log.debug("Starting {} {} vehicle initialization", plate, id)

        try {
            assignedRoute = routeGenerator.generateRoute()
            currentRoute = assignedRoute.drop(1)

            vehicle = Vehicle(
                plate,
                current = assignedRoute.first(),
                waypoint = currentRoute.first(),
                updateTimestamp = Instant.now().toEpochMilli(),
                speed = Random.nextDouble(60.0, 90.0),
                visible = vehicleConfig.visible,
                id = id
            )
            currentRoute = assignedRoute.drop(1)

            vertx.eventBus().publish(FleetEvent.VehicleCreated, vehicle.toJson())
            VehicleSimCounters.vehiclesCreated.inc()
            VehicleSimCounters.vehiclesCurrent.inc()

            timer = vertx.setPeriodic(refreshTimeMillis) {
                vertx.eventBus().publish(tickEvent, plate)
            }

            vertx.eventBus().consumer(tickEvent, Handler<Message<String>> { event ->
                GlobalScope.launch(Dispatchers.IO) { handle(event) }
            })

            mqttConnect.getConnectedMqttClient()
        } catch (e: Exception) {
            log.info("${plate} will be removed because of initialization error")
            VehicleSimCounters.vehicleInitErrors.inc()
            undeployVerticle()
        }

        log.debug("Finished $plate $deploymentID vehicle initialization")
    }

    private suspend fun handle(event: Message<String>) {
        vehicleMutex.withLock {
            if (waitCycles == 0) {
                if (!vehicle.isArrived()) {
                    val now = Instant.now().toEpochMilli()
                    val elapsed = now - vehicle.updateTimestamp

                    vehicle = vehicle.updatePosition(elapsed)
                    vertx.eventBus().publish(FleetEvent.VehicleUpdated, vehicle.toJson())
                    VehicleSimCounters.vehicleUpdates.inc()
                    publishVehicleUpdateToMqtt()
                } else {
                    VehicleSimCounters.vehicleArriveEvents.inc()
                    if (currentRoute.isEmpty()) {
                        // this pauses the vehicle for a while before starting a new route
                        waitCycles = Random.nextInt(100)
                        if (wayBack) {
                            currentRoute = assignedRoute.asReversed()
                        } else {
                            currentRoute = assignedRoute
                        }
                        wayBack = !wayBack
                    }

                    val newWaypoint = currentRoute.first()
                    val distanceKm = vehicle.current.distance(newWaypoint)
                    if (distanceKm > 50)
                        Vehicle.log.warn(
                            "Jump too far: {} from {} to {}, by {} km. \nCurrent route: {}\nAssigned route: {}",
                            plate,
                            vehicle.current,
                            newWaypoint,
                            distanceKm,
                            currentRoute,
                            assignedRoute
                        )
                    vehicle = vehicle.setWaypoint(newWaypoint)
                    currentRoute = currentRoute.drop(1)
                    vertx.eventBus().publish(FleetEvent.VehicleUpdated, vehicle.toJson())
                    VehicleSimCounters.vehicleUpdates.inc()
                }
            } else {
                waitCycles--
            }
        }
    }

    override suspend fun stop() {
        super.stop()
        PlateIssuer.default.returnPlate(vehicle.plate)
        VehicleSimCounters.vehiclesRemoved.inc()
        VehicleSimCounters.vehiclesCurrent.dec()
        mqttConnect.disconnectMqtt()
        if (!undeployRequested) {
            VehicleSimCounters.vehiclesUnsolicitedRemovals.inc()
            log.warn("Vehicle $plate $deploymentID stopped without undeployVerticle")
        }
    }

    private suspend fun undeployVerticle() {
        VehicleSimCounters.vehiclesUndeployRequested.inc()
        vertx.undeploy(this.deploymentID)
        vertx.eventBus().publish(FleetEvent.VehicleRemoved, vehicle.toJson())
        publishVehicleRemoveToMqtt()
        undeployRequested = true
    }

    private suspend fun publishVehicleUpdateToMqtt() {
        val body = Buffer.buffer(vehicle.toJson().toString())
        mqttConnect.getConnectedMqttClient().publish(
            "${topicPrefix}vehicle_updates/${vehicle.plate}",
            body,
            MqttQoS.AT_MOST_ONCE,
            false,
            false
        )
        VehicleSimCounters.mqttMessagesSent.inc()

        //TBD consider using ksqlDB to extract visible vehicles
        if(vehicle.visible) {
            mqttConnect.getConnectedMqttClient().publish(
                "${topicPrefix}visible_vehicle_updates/${vehicle.plate}",
                body,
                MqttQoS.AT_MOST_ONCE,
                false,
                false
            )
            VehicleSimCounters.mqttMessagesSent.inc()
        }
    }

    private suspend fun publishVehicleRemoveToMqtt() {
        mqttConnect.getConnectedMqttClient().publish(
            "${topicPrefix}vehicle_updates/${vehicle.plate}",
            Buffer.buffer(),
            MqttQoS.AT_MOST_ONCE,
            false,
            false
        )
        VehicleSimCounters.mqttMessagesSent.inc()

        if(vehicle.visible) {
            mqttConnect.getConnectedMqttClient().publish(
                "${topicPrefix}visible_vehicle_updates/${vehicle.plate}",
                Buffer.buffer(),
                MqttQoS.AT_MOST_ONCE,
                false,
                false
            )
            VehicleSimCounters.mqttMessagesSent.inc()
        }
    }
}
