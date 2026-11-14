package com.apk.music_phone.ui

import com.apk.music_phone.data.model.PhoneTrack
import com.apk.music_phone.data.model.TransferProgress
import com.apk.music_phone.data.model.WatchDirectoryContent
import com.apk.music_phone.data.model.WatchNode
import com.apk.music_phone.data.model.WatchTrack

enum class MainTab(val title: String) {
    PHONE_LIBRARY("Phone Music"),
    WATCH_STORAGE("Watch Storage"),
    TRANSFERS("Transfers")
}

data class MainUiState(
    val activeTab: MainTab = MainTab.PHONE_LIBRARY,
    val connectedNode: WatchNode? = null,
    val availableNodes: List<WatchNode> = emptyList(),
    val isSearchingNodes: Boolean = false,
    val hasEverConnectedWatch: Boolean = false,
    val lastWatchName: String = "",
    val phoneTracks: List<PhoneTrack> = emptyList(),
    val filteredPhoneTracks: List<PhoneTrack> = emptyList(),
    val selectedTrackIds: Set<String> = emptySet(),
    val isScanningPhone: Boolean = false,
    val hasStoragePermission: Boolean = false,
    val searchQuery: String = "",
    val watchTracks: List<WatchTrack> = emptyList(),
    val isLoadingWatchTracks: Boolean = false,
    val watchDirectoryContent: WatchDirectoryContent = WatchDirectoryContent(),
    val isLoadingDirectory: Boolean = false,
    val showFileExplorer: Boolean = false,
    val transfers: List<TransferProgress> = emptyList(),
    val isBatchTransferring: Boolean = false,
    val userMessage: String? = null
) {
    fun isTrackOnWatch(phoneTrack: PhoneTrack): Boolean {
        return watchTracks.any { watchTrack ->
            watchTrack.fileName.equals(phoneTrack.fileName, ignoreCase = true) ||
                    (watchTrack.title.equals(phoneTrack.title, ignoreCase = true) &&
                            watchTrack.artist.equals(phoneTrack.artist, ignoreCase = true))
        }
    }
}
