package com.apk.music_phone.data.config

import android.content.Context
import android.content.SharedPreferences

class AppConfig(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("music_phone_config", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_HAS_EVER_CONNECTED_WATCH = "has_ever_connected_watch"
        private const val KEY_LAST_WATCH_NAME = "last_watch_name"
    }

    var hasEverConnectedWatch: Boolean
        get() = prefs.getBoolean(KEY_HAS_EVER_CONNECTED_WATCH, false)
        set(value) {
            prefs.edit().putBoolean(KEY_HAS_EVER_CONNECTED_WATCH, value).apply()
        }

    var lastWatchName: String
        get() = prefs.getString(KEY_LAST_WATCH_NAME, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_LAST_WATCH_NAME, value).apply()
        }
}
