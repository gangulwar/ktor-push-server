package com.gangulwar

import com.gangulwar.application.NotificationManager
import com.gangulwar.model.Notification
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Up and running!")
        }

        post("/sendNotification") {
            val request = call.receive<Notification>()
            NotificationManager.sendNotification(request.deviceId, request.message)
            call.respond(HttpStatusCode.OK, "Notification sent")
        }

        post("/broadcast") {
            val request = call.receive<Notification>()
            NotificationManager.broadcast(request.message)
            call.respond(HttpStatusCode.OK, "Broadcast sent")
        }
    }
}
