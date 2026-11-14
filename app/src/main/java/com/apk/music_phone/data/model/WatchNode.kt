package com.apk.music_phone.data.model

data class WatchNode(
    val id: String,
    val displayName: String,
    val isNearby: Boolean,
    val isConnected: Boolean = true,
    val hasApp: Boolean = false
)
