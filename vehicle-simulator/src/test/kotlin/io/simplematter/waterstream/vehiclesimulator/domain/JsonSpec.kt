package io.simplematter.waterstream.vehiclesimulator.domain

import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.simplematter.waterstream.vehiclesimulator.config.MqttConfig
import io.simplematter.waterstream.vehiclesimulator.tools.JsonUtils
import io.simplematter.waterstream.vehiclesimulator.verticles.VehicleVerticle
import io.vertx.core.json.JsonObject

class JsonSpec : StringSpec() {
    init {
        "Serialize and deserialize MqttConfig with ObjectMapper" {
            val mqttConfig = MqttConfig(
                host = "host1",
                port = 1883,
                fleetClientId = "fleet",
                topicPrefix = "t1/",
                username = null,
                password = null
            )

            val str = JsonUtils.mapper.writeValueAsString(mqttConfig)
            println("str=$str")

            val mqttConfigDeserialized = JsonUtils.mapper.readValue(str, MqttConfig::class.java)
            mqttConfigDeserialized shouldBe mqttConfig
        }

        "Serialize and deserialize MqttConfig with JsonObject" {
            val mqttConfig = MqttConfig(
                host = "host1",
                port = 1883,
                fleetClientId = "fleet",
                topicPrefix = "t1/",
                username = null,
                password = null
            )

            val jsonObject = JsonObject.mapFrom(mqttConfig)

//            val mqttConfigDeserialized = jsonObject.mapTo(MqttConfig::class.java)
            val mqttConfigDeserialized = JsonUtils.fromVertxJson(jsonObject, MqttConfig::class.java)
            mqttConfigDeserialized shouldBe mqttConfig


        }

    }
}
