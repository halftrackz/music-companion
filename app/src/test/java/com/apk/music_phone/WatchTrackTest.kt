package com.apk.music_phone

import com.apk.music_phone.data.model.WatchTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchTrackTest {

    @Test
    fun parseJsonArray_validInput_returnsWatchTracks() {
        val json = """
            [
                {"id":"101","title":"Bohemian Rhapsody","artist":"Queen","duration":354000,"filename":"bohemian_rhapsody.mp3","sizeBytes":8500000},
                {"id":"102","title":"Hotel California","artist":"Eagles","durationMs":390000,"fileName":"hotel_california.mp3","size":9200000}
            ]
        """.trimIndent()

        val tracks = WatchTrack.parseJsonArray(json)

        assertEquals(2, tracks.size)

        assertEquals("101", tracks[0].id)
        assertEquals("Bohemian Rhapsody", tracks[0].title)
        assertEquals("Queen", tracks[0].artist)
        assertEquals(354000L, tracks[0].durationMs)
        assertEquals("bohemian_rhapsody.mp3", tracks[0].fileName)
        assertEquals(8500000L, tracks[0].sizeBytes)
        assertEquals("5:54", tracks[0].formattedDuration)
        assertEquals("8.1 MB", tracks[0].formattedSize)

        assertEquals("102", tracks[1].id)
        assertEquals("Hotel California", tracks[1].title)
        assertEquals("Eagles", tracks[1].artist)
        assertEquals(390000L, tracks[1].durationMs)
        assertEquals("hotel_california.mp3", tracks[1].fileName)
        assertEquals(9200000L, tracks[1].sizeBytes)
        assertEquals("6:30", tracks[1].formattedDuration)
        assertEquals("8.8 MB", tracks[1].formattedSize)
    }

    @Test
    fun parseJsonArray_emptyString_returnsEmptyList() {
        val tracks = WatchTrack.parseJsonArray("")
        assertTrue(tracks.isEmpty())
    }

    @Test
    fun parseJsonArray_invalidJson_returnsEmptyList() {
        val tracks = WatchTrack.parseJsonArray("invalid_json_content")
        assertTrue(tracks.isEmpty())
    }
}
