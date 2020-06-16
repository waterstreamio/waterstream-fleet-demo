package io.simplematter.waterstream.vehiclesimulator.config

import com.typesafe.config.ConfigFactory
import io.github.config4k.extract

data class VehicleSimulatorConfig(
  val vehicles: VehiclesConfig,
  val mqtt: MqttConfig,
  val routing: RoutingConfig,
  val kafka: KafkaConfig,
  val monitoring: MonitoringConfig,
  val httpPort: Int,
  val allowedOriginPattern: String
) {
    companion object {
        fun load(): VehicleSimulatorConfig {
            val config = ConfigFactory.load()
            return config.extract<VehicleSimulatorConfig>()
        }
    }
}

data class VehiclesConfig(
  val totalNumber: Int,
  val visibleNumber: Int
)

data class MqttConfig(
  val host: String,
  val port: Int,
  val fleetClientId: String,
  val topicPrefix: String,
  val username: String?,
  val password: String?
)

data class RoutingConfig(val url: String)

data class KafkaConfig(
  val bootstrapServers: String,
  val vehiclesStatsTopic: String,
  val consumerGroup: String
)

data class MonitoringConfig(
  val port: Int = 1884,
  val metricsEndpoint: String = "/metrics",
  val includeJavaMetrics: Boolean = true
)
