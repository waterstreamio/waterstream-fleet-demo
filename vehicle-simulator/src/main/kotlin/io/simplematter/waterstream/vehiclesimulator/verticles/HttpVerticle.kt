package io.simplematter.waterstream.vehiclesimulator.verticles

import io.prometheus.client.hotspot.DefaultExports
import io.prometheus.client.vertx.MetricsHandler
import io.simplematter.waterstream.vehiclesimulator.SocketAddress
import io.simplematter.waterstream.vehiclesimulator.clientprotocol.ClientMessage
import io.simplematter.waterstream.vehiclesimulator.clientprotocol.toFleetCompositionMessage
import io.simplematter.waterstream.vehiclesimulator.clientprotocol.toVehicleRemoveMessage
import io.simplematter.waterstream.vehiclesimulator.clientprotocol.toVehicleUpdatedMessage
import io.simplematter.waterstream.vehiclesimulator.config.MonitoringConfig
import io.simplematter.waterstream.vehiclesimulator.config.VehicleSimulatorConfig
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.sockjs.BridgeOptions
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle

class HttpVerticle : CoroutineVerticle() {

    companion object {
        private val log = LoggerFactory.getLogger(HttpVerticle::class.java.name)!!
    }

    override suspend fun start() {
        // TODO: we should avoid reading config from every verticle. We should instead leverage Vertx config tools
        val serviceConfig = VehicleSimulatorConfig.load()

        val router = Router.router(vertx)

        router.route().handler(
            CorsHandler.create(serviceConfig.allowedOriginPattern)
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.OPTIONS)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.CONNECT)
                .allowedMethod(HttpMethod.OTHER)
                .allowedHeader("Access-Control-Request-Method")
                .allowedHeader("Access-Control-Allow-Credentials")
                .allowedHeader("Access-Control-Allow-Origin")
                .allowedHeader("Access-Control-Allow-Headers")
                .allowedHeader("Content-Type")
        )

        router.route("/eventbus/*").handler(websocketHandler())

        vertx.eventBus().consumer(ClientMessage.Out.FleetComposition, Handler<Message<JsonArray>> { event ->
            vertx.eventBus().publish(SocketAddress.Outbound, toFleetCompositionMessage(event.body()))
        })

        exposeMonitoringEndpoint(serviceConfig.monitoring)

        // Start the server
        vertx
            .createHttpServer()
            .requestHandler(router)
            .listenAwait(serviceConfig.httpPort)
    }

    private fun exposeMonitoringEndpoint(config: MonitoringConfig) {
        if (config.includeJavaMetrics)
            DefaultExports.initialize()
        val router = Router.router(vertx)
        router.route(config.metricsEndpoint).handler(MetricsHandler())
        val server = vertx.createHttpServer()
        server.requestHandler(router).listen(config.port)
    }

    private fun websocketHandler(): SockJSHandler {
        val options = BridgeOptions()
            .addOutboundPermitted(PermittedOptions().setAddressRegex(SocketAddress.Outbound))
            .addInboundPermitted(PermittedOptions().setAddressRegex(SocketAddress.InBound))

        val eventBus = vertx.eventBus()

        val sockJSHandler: SockJSHandler = SockJSHandler.create(vertx)

        sockJSHandler.bridge(options) { event ->
            if (event.type() === io.vertx.ext.bridge.BridgeEventType.SOCKET_CREATED) {
                eventBus.publish(ClientMessage.In.Connected, event.rawMessage)
                log.info("A socket was created")
            }

            if (event.type() === io.vertx.ext.bridge.BridgeEventType.SEND) {
                log.info("RawMessage was received ${event.rawMessage}")
                eventBus.publish(ClientMessage.In.Connected, event.rawMessage)
            }

            event.complete(true)
        }
        return sockJSHandler
    }
}
