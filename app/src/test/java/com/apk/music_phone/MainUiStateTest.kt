package com.apk.music_phone

import com.apk.music_phone.data.model.PhoneTrack
import com.apk.music_phone.data.model.WatchTrack
import com.apk.music_phone.ui.MainUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainUiStateTest {

    @Test
    fun isTrackOnWatch_matchingFileName_returnsTrue() {
        val phoneTrack = PhoneTrack(
            id = "1",
            title = "Song A",
            artist = "Artist A",
            album = "Album A",
            durationMs = 180000L,
            sizeBytes = 4000000L,
            fileName = "song_a.mp3",
            mimeType = "audio/mpeg"
        )

        val watchTrack = WatchTrack(
            id = "w1",
            title = "Different Title",
            artist = "Different Artist",
            durationMs = 180000L,
            fileName = "song_a.mp3",
            sizeBytes = 4000000L
        )

        val uiState = MainUiState(watchTracks = listOf(watchTrack))

        assertTrue(uiState.isTrackOnWatch(phoneTrack))
    }

    @Test
    fun isTrackOnWatch_matchingTitleAndArtist_returnsTrue() {
        val phoneTrack = PhoneTrack(
            id = "1",
            title = "Song B",
            artist = "Artist B",
            album = "Album B",
            durationMs = 210000L,
            sizeBytes = 5000000L,
            fileName = "phone_file_b.mp3",
            mimeType = "audio/mpeg"
        )

        val watchTrack = WatchTrack(
            id = "w2",
            title = "Song B",
            artist = "Artist B",
            durationMs = 210000L,
            fileName = "watch_file_b.mp3",
            sizeBytes = 5000000L
        )

        val uiState = MainUiState(watchTracks = listOf(watchTrack))

        assertTrue(uiState.isTrackOnWatch(phoneTrack))
    }

    @Test
    fun isTrackOnWatch_notPresentOnWatch_returnsFalse() {
        val phoneTrack = PhoneTrack(
            id = "1",
            title = "Song C",
            artist = "Artist C",
            album = "Album C",
            durationMs = 240000L,
            sizeBytes = 6000000L,
            fileName = "song_c.mp3",
            mimeType = "audio/mpeg"
        )

        val uiState = MainUiState(
            watchTracks = listOf(
                WatchTrack(
                    id = "w1",
                    title = "Other Song",
                    artist = "Other Artist",
                    durationMs = 180000L,
                    fileName = "other.mp3"
                )
            )
        )

        assertFalse(uiState.isTrackOnWatch(phoneTrack))
    }
}
