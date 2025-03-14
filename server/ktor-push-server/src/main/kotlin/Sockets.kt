package com.gangulwar

import com.gangulwar.application.NotificationManager
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = 15.seconds
        timeout = 15.seconds
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    routing {
        webSocket("/connect/{deviceId}") {
            val deviceId = call.parameters["deviceId"] ?: return@webSocket close()
            NotificationManager.addSession(deviceId, this)

            try {
                for (frame in incoming) {
                    if (frame is Frame.Text) {
                        val receivedMessage = frame.readText()
                        println("Received from $deviceId: $receivedMessage")
                    }
                }
            } catch (e: Exception) {
                println("Connection lost for $deviceId")
            } finally {
                NotificationManager.removeSession(deviceId)
            }
        }
    }

}