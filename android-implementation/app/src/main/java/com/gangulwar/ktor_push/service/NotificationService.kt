package com.gangulwar.ktor_push.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.WebSocketSession
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NotificationService : Service() {

    private val client = HttpClient { install(WebSockets) }
    private var webSocketSession: WebSocketSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Service created")
        startForegroundService()
        ServiceManager._isServiceRunning.value = true
        connectWebSocket()
    }

    private fun startForegroundService() {
        Log.d("NotificationService", "Starting foreground service")
        val channelId = "notification_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Notification Service", NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notification Service Running")
            .setContentText("Maintaining WebSocket connection...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(1, notification)
        Log.d("NotificationService", "Started foreground service")
    }

    private fun connectWebSocket() {
        serviceScope.launch {
            try {
                webSocketSession = client.webSocketSession("ws://192.168.1.11:8080/connect/device123")
                Log.d("WebSocket", "Connected to WebSocket")

                for (frame in webSocketSession!!.incoming) {
                    if (frame is Frame.Text) {
                        val message = frame.readText()
                        Log.d("WebSocket", "Received: $message")

                        sendPushNotification(message)
                    }
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Error: ${e.message}")
            }
        }
    }

    fun sendNotification(message: String) {
        serviceScope.launch {
            webSocketSession?.send(Frame.Text(message))
        }
    }

    private fun sendPushNotification(message: String) {
        val channelId = "websocket_message_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "WebSocket Messages", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for WebSocket messages"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("New Message Received")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    override fun onDestroy() {
        Log.d("NotificationService", "Stopping foreground service")
        serviceScope.cancel()
        ServiceManager._isServiceRunning.value = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
