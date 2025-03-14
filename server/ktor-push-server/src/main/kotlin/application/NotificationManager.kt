package com.gangulwar.application

import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

object NotificationManager {
    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    fun addSession(deviceId: String, session: WebSocketSession) {
        sessions[deviceId] = session
    }

    fun removeSession(deviceId: String) {
        sessions.remove(deviceId)
    }

    suspend fun sendNotification(deviceId: String, message: String) {
        sessions[deviceId]?.send(Frame.Text(message))
    }

    suspend fun broadcast(message: String) {
        sessions.values.forEach { session ->
            session.send(Frame.Text(message))
        }
    }
}