package com.apk.music_phone.data.model

import org.json.JSONArray

data class WatchTrack(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val fileName: String,
    val sizeBytes: Long = 0L
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
        }

    val formattedSize: String
        get() {
            if (sizeBytes <= 0) return ""
            val mb = sizeBytes / (1024.0 * 1024.0)
            return String.format(java.util.Locale.US, "%.1f MB", mb)
        }

    companion object {
        fun parseJsonArray(jsonString: String): List<WatchTrack> {
            val result = mutableListOf<WatchTrack>()
            if (jsonString.isBlank()) return result
            try {
                val array = JSONArray(jsonString)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", i.toString())
                    val title = obj.optString("title", "Unknown Title")
                    val artist = obj.optString("artist", "Unknown Artist")
                    val durationMs = obj.optLong("duration", obj.optLong("durationMs", 0L))
                    val fileName = obj.optString("fileName", obj.optString("filename", "$title.mp3"))
                    val sizeBytes = obj.optLong("sizeBytes", obj.optLong("size", 0L))
                    result.add(
                        WatchTrack(
                            id = id,
                            title = title,
                            artist = artist,
                            durationMs = durationMs,
                            fileName = fileName,
                            sizeBytes = sizeBytes
                        )
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return result
        }
    }
}
