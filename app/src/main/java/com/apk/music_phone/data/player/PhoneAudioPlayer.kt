package com.apk.music_phone.data.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
    val isShuffle: Boolean = false,
    val isRepeat: Boolean = false,
)

class PhoneAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    fun playTrack(track: PhoneTrack, playlist: List<PhoneTrack> = emptyList()) {
        val currentList = playlist.ifEmpty { _playerState.value.playlist }
        releasePlayer()

        val uri = track.contentUri ?: return

        try {
            mediaPlayer = MediaPlayer().apply {
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

            _playerState.update {
                it.copy(
                    currentTrack = track,
                    isPlaying = true,
                    currentPositionMs = 0L,
                    durationMs = mediaPlayer?.duration?.toLong() ?: track.durationMs,
                    playlist = currentList
                )
            }

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
    }

    fun resume() {
        val track = _playerState.value.currentTrack
        if (mediaPlayer != null) {
            mediaPlayer?.start()
            _playerState.update { it.copy(isPlaying = true) }
            startProgressTicker()
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
    }

    fun skipNext() {
        val (currentTrack, _, _, _, playlist, isShuffle, _) = _playerState.value
        if (playlist.isEmpty() || currentTrack == null) return

        val currentIndex = playlist.indexOfFirst { it.id == currentTrack.id }
        if (currentIndex != -1) {
            val nextIndex = if (isShuffle) {
                (playlist.indices).random()
            } else {
                (currentIndex + 1) % playlist.size
            }
            playTrack(playlist[nextIndex], playlist)
        }
    }

    fun skipPrevious() {
        val (currentTrack, _, _, _, playlist, _, _) = _playerState.value
        if (playlist.isEmpty() || currentTrack == null) return

        val currentIndex = playlist.indexOfFirst { it.id == currentTrack.id }
        if (currentIndex != -1) {
            val prevIndex = if (currentIndex - 1 < 0) playlist.size - 1 else currentIndex - 1
            playTrack(playlist[prevIndex], playlist)
        }
    }

    fun toggleShuffle() {
        _playerState.update { it.copy(isShuffle = !it.isShuffle) }
    }

    fun toggleRepeat() {
        _playerState.update { it.copy(isRepeat = !it.isRepeat) }
    }

    private fun startProgressTicker() {
        stopProgressTicker()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        _playerState.update {
                            it.copy(currentPositionMs = player.currentPosition.toLong())
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
    }
}
