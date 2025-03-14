package com.gangulwar.ktor_push.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceManager {
    sealed class ServiceState {
        object Stopped : ServiceState()
        object Running : ServiceState()
        data class Connecting(val attempt: Int) : ServiceState()
        data class Error(val message: String, val reconnectIn: Int) : ServiceState()
    }

    private val _serviceState = MutableStateFlow<ServiceState>(ServiceState.Stopped)
    val serviceState = _serviceState.asStateFlow()

    val isServiceRunning: Boolean
        get() = _serviceState.value !is ServiceState.Stopped

    fun updateState(state: ServiceState) {
        _serviceState.value = state
    }

    fun startService(context: Context) {
        if (_serviceState.value is ServiceState.Stopped) {
            updateState(ServiceState.Connecting(1))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, NotificationService::class.java))
            } else {
                context.startService(Intent(context, NotificationService::class.java))
            }
        }
    }

    fun stopService(context: Context) {
        if (_serviceState.value !is ServiceState.Stopped) {
            context.stopService(Intent(context, NotificationService::class.java))
            updateState(ServiceState.Stopped)
        }
    }

    fun updateServiceStatus(context: Context) {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isRunning = manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == NotificationService::class.java.name }

        if (!isRunning && _serviceState.value !is ServiceState.Stopped) {
            updateState(ServiceState.Stopped)
        }
    }
}