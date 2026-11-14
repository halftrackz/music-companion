package com.apk.music_phone.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.apk.music_phone.data.model.PhoneTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PhoneTrackRepository(private val context: Context) {

    private fun isInsideMusicFolder(filePath: String, relativePath: String): Boolean {
        val combined = "$filePath $relativePath".lowercase().replace("\\", "/")
        if (combined.trim().isEmpty()) return true
        return combined.contains("music")
    }

    private fun isIgnoredAudio(
        title: String,
        fileName: String,
        mimeType: String,
        filePath: String,
        relativePath: String,
    ): Boolean {
        // IGNORE .OGG AND AUDIO/OGG FILES
        if (fileName.endsWith(".ogg", ignoreCase = true) || mimeType.contains("ogg", ignoreCase = true)) {
            return true
        }

        if (fileName.endsWith(".amr", ignoreCase = true) || mimeType.contains("amr", ignoreCase = true)) {
            return true
        }

        val normalizedTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "")
        val normalizedFileName = fileName.lowercase().replace(Regex("[^a-z0-9]"), "")
        if (normalizedTitle.contains("overthehorizon") || normalizedFileName.contains("overthehorizon")) {
            return true
        }

        if (!isInsideMusicFolder(filePath, relativePath)) {
            return true
        }

        return false
    }

    suspend fun scanPhoneTracks(): List<PhoneTrack> = withContext(Dispatchers.IO) {
        val trackList = mutableListOf<PhoneTrack>()
        val seenIds = mutableSetOf<String>()

        val collection: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projectionList = mutableListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.Audio.Media.RELATIVE_PATH)
        }

        val projection = projectionList.toTypedArray()
        val selection = "${MediaStore.Audio.Media.SIZE} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val dataColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
                val relativePathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
                } else -1

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val displayName = cursor.getString(displayNameColumn) ?: "$title.mp3"
                    val mimeType = cursor.getString(mimeTypeColumn) ?: "audio/mpeg"
                    val albumId = cursor.getLong(albumIdColumn)
                    val filePath = if (dataColumn >= 0) cursor.getString(dataColumn) ?: "" else ""
                    val relativePath = if (relativePathColumn >= 0) cursor.getString(relativePathColumn) ?: "" else ""

                    if (isIgnoredAudio(title, displayName, mimeType, filePath, relativePath)) {
                        continue
                    }

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = if (albumId > 0) {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        )
                    } else null

                    val trackId = id.toString()
                    seenIds.add(trackId)

                    trackList.add(
                        PhoneTrack(
                            id = trackId,
                            title = if (title.isBlank() || title == "Unknown Title") displayName.substringBeforeLast(".") else title,
                            artist = if (artist == "<unknown>" || artist.isBlank()) "Unknown Artist" else artist,
                            album = if (album == "<unknown>" || album.isBlank()) "Unknown Album" else album,
                            durationMs = duration,
                            sizeBytes = size,
                            fileName = displayName,
                            mimeType = mimeType,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        scanMusicDirectoryFallback(trackList)

        trackList
    }

    private fun scanMusicDirectoryFallback(trackList: MutableList<PhoneTrack>) {
        val musicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
        if (musicDir != null && musicDir.exists() && musicDir.isDirectory) {
            // "ogg" has been removed from supported extensions
            val supportedExtensions = setOf("mp3", "m4a", "flac", "wav", "aac", "wma", "opus", "m4p", "mp4")
            val existingFileNames = trackList.map { it.fileName.lowercase() }.toSet()

            musicDir.walkTopDown().maxDepth(5).forEach { file ->
                if (file.isFile && file.length() > 10000) {
                    val ext = file.extension.lowercase()
                    if (supportedExtensions.contains(ext) && !existingFileNames.contains(file.name.lowercase())) {
                        val name = file.name
                        if (!isIgnoredAudio(name, name, "audio/$ext", file.absolutePath, "Music/")) {
                            trackList.add(
                                PhoneTrack(
                                    id = file.absolutePath.hashCode().toString(),
                                    title = file.nameWithoutExtension,
                                    artist = "Local Music",
                                    album = file.parentFile?.name ?: "Music",
                                    durationMs = 0L,
                                    sizeBytes = file.length(),
                                    fileName = file.name,
                                    mimeType = "audio/$ext",
                                    contentUri = Uri.fromFile(file)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun searchTracks(query: String, tracks: List<PhoneTrack>): List<PhoneTrack> {
        if (query.isBlank()) return tracks
        val lower = query.lowercase().trim()
        return tracks.filter { (title, artist, album, _, _, _, fileName) ->
            title.lowercase().contains(lower) ||
                    artist.lowercase().contains(lower) ||
                    album.lowercase().contains(lower) ||
                    fileName.lowercase().contains(lower)
        }
    }
}