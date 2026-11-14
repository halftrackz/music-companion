package com.apk.music_phone

import com.apk.music_phone.data.model.PhoneTrack
import com.apk.music_phone.data.repository.PhoneTrackRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneTrackRepositoryTest {

    private val sampleTracks = listOf(
        PhoneTrack(
            id = "1",
            title = "Shape of You",
            artist = "Ed Sheeran",
            album = "Divide",
            durationMs = 233000L,
            sizeBytes = 5500000L,
            fileName = "shape_of_you.mp3",
            mimeType = "audio/mpeg"
        ),
        PhoneTrack(
            id = "2",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationMs = 200000L,
            sizeBytes = 4800000L,
            fileName = "blinding_lights.mp3",
            mimeType = "audio/mpeg"
        ),
        PhoneTrack(
            id = "3",
            title = "Perfect",
            artist = "Ed Sheeran",
            album = "Divide",
            durationMs = 263000L,
            sizeBytes = 6100000L,
            fileName = "perfect.mp3",
            mimeType = "audio/mpeg"
        )
    )

    @Test
    fun searchTracks_byArtist_returnsMatchingTracks() {
        val repository = PhoneTrackRepository(DummyContext())
        val results = repository.searchTracks("Ed Sheeran", sampleTracks)

        assertEquals(2, results.size)
        assertEquals("1", results[0].id)
        assertEquals("3", results[1].id)
    }

    @Test
    fun searchTracks_byTitleCaseInsensitive_returnsMatchingTracks() {
        val repository = PhoneTrackRepository(DummyContext())
        val results = repository.searchTracks("blinding", sampleTracks)

        assertEquals(1, results.size)
        assertEquals("Blinding Lights", results[0].title)
    }

    @Test
    fun searchTracks_emptyQuery_returnsAllTracks() {
        val repository = PhoneTrackRepository(DummyContext())
        val results = repository.searchTracks("   ", sampleTracks)

        assertEquals(3, results.size)
    }
}

private class DummyContext : android.content.ContextWrapper(null)
