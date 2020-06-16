package io.simplematter.waterstream.vehiclesimulator.tools

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.vertx.core.json.JsonObject


object JsonUtils {
    val mapper = ObjectMapper().registerKotlinModule()

    fun <T> fromVertxJson(jsonObject: JsonObject, targetClass: Class<T>): T {
        return mapper.readValue<T>(jsonObject.encode(), targetClass)
    }
}