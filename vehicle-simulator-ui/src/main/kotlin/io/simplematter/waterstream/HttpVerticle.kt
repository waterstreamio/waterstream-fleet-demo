package io.simplematter.waterstream

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.vertx.core.AsyncResult
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Route
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.ext.web.templ.thymeleaf.ThymeleafTemplateEngine
import io.vertx.kotlin.core.http.listenAwait
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.launch

class HttpVerticle : CoroutineVerticle() {

  override suspend fun start() {

    val config = VehicleSimulatorUiConfig.load()
    val engine =  ThymeleafTemplateEngine.create(vertx)

    val router = Router.router(vertx)
    val jsStaticHandler = StaticHandler.create("static/js").setCachingEnabled(false)
    val cssStaticHandler = StaticHandler.create("static/css").setCachingEnabled(false)
    val imgStaticHandler = StaticHandler.create("static/img").setCachingEnabled(false)

    router.get("/js/*").handler(jsStaticHandler)
    router.get("/css/*").handler(cssStaticHandler)
    router.get("/img/*").handler(imgStaticHandler)


    router.get().handler { ctx: RoutingContext ->
      val data = JsonObject()
        .put("messageCount", config.messageCountPanelAddress)
        .put("mqttHost", config.mqttHost)
        .put("mqttPort", config.mqttPort)
        .put("mqttUseSsl", config.mqttUseSsl)
        .put("mqttClientPrefix", config.mqttClientPrefix)
        .put("mqttVisibleVehiclesTopicPrefix", config.mqttVisibleVehiclesTopicPrefix)
        .put("mqttDirectionStatsTopicPrefix", config.mqttDirectionStatsTopicPrefix)

      engine.render(data, "static/html/index.html"
      ) { res: AsyncResult<Buffer?> ->
        if (res.succeeded()) {
          ctx.response().end(res.result())
        } else {
          ctx.fail(res.cause())
        }
      }
    }


    // Start the server
    vertx
      .createHttpServer()
      .requestHandler(router)
      .listenAwait(8080)
  }

  fun Route.coroutineHandler(fn: suspend (RoutingContext) -> Unit) {
    handler { ctx ->
      launch(ctx.vertx().dispatcher()) {
        try {
          fn(ctx)
        } catch (e: Exception) {
          ctx.fail(e)
        }
      }
    }
  }

}
