package com.apk.music_phone.data.model

import android.net.Uri

data class PhoneTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val year: Int = 0,
    val durationMs: Long,
    val sizeBytes: Long,
    val fileName: String,
    val mimeType: String,
    val contentUri: Uri? = null,
    val albumArtUri: Uri? = null
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
            val mb = sizeBytes / (1024.0 * 1024.0)
            return String.format(java.util.Locale.US, "%.1f MB", mb)
        }
}
