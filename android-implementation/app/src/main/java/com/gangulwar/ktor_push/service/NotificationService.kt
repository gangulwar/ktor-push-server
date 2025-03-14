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
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class NotificationService : Service() {

    private val client = HttpClient {
        install(WebSockets) {
            pingInterval = 30000
        }
    }
    private var webSocketSession: WebSocketSession? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val maxReconnectAttempts = 5
    private var reconnectAttempt = 0
    private val baseReconnectDelay = 5000L
    private val maxReconnectDelay = 60000L
    private var reconnectJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("NotificationService", "Service created")
        startForegroundService()
        ServiceManager.updateState(ServiceManager.ServiceState.Connecting(1))
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
        reconnectJob?.cancel()

        reconnectAttempt++
        ServiceManager.updateState(ServiceManager.ServiceState.Connecting(reconnectAttempt))

        serviceScope.launch {
            try {
                Log.d("WebSocket", "Connecting to WebSocket (Attempt: $reconnectAttempt)")
                webSocketSession = client.webSocketSession("ws://192.168.1.11:8080/connect/device123")
                Log.d("WebSocket", "Connected to WebSocket")

                reconnectAttempt = 0
                ServiceManager.updateState(ServiceManager.ServiceState.Running)

                try {
                    for (frame in webSocketSession!!.incoming) {
                        if (frame is Frame.Text) {
                            val message = frame.readText()
                            Log.d("WebSocket", "Received: $message")
                            sendPushNotification(message)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket", "Connection lost: ${e.message}")
                    handleConnectionError(e)
                }
            } catch (e: Exception) {
                Log.e("WebSocket", "Connection error: ${e.message}")
                handleConnectionError(e)
            }
        }
    }

    private fun handleConnectionError(e: Exception) {
        if (reconnectAttempt < maxReconnectAttempts) {
            val delayMillis = calculateReconnectDelay()
            val errorMessage = "Connection error: ${e.message ?: "Unknown error"}. Reconnecting in ${delayMillis/1000} seconds."
            Log.d("WebSocket", errorMessage)

            ServiceManager.updateState(ServiceManager.ServiceState.Error(
                errorMessage,
                (delayMillis/1000).toInt()
            ))

            reconnectJob = serviceScope.launch {
                delay(delayMillis)
                connectWebSocket()
            }
        } else {
            val errorMessage = "Failed to connect after $maxReconnectAttempts attempts. Please check your connection and restart the service."
            Log.e("WebSocket", errorMessage)
            ServiceManager.updateState(ServiceManager.ServiceState.Error(errorMessage, 0))
        }
    }

    private fun calculateReconnectDelay(): Long {
        val exponentialDelay = baseReconnectDelay * (1 shl (reconnectAttempt - 1))
        val cappedDelay = exponentialDelay.coerceAtMost(maxReconnectDelay)
        val jitter = (0..1000).random()
        return cappedDelay + jitter
    }

    fun sendNotification(message: String) {
        serviceScope.launch {
            try {
                webSocketSession?.send(Frame.Text(message))
                Log.d("WebSocket", "Sent message: $message")
            } catch (e: Exception) {
                Log.e("WebSocket", "Error sending message: ${e.message}")
                if (webSocketSession == null || ServiceManager.serviceState.value !is ServiceManager.ServiceState.Running) {
                    handleConnectionError(e)
                }
            }
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
        reconnectJob?.cancel()
        serviceScope.cancel()
        ServiceManager.updateState(ServiceManager.ServiceState.Stopped)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "SEND_NOTIFICATION") {
            val message = intent.getStringExtra("message") ?: ""
            if (message.isNotEmpty()) {
                sendNotification(message)
            }
        }

        return START_STICKY
    }
}