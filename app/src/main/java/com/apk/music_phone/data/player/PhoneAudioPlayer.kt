package com.apk.music_phone.data.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaMetadata
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.util.Size
import com.apk.music_phone.data.model.PhoneTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class PlayerState(
    val currentTrack: PhoneTrack? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val playlist: List<PhoneTrack> = emptyList(),
    val queue: List<PhoneTrack> = emptyList(),
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
)

class PhoneAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    val mediaSession: MediaSession = MediaSession(context, "PhoneAudioPlayer").apply {
        setCallback(object : MediaSession.Callback() {
            override fun onPlay() {
                resume()
            }

            override fun onPause() {
                pause()
            }

            override fun onSkipToNext() {
                skipNext()
            }

            override fun onSkipToPrevious() {
                skipPrevious()
            }

            override fun onSeekTo(pos: Long) {
                seekTo(pos)
            }
        })
        isActive = true
    }

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playTrack(track: PhoneTrack, playlist: List<PhoneTrack> = emptyList()) {
        val currentList = playlist.ifEmpty { _playerState.value.playlist }
        releasePlayer()

        val uri = track.contentUri ?: return

        try {
            mediaPlayer = MediaPlayer().apply {
                setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(context, uri)
                prepare()
                start()
            }

            val newQueue = if (_playerState.value.isShuffle) {
                listOf(track) + (currentList.filter { it.id != track.id }.shuffled())
            } else {
                currentList
            }

            val duration = mediaPlayer?.duration?.toLong() ?: track.durationMs

            _playerState.update {
                it.copy(
                    currentTrack = track,
                    isPlaying = true,
                    currentPositionMs = 0L,
                    durationMs = duration,
                    playlist = currentList,
                    queue = newQueue
                )
            }

            updateSystemMediaSession(track, true, 0L, duration)
            startProgressTicker()

            mediaPlayer?.setOnCompletionListener {
                if (_playerState.value.isRepeat) {
                    playTrack(track, currentList)
                } else {
                    skipNext()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _playerState.update { it.copy(isPlaying = false) }
        }
    }

    fun pause() {
        mediaPlayer?.pause()
        _playerState.update { it.copy(isPlaying = false) }
        stopProgressTicker()

        val track = _playerState.value.currentTrack
        if (track != null) {
            updateSystemMediaSession(track, false, _playerState.value.currentPositionMs, _playerState.value.durationMs)
        }
    }

    fun resume() {
        val track = _playerState.value.currentTrack
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            _playerState.update { it.copy(isPlaying = true) }
            startProgressTicker()
            if (track != null) {
                updateSystemMediaSession(track, true, _playerState.value.currentPositionMs, _playerState.value.durationMs)
            }
        } else if (track != null) {
            playTrack(track)
        }
    }

    fun togglePlayPause() {
        if (_playerState.value.isPlaying) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.seekTo(positionMs.toInt())
        _playerState.update { it.copy(currentPositionMs = positionMs) }
        val track = _playerState.value.currentTrack
        if (track != null) {
            updateSystemMediaSession(track, _playerState.value.isPlaying, positionMs, _playerState.value.durationMs)
        }
    }

    fun skipNext() {
        val (currentTrack, _, _, _, playlist, queue, _, _) = _playerState.value
        val activeList = if (queue.isNotEmpty()) queue else playlist
        if (activeList.isEmpty() || currentTrack == null) return

        val currentIndex = activeList.indexOfFirst { it.id == currentTrack.id }
        if (currentIndex != -1) {
            val nextIndex = (currentIndex + 1) % activeList.size
            playTrack(activeList[nextIndex], playlist)
        }
    }

    fun skipPrevious() {
        val (currentTrack, _, _, _, playlist, queue, _, _) = _playerState.value
        val activeList = if (queue.isNotEmpty()) queue else playlist
        if (activeList.isEmpty() || currentTrack == null) return

        val currentIndex = activeList.indexOfFirst { it.id == currentTrack.id }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) activeList.size - 1 else currentIndex - 1
            playTrack(activeList[prevIndex], playlist)
        }
    }

    fun moveQueueItemUp(index: Int) {
        val currentQueue = _playerState.value.queue.toMutableList()
        if (index <= 0 || index >= currentQueue.size) return
        val item = currentQueue.removeAt(index)
        currentQueue.add(index - 1, item)
        _playerState.update { it.copy(queue = currentQueue) }
    }

    fun moveQueueItemDown(index: Int) {
        val currentQueue = _playerState.value.queue.toMutableList()
        if (index < 0 || index >= currentQueue.size - 1) return
        val item = currentQueue.removeAt(index)
        currentQueue.add(index + 1, item)
        _playerState.update { it.copy(queue = currentQueue) }
    }

    fun toggleShuffle() {
        val newShuffle = !_playerState.value.isShuffle
        val (currentTrack, _, _, _, playlist, _, _, _) = _playerState.value
        val newQueue = if (newShuffle && currentTrack != null) {
            listOf(currentTrack) + (playlist.filter { it.id != currentTrack.id }.shuffled())
        } else {
            playlist
        }
        _playerState.update {
            it.copy(
                isShuffle = newShuffle,
                queue = newQueue
            )
        }
    }

    fun toggleRepeat() {
        _playerState.update { it.copy(isRepeat = !it.isRepeat) }
    }

    private fun updateSystemMediaSession(track: PhoneTrack, isPlaying: Boolean, positionMs: Long, durationMs: Long) {
        try {
            val bitmap = loadAlbumArtBitmap(track)

            val metadataBuilder = MediaMetadata.Builder()
                .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
                .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
                .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
                .putLong(MediaMetadata.METADATA_KEY_DURATION, durationMs)

            if (track.year > 0) {
                metadataBuilder.putLong(MediaMetadata.METADATA_KEY_YEAR, track.year.toLong())
            }

            bitmap?.let {
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, it)
                metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, it)
            }

            track.albumArtUri?.let { artUri ->
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_ALBUM_ART_URI, artUri.toString())
                metadataBuilder.putString(MediaMetadata.METADATA_KEY_ART_URI, artUri.toString())
            }

            mediaSession.setMetadata(metadataBuilder.build())

            val state = PlaybackState.Builder()
                .setState(
                    if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                    positionMs,
                    1.0f
                )
                .setActions(
                    PlaybackState.ACTION_PLAY or
                            PlaybackState.ACTION_PAUSE or
                            PlaybackState.ACTION_PLAY_PAUSE or
                            PlaybackState.ACTION_SKIP_TO_NEXT or
                            PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                            PlaybackState.ACTION_SEEK_TO
                )
                .build()

            mediaSession.setPlaybackState(state)

            AudioPlayerService.startService(
                context = context,
                title = track.title,
                artist = track.artist,
                album = track.album,
                playing = isPlaying,
                bitmap = bitmap,
                token = mediaSession.sessionToken
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadAlbumArtBitmap(track: PhoneTrack): Bitmap? {
        val uri = track.contentUri ?: track.albumArtUri ?: return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.contentResolver.loadThumbnail(uri, Size(600, 600), null)
            } else {
                track.albumArtUri?.let { artUri ->
                    context.contentResolver.openInputStream(artUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
            }
        } catch (_: Exception) {
            try {
                track.albumArtUri?.let { artUri ->
                    context.contentResolver.openInputStream(artUri)?.use { input ->
                        BitmapFactory.decodeStream(input)
                    }
                }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val pos = player.currentPosition.toLong()
                        _playerState.update {
                            it.copy(currentPositionMs = pos)
                        }
                    }
                }
                delay(500.milliseconds)
            }
        }
    }

    private fun stopProgressTicker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun releasePlayer() {
        stopProgressTicker()
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        try {
            mediaSession.isActive = false
            AudioPlayerService.stopService(context)
        } catch (_: Exception) {}
    }
}
