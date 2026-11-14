package com.apk.music_phone.data.manager

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.PowerManager
import android.util.Log
import com.apk.music_phone.data.model.WatchDirectoryContent
import com.apk.music_phone.data.model.WatchTrack
import com.google.android.gms.wearable.Asset
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import kotlin.time.Duration.Companion.milliseconds

class WearTransferManager(private val context: Context) {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    companion object {
        private const val TAG = "WearTransferManager"
        const val PATH_MUSIC_TRANSFER_PREFIX = "/music_transfer"
        const val PATH_REQUEST_TRACK_LIST = "/request_track_list"
        const val PATH_TRACK_LIST_RESPONSE = "/track_list_response"
        const val PATH_DELETE_TRACK = "/delete_track"
        const val PATH_REQUEST_DIRECTORY_LIST = "/request_directory_list"
        const val PATH_DIRECTORY_LIST_RESPONSE = "/directory_list_response"
        const val PATH_CREATE_DIRECTORY = "/create_directory"
        const val PATH_MOVE_FILE = "/move_file"
    }

    /**
     * Sends a single music file as an Asset via DataClient for robust background sync.
     */
    suspend fun sendTrack(
        @Suppress("UNUSED_PARAMETER") nodeId: String,
        fileUri: Uri?,
        fileName: String,
        onProgress: (bytesTransferred: Long, totalBytes: Long, progress: Float) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        if (fileUri == null) return@withContext false

        // --- DEBUG: Check connected Wear OS nodes before pushing data ---
        try {
            val nodes = nodeClient.connectedNodes.await()
            Log.d(TAG, ">>> WEAR NODE CHECK: Found ${nodes.size} connected node(s) <<<")
            nodes.forEach { node ->
                Log.d(TAG, ">>> Connected Node -> ID: ${node.id}, Name: ${node.displayName}, isNearby: ${node.isNearby}")
            }
            if (nodes.isEmpty()) {
                Log.w(TAG, ">>> WARNING: 0 connected Wear OS nodes! Play Services will queue items locally until bridged.")
            }
        } catch (e: Exception) {
            Log.e(TAG, ">>> Failed to query connected nodes: ${e.message}")
        }
        // -----------------------------------------------------------------

        val cleanFileName = fileName.replace("/", "_").replace("\\", "_")
        Log.d(TAG, "sendTrack via DataClient asset started: fileName=$cleanFileName, fileUri=$fileUri")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        @Suppress("DEPRECATION")
        val wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MusicPhone:AssetTransfer")
        wakeLock?.acquire(10 * 60 * 1000L)

        try {
            // Read content URI into byte array
            val audioBytes = context.contentResolver.openInputStream(fileUri)?.use { input ->
                input.readBytes()
            } ?: return@withContext false

            val totalBytes = audioBytes.size.toLong()
            onProgress(0L, totalBytes, 0.2f)

            Log.d(TAG, "Read audio file into memory: $totalBytes bytes. Creating Asset...")

            // Create Wear OS Asset
            val asset = Asset.createFromBytes(audioBytes)

            onProgress(totalBytes / 2, totalBytes, 0.5f)

            // Build PutDataMapRequest with asset and metadata
            val putDataMapReq = PutDataMapRequest.create("$PATH_MUSIC_TRANSFER_PREFIX/$cleanFileName").apply {
                dataMap.putAsset("audio_asset", asset)
                dataMap.putString("fileName", cleanFileName)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }

            // Mark as urgent for immediate synchronization
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()

            dataClient.putDataItem(putDataReq).await()

            Log.d(TAG, "DataClient putDataItem completed successfully for $cleanFileName!")
            onProgress(totalBytes, totalBytes, 1.0f)
            true

        } catch (e: Exception) {
            Log.e(TAG, "DataClient asset transfer failed: ${e.message}", e)
            false
        } finally {
            wakeLock?.let { if (it.isHeld) it.release() }
        }
    }

    /**
     * Sends multiple tracks sequentially with a 5-second pacing delay between each track
     * to ensure the watch storage and CPU never bottleneck.
     */
    suspend fun sendTracksSequentially(
        nodeId: String,
        tracks: List<Pair<Uri?, String>>, // Pair of (fileUri, fileName)
        onTrackFinished: (fileName: String, success: Boolean) -> Unit
    ) = withContext(Dispatchers.IO) {
        val total = tracks.size
        tracks.forEachIndexed { index, (uri, name) ->
            val success = sendTrack(nodeId, uri, name) { _, _, _ -> }
            onTrackFinished(name, success)

            // 5-second pacing delay between songs
            if (index < total - 1) {
                delay(5000L)
            }
        }
    }

