package com.apk.music_phone.data.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.media.session.MediaController
import android.media.session.MediaSession
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import androidx.core.app.NotificationCompat
import com.apk.music_phone.MainActivity
import com.apk.music_phone.R

class AudioPlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "music_player_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY_PAUSE = "com.apk.music_phone.ACTION_PLAY_PAUSE"
        const val ACTION_NEXT = "com.apk.music_phone.ACTION_NEXT"
        const val ACTION_PREVIOUS = "com.apk.music_phone.ACTION_PREVIOUS"
        const val ACTION_STOP = "com.apk.music_phone.ACTION_STOP"

        var mediaSessionToken: MediaSession.Token? = null
        var currentTitle: String = "Playing Music"
        var currentArtist: String = "Phone Audio Player"
        var currentAlbum: String = "Unknown Album"
        var currentBitmap: Bitmap? = null
        var isPlaying: Boolean = false

        fun startService(
            context: Context,
            title: String,
            artist: String,
            album: String,
            playing: Boolean,
            bitmap: Bitmap?,
            token: MediaSession.Token?
        ) {
            currentTitle = title
            currentArtist = artist
            currentAlbum = album
            isPlaying = playing
            currentBitmap = bitmap
            mediaSessionToken = token

            val intent = Intent(context, AudioPlayerService::class.java)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, AudioPlayerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        mediaSessionToken?.let { token ->
            try {
                val controller = MediaController(this, token)
                when (action) {
                    ACTION_PLAY_PAUSE -> {
                        if (isPlaying) {
                            controller.transportControls.pause()
                        } else {
                            controller.transportControls.play()
                        }
                    }
                    ACTION_NEXT -> {
                        controller.transportControls.skipToNext()
                    }
                    ACTION_PREVIOUS -> {
                        controller.transportControls.skipToPrevious()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithNotification()
        return START_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Phone Music Playback",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Media playback controls and notification"
            setSound(null, null)
            enableVibration(false)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, AudioPlayerService::class.java).apply { action = ACTION_PREVIOUS }
        val prevPendingIntent = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(this, AudioPlayerService::class.java).apply { action = ACTION_PLAY_PAUSE }
        val playPausePendingIntent = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, AudioPlayerService::class.java).apply { action = ACTION_NEXT }
        val nextPendingIntent = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val subtitle = if (currentAlbum.isNotBlank() && currentAlbum != "Unknown Album") {
            "$currentArtist • $currentAlbum"
        } else {
            currentArtist
        }

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(currentTitle)
            .setContentText(subtitle)
            .setSubText(currentAlbum)
            .setOngoing(isPlaying)
            .setContentIntent(contentPendingIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPendingIntent)
            .addAction(
                if (isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (isPlaying) "Pause" else "Play",
                playPausePendingIntent
            )
            .addAction(android.R.drawable.ic_media_next, "Next", nextPendingIntent)

        currentBitmap?.let { bmp ->
            builder.setLargeIcon(bmp)
        }

        mediaSessionToken?.let { token ->
            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(MediaSessionCompat.Token.fromToken(token))
                    .setShowActionsInCompactView(0, 1, 2)
            )
        }

        return builder.build()
    }
}
