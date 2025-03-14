package com.gangulwar.ktor_push.service

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object ServiceManager {
    val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning.asStateFlow()

    fun startService(context: Context) {
        if (!_isServiceRunning.value) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(Intent(context, NotificationService::class.java))
            } else {
                context.startService(Intent(context, NotificationService::class.java))
            }
            _isServiceRunning.value = true
        }
    }

    fun stopService(context: Context) {
        if (_isServiceRunning.value) {
            context.stopService(Intent(context, NotificationService::class.java))
            _isServiceRunning.value = false
        }
    }

    fun updateServiceStatus(context: Context) {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        _isServiceRunning.value = manager.getRunningServices(Integer.MAX_VALUE)
            .any { it.service.className == NotificationService::class.java.name }
    }
}