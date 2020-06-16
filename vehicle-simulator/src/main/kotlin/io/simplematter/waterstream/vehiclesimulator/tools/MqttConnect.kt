package io.simplematter.waterstream.vehiclesimulator.tools

import io.simplematter.waterstream.vehiclesimulator.monitoring.VehicleSimCounters
import io.vertx.core.Vertx
import io.vertx.mqtt.MqttClient
import io.vertx.mqtt.MqttClientOptions
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


class MqttConnect(
    private val vertx: Vertx,
    private val mqttUrl: String,
    private val mqttPort: Int,
    private val clientId: String,
    private val username: String?,
    private val password: String?
) {

    private var client: MqttClient? = null
    private val clientInitMutex = Mutex()

    suspend fun getConnectedMqttClient(): MqttClient {
        return clientInitMutex.withLock {
            val existingClient = client
            if (existingClient != null && existingClient.isConnected) {
                existingClient
            } else {
                val options = MqttClientOptions().setClientId(clientId)
                username?.let { options.setUsername(it) }
                password?.let { options.setPassword(it) }
                val c = MqttClient.create(vertx, options)
                VehicleSimCounters.mqttConnectAttempts.inc()
                suspendCoroutine<Unit> { continuation ->
                    c.connect(mqttPort, mqttUrl) { result ->
                        if (result.succeeded()) {
                            log.debug("MQTT client {} successfully connected", clientId)
                            VehicleSimCounters.mqttConnectSuccess.inc()
                            VehicleSimCounters.mqttCurrentConections.inc()
                            continuation.resume(Unit)
                        } else {
                            log.warn("MQTT client ${clientId} failed to connect", result.cause())
                            VehicleSimCounters.mqttConnectFailed.inc()
                            continuation.resumeWithException(result.cause())
                        }
                    }
                }
                c.closeHandler {
                    log.debug("MQTT client {} closed", clientId)
                    VehicleSimCounters.mqttConnectionsClosed.inc()
                    VehicleSimCounters.mqttCurrentConections.dec()
                }
                client = c
                c
            }
        }
    }

    suspend fun disconnectMqtt() {
        clientInitMutex.withLock {
            val c = client
            if (c != null) {
                c.disconnect()
                VehicleSimCounters.mqttDisconnectSent.inc()
                client = null
            }
        }
    }

    companion object {
        val log = LoggerFactory.getLogger(MqttConnect::class.java)
    }
}