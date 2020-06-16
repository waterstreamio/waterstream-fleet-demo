package io.simplematter.waterstream.vehiclesimulator

import io.simplematter.waterstream.vehiclesimulator.config.VehicleSimulatorConfig
import io.simplematter.waterstream.vehiclesimulator.verticles.FleetVerticle
import io.simplematter.waterstream.vehiclesimulator.verticles.HttpVerticle
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.file.FileSystemOptions
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory

class Service {

  companion object {
    private val log = LoggerFactory.getLogger(Service::class.java.name)!!

    @JvmStatic
    fun main(args: Array<String>) {
      val options = VertxOptions()
        .setFileSystemOptions(
          FileSystemOptions()
            .setFileCachingEnabled(false)
        )

      val vertx = Vertx.vertx(options)

      vertx.deployVerticle(HttpVerticle::class.qualifiedName)
      vertx.deployFleetVerticle(VehicleSimulatorConfig.load())
      log.info("Service started")
    }
  }
}

fun Vertx.deployFleetVerticle(config: VehicleSimulatorConfig) {
  val configJson = JsonObject.mapFrom(config)

  val deployOptions = DeploymentOptions()
  deployOptions.config = configJson

  this.deployVerticle(FleetVerticle::class.qualifiedName, deployOptions)
}
