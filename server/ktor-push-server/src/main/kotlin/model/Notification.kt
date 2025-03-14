package com.gangulwar.model

import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val deviceId: String,
    val message: String
)
