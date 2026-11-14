package com.apk.music_phone

import com.apk.music_phone.data.model.WatchDirectoryContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchFileExplorerTest {

    @Test
    fun parseJson_validDirectoryContent_returnsParsedItems() {
        val json = """
            {
                "currentPath": "/sdcard/Music",
                "parentPath": "/sdcard",
                "items": [
                    {
                        "name": "Transferred",
                        "path": "/sdcard/Music/Transferred",
                        "isDirectory": true,
                        "sizeBytes": 0,
                        "itemCount": 12
                    },
                    {
                        "name": "song.mp3",
                        "path": "/sdcard/Music/song.mp3",
                        "isDirectory": false,
                        "sizeBytes": 5242880,
                        "itemCount": 0
                    }
                ]
            }
        """.trimIndent()

        val content = WatchDirectoryContent.parseJson(json)

        assertEquals("/sdcard/Music", content.currentPath)
        assertEquals("/sdcard", content.parentPath)
        assertEquals(2, content.items.size)

        val dirItem = content.items[0]
        assertEquals("Transferred", dirItem.name)
        assertTrue(dirItem.isDirectory)
        assertEquals(12, dirItem.itemCount)
        assertEquals("12 items", dirItem.formattedSize)

        val fileItem = content.items[1]
        assertEquals("song.mp3", fileItem.name)
        assertFalse(fileItem.isDirectory)
        assertEquals(5242880L, fileItem.sizeBytes)
        assertEquals("5.0 MB", fileItem.formattedSize)
    }

    @Test
    fun parseJson_emptyJson_returnsDefault() {
        val content = WatchDirectoryContent.parseJson("")
        assertEquals("/sdcard/Music", content.currentPath)
        assertTrue(content.items.isEmpty())
    }
}