    suspend fun requestWatchTracks(nodeId: String, timeoutMs: Long = 10000L): List<WatchTrack> = withContext(Dispatchers.IO) {
        val deferredResponse = CompletableDeferred<List<WatchTrack>>()
        val listener = MessageClient.OnMessageReceivedListener { messageEvent: MessageEvent ->
            if (messageEvent.path == PATH_TRACK_LIST_RESPONSE || messageEvent.path == "/tracks_response") {
                val jsonString = String(messageEvent.data, Charsets.UTF_8)
                val tracks = WatchTrack.parseJsonArray(jsonString)
                deferredResponse.complete(tracks)
            }
        }

        try {
            messageClient.addListener(listener).await()
            messageClient.sendMessage(nodeId, PATH_REQUEST_TRACK_LIST, byteArrayOf()).await()
            withTimeoutOrNull(timeoutMs.milliseconds) { deferredResponse.await() } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "requestWatchTracks error: ${e.message}", e)
            emptyList()
        } finally {
            try { messageClient.removeListener(listener).await() } catch (_: Exception) {}
        }
    }

    suspend fun requestWatchDirectoryContent(
        nodeId: String,
        directoryPath: String = "${Environment.getExternalStorageDirectory().path}/Music",
        timeoutMs: Long = 10000L,
    ): WatchDirectoryContent = withContext(Dispatchers.IO) {
        val deferredResponse = CompletableDeferred<WatchDirectoryContent>()
        val listener = MessageClient.OnMessageReceivedListener { messageEvent: MessageEvent ->
            if (messageEvent.path == PATH_DIRECTORY_LIST_RESPONSE || messageEvent.path == "/directory_response") {
                val jsonString = String(messageEvent.data, Charsets.UTF_8)
                val content = WatchDirectoryContent.parseJson(jsonString)
                deferredResponse.complete(content)
            }
        }

        try {
            messageClient.addListener(listener).await()
            val payload = directoryPath.toByteArray(Charsets.UTF_8)
            messageClient.sendMessage(nodeId, PATH_REQUEST_DIRECTORY_LIST, payload).await()
            withTimeoutOrNull(timeoutMs.milliseconds) { deferredResponse.await() } ?: WatchDirectoryContent(currentPath = directoryPath)
        } catch (e: Exception) {
            Log.e(TAG, "requestWatchDirectoryContent error: ${e.message}", e)
            WatchDirectoryContent(currentPath = directoryPath)
        } finally {
            try { messageClient.removeListener(listener).await() } catch (_: Exception) {}
        }
    }

    suspend fun createWatchDirectory(nodeId: String, directoryPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            messageClient.sendMessage(nodeId, PATH_CREATE_DIRECTORY, directoryPath.toByteArray(Charsets.UTF_8)).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "createWatchDirectory error: ${e.message}", e)
            false
        }
    }

    suspend fun moveWatchFile(nodeId: String, sourcePath: String, targetPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject().apply {
                put("sourcePath", sourcePath)
                put("targetPath", targetPath)
            }.toString()
            messageClient.sendMessage(nodeId, PATH_MOVE_FILE, json.toByteArray(Charsets.UTF_8)).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "moveWatchFile error: ${e.message}", e)
            false
        }
    }

    suspend fun deleteWatchTrack(nodeId: String, fileNameOrPath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            messageClient.sendMessage(nodeId, PATH_DELETE_TRACK, fileNameOrPath.toByteArray(Charsets.UTF_8)).await()
            true
        } catch (e: Exception) {
            Log.e(TAG, "deleteWatchTrack error: ${e.message}", e)
            false
        }
    }

    fun observeWatchMessages(): Flow<Pair<String, ByteArray>> = callbackFlow {
        val listener = MessageClient.OnMessageReceivedListener { event ->
            trySend(event.path to event.data)
        }
        messageClient.addListener(listener)
        awaitClose {
            try { messageClient.removeListener(listener) } catch (_: Exception) {}
        }
    }
}