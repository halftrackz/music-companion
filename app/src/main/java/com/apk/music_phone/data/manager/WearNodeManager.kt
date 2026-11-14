package com.apk.music_phone.data.manager

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.apk.music_phone.data.model.WatchNode
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class WearNodeManager(context: Context) {
    private val capabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    companion object {
        private const val TAG = "WearNodeManager"
        const val CAPABILITY_WEAR_MUSIC_PLAYER = "wear_music_player"

        fun openWearableApp(context: Context) {
            val wearablePackages = listOf(
                "com.samsung.android.app.watchmanager",
                "com.google.android.apps.wear.companion",
                "com.google.android.wearable.app",
                "com.mobvoi.companion.at",
            )

            for (pkg in wearablePackages) {
                val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    try {
                        context.startActivity(intent)
                        return
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            try {
                val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    suspend fun getConnectedMusicWatchNode(): WatchNode? {
        val nodes = getConnectedMusicWatchNodes()
        return nodes.firstOrNull { it.hasApp && it.isNearby }
            ?: nodes.firstOrNull { it.hasApp }
            ?: nodes.firstOrNull { it.isNearby }
            ?: nodes.firstOrNull()
    }

    suspend fun getConnectedMusicWatchNodes(): List<WatchNode> {
        return try {
            val connectedNodes = nodeClient.connectedNodes.await()
            Log.d(TAG, "Connected raw Wearable nodes: ${connectedNodes.map { "${it.displayName} (${it.id})" }}")

            val capabilityNodes = try {
                capabilityClient.getCapability(
                    CAPABILITY_WEAR_MUSIC_PLAYER,
                    CapabilityClient.FILTER_ALL
                ).await().nodes
            } catch (e: Exception) {
                Log.w(TAG, "Failed capability query: ${e.message}")
                emptySet()
            }

            Log.d(TAG, "Capability '$CAPABILITY_WEAR_MUSIC_PLAYER' nodes: ${capabilityNodes.map { "${it.displayName} (${it.id})" }}")

            val capabilityNodeIds = capabilityNodes.asSequence().map { it.id }.toSet()

            val resultMap = mutableMapOf<String, WatchNode>()

            for (node in connectedNodes) {
                val hasApp = capabilityNodeIds.contains(node.id) || connectedNodes.isNotEmpty()
                resultMap[node.id] = WatchNode(
                    id = node.id,
                    displayName = node.displayName,
                    isNearby = node.isNearby,
                    isConnected = true,
                    hasApp = hasApp
                )
            }

            for (node in capabilityNodes) {
                if (!resultMap.containsKey(node.id)) {
                    resultMap[node.id] = WatchNode(
                        id = node.id,
                        displayName = node.displayName,
                        isNearby = node.isNearby,
                        isConnected = true,
                        hasApp = true
                    )
                }
            }

            resultMap.values.toList()
        } catch (e: Exception) {
            Log.e(TAG, "Error discovering watch nodes: ${e.message}", e)
            emptyList()
        }
    }

    fun observeConnectedWatchNodes(): Flow<List<WatchNode>> = callbackFlow {
        val listener = CapabilityClient.OnCapabilityChangedListener {
            try {
                val nodes = kotlinx.coroutines.runBlocking { getConnectedMusicWatchNodes() }
                trySend(nodes)
            } catch (_: Exception) {
                trySend(emptyList())
            }
        }

        capabilityClient.addListener(listener, CAPABILITY_WEAR_MUSIC_PLAYER)

        try {
            val initialNodes = getConnectedMusicWatchNodes()
            trySend(initialNodes)
        } catch (_: Exception) {
            trySend(emptyList())
        }

        awaitClose {
            capabilityClient.removeListener(listener)
        }
    }
}
