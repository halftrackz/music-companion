package com.apk.music_phone.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.apk.music_phone.data.config.AppConfig
import com.apk.music_phone.data.manager.WearNodeManager
import com.apk.music_phone.data.manager.WearTransferManager
import com.apk.music_phone.data.model.PhoneTrack
import com.apk.music_phone.data.model.TransferProgress
import com.apk.music_phone.data.model.TransferStatus
import com.apk.music_phone.data.model.WatchDirectoryContent
import com.apk.music_phone.data.model.WatchNode
import com.apk.music_phone.data.model.WatchTrack
import com.apk.music_phone.data.player.PhoneAudioPlayer
import com.apk.music_phone.data.player.PlayerState
import com.apk.music_phone.data.repository.PhoneTrackRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appConfig = AppConfig(application)
    private val nodeManager = WearNodeManager(application)
    private val phoneTrackRepository = PhoneTrackRepository(application)
    private val transferManager = WearTransferManager(application)
    private val audioPlayer = PhoneAudioPlayer(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            hasEverConnectedWatch = appConfig.hasEverConnectedWatch,
            lastWatchName = appConfig.lastWatchName
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState

    init {
        observeWatchNodes()
        observeWatchMessages()
    }

    private fun observeWatchNodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingNodes = true) }
            nodeManager.observeConnectedWatchNodes().collect { nodes ->
                val connected = nodes.firstOrNull { it.hasApp && it.isNearby }
                    ?: nodes.firstOrNull { it.hasApp }
                    ?: nodes.firstOrNull { it.isNearby }
                    ?: nodes.firstOrNull()

                if (connected != null && connected.hasApp) {
                    appConfig.hasEverConnectedWatch = true
                    appConfig.lastWatchName = connected.displayName
                }

                _uiState.update { currentState ->
                    currentState.copy(
                        availableNodes = nodes,
                        connectedNode = connected,
                        hasEverConnectedWatch = appConfig.hasEverConnectedWatch,
                        lastWatchName = appConfig.lastWatchName,
                        isSearchingNodes = false
                    )
                }
                connected?.let { node ->
                    fetchWatchTracks(node.id)
                    fetchWatchDirectoryContent(node.id)
                }
            }
        }
    }

    private fun observeWatchMessages() {
        viewModelScope.launch {
            transferManager.observeWatchMessages().collect { (path, data) ->
                if (path == WearTransferManager.PATH_TRACK_LIST_RESPONSE) {
                    val jsonString = String(data, Charsets.UTF_8)
                    val tracks = WatchTrack.parseJsonArray(jsonString)
                    _uiState.update {
                        it.copy(
                            watchTracks = tracks,
                            isLoadingWatchTracks = false
                        )
                    }
                } else if (path == WearTransferManager.PATH_DIRECTORY_LIST_RESPONSE) {
                    val jsonString = String(data, Charsets.UTF_8)
                    val dirContent = WatchDirectoryContent.parseJson(jsonString)
                    _uiState.update {
                        it.copy(
                            watchDirectoryContent = dirContent,
                            isLoadingDirectory = false
                        )
                    }
                }
            }
        }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasStoragePermission = true) }
        loadPhoneTracks()
    }

    fun loadPhoneTracks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanningPhone = true) }
            val tracks = phoneTrackRepository.scanPhoneTracks()
            val filtered = phoneTrackRepository.searchTracks(_uiState.value.searchQuery, tracks)
            _uiState.update {
                it.copy(
                    phoneTracks = tracks,
                    filteredPhoneTracks = filtered,
                    isScanningPhone = false
                )
            }
        }
    }

    fun playPhoneTrack(track: PhoneTrack) {
        val playlist = _uiState.value.filteredPhoneTracks
        audioPlayer.playTrack(track, playlist)
    }

    fun togglePlayerPlayPause() {
        audioPlayer.togglePlayPause()
    }

    fun skipPlayerNext() {
        audioPlayer.skipNext()
    }

    fun skipPlayerPrevious() {
        audioPlayer.skipPrevious()
    }

    fun seekPlayerTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
    }

    fun togglePlayerShuffle() {
        audioPlayer.toggleShuffle()
    }

    fun togglePlayerRepeat() {
        audioPlayer.toggleRepeat()
    }

    fun moveQueueItemUp(index: Int) {
        audioPlayer.moveQueueItemUp(index)
    }

    fun moveQueueItemDown(index: Int) {
        audioPlayer.moveQueueItemDown(index)
    }

    fun onSearchQueryChanged(query: String) {
        val filtered = phoneTrackRepository.searchTracks(query, _uiState.value.phoneTracks)
        _uiState.update {
            it.copy(
                searchQuery = query,
                filteredPhoneTracks = filtered
            )
        }
    }

    fun selectTab(tab: MainTab) {
        _uiState.update { it.copy(activeTab = tab) }
        if (tab == MainTab.WATCH_STORAGE) {
            _uiState.value.connectedNode?.let { node ->
                fetchWatchTracks(node.id)
                fetchWatchDirectoryContent(node.id)
            }
        }
    }

    fun toggleFileExplorerView() {
        _uiState.update { it.copy(showFileExplorer = !it.showFileExplorer) }
    }

    fun fetchWatchDirectoryContent(
        nodeId: String? = _uiState.value.connectedNode?.id,
        directoryPath: String = _uiState.value.watchDirectoryContent.currentPath
    ) {
        if (nodeId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingDirectory = true) }
            val content = transferManager.requestWatchDirectoryContent(nodeId, directoryPath)
            _uiState.update {
                it.copy(
                    watchDirectoryContent = content,
                    isLoadingDirectory = false
                )
            }
        }
    }

    fun createWatchDirectory(newFolderPath: String) {
        val node = _uiState.value.connectedNode ?: return
        viewModelScope.launch {
            val success = transferManager.createWatchDirectory(node.id, newFolderPath)
            if (success) {
                _uiState.update { it.copy(userMessage = "Created folder on watch: $newFolderPath") }
                fetchWatchDirectoryContent(node.id, _uiState.value.watchDirectoryContent.currentPath)
            } else {
                _uiState.update { it.copy(userMessage = "Failed to create folder on watch") }
            }
        }
    }

    fun moveWatchFile(sourcePath: String, targetPath: String) {
        val node = _uiState.value.connectedNode ?: return
        viewModelScope.launch {
            val success = transferManager.moveWatchFile(node.id, sourcePath, targetPath)
            if (success) {
                _uiState.update { it.copy(userMessage = "Moved file on watch") }
                fetchWatchDirectoryContent(node.id, _uiState.value.watchDirectoryContent.currentPath)
                fetchWatchTracks(node.id)
            } else {
                _uiState.update { it.copy(userMessage = "Failed to move file on watch") }
            }
        }
    }

    fun deleteWatchFile(path: String) {
        val node = _uiState.value.connectedNode ?: return
        viewModelScope.launch {
            val success = transferManager.deleteWatchTrack(node.id, path)
            if (success) {
                _uiState.update { it.copy(userMessage = "Deleted from watch: $path") }
                fetchWatchDirectoryContent(node.id, _uiState.value.watchDirectoryContent.currentPath)
                fetchWatchTracks(node.id)
            } else {
                _uiState.update { it.copy(userMessage = "Failed to delete file on watch") }
            }
        }
    }

    fun toggleTrackSelection(trackId: String) {
        _uiState.update { state ->
            val current = state.selectedTrackIds.toMutableSet()
            if (current.contains(trackId)) {
                current.remove(trackId)
            } else {
                current.add(trackId)
            }
            state.copy(selectedTrackIds = current)
        }
    }

    fun selectAllTracks() {
        _uiState.update { state ->
            val allIds = state.filteredPhoneTracks.map { it.id }.toSet()
            state.copy(selectedTrackIds = allIds)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedTrackIds = emptySet()) }
    }

    fun selectNode(node: WatchNode) {
        if (node.hasApp) {
            appConfig.hasEverConnectedWatch = true
            appConfig.lastWatchName = node.displayName
        }
        _uiState.update {
            it.copy(
                connectedNode = node,
                hasEverConnectedWatch = appConfig.hasEverConnectedWatch,
                lastWatchName = appConfig.lastWatchName
            )
        }
        fetchWatchTracks(node.id)
        fetchWatchDirectoryContent(node.id)
    }

    fun refreshWatchNodes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingNodes = true) }
            val nodes = nodeManager.getConnectedMusicWatchNodes()
            val connected = nodes.firstOrNull { it.hasApp && it.isNearby }
                ?: nodes.firstOrNull { it.hasApp }
                ?: nodes.firstOrNull { it.isNearby }
                ?: nodes.firstOrNull()

            if (connected != null && connected.hasApp) {
                appConfig.hasEverConnectedWatch = true
                appConfig.lastWatchName = connected.displayName
            }

            _uiState.update {
                it.copy(
                    availableNodes = nodes,
                    connectedNode = connected,
                    hasEverConnectedWatch = appConfig.hasEverConnectedWatch,
                    lastWatchName = appConfig.lastWatchName,
                    isSearchingNodes = false
                )
            }
            connected?.let {
                fetchWatchTracks(it.id)
                fetchWatchDirectoryContent(it.id)
            }
        }
    }

    fun fetchWatchTracks(nodeId: String? = _uiState.value.connectedNode?.id) {
        if (nodeId == null) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingWatchTracks = true) }
            val tracks = transferManager.requestWatchTracks(nodeId)
            _uiState.update {
                it.copy(
                    watchTracks = tracks,
                    isLoadingWatchTracks = false
                )
            }
        }
    }

    fun transferSelectedTracks() {
        val node = _uiState.value.connectedNode
        if (node == null) {
            _uiState.update { it.copy(userMessage = "No smartwatch connected!") }
            return
        }

        val selectedIds = _uiState.value.selectedTrackIds
        val tracksToTransfer = _uiState.value.phoneTracks.filter { selectedIds.contains(it.id) }

        if (tracksToTransfer.isEmpty()) {
            _uiState.update { it.copy(userMessage = "No tracks selected") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBatchTransferring = true,
                    activeTab = MainTab.TRANSFERS
                )
            }

            var successCount = 0

            for (track in tracksToTransfer) {
                val transferId = UUID.randomUUID().toString()
                val initialProgress = TransferProgress(
                    transferId = transferId,
                    fileName = track.fileName,
                    trackTitle = track.title,
                    totalBytes = track.sizeBytes,
                    status = TransferStatus.PENDING
                )

                _uiState.update { state ->
                    state.copy(transfers = listOf(initialProgress) + state.transfers)
                }

                _uiState.update { state ->
                    val updated = state.transfers.map {
                        if (it.transferId == transferId) it.copy(status = TransferStatus.IN_PROGRESS) else it
                    }
                    state.copy(transfers = updated)
                }

                // Correct signature: nodeId, fileUri, fileName, onProgress lambda
                val success = transferManager.sendTrack(
                    nodeId = node.id,
                    fileUri = track.contentUri,
                    fileName = track.fileName
                ) { bytesTransferred, totalBytes, progress ->
                    val actualTotal = if (totalBytes > 0) totalBytes else track.sizeBytes
                    val actualProgress = if (progress >= 0f) progress else if (actualTotal > 0) bytesTransferred.toFloat() / actualTotal.toFloat() else 0f

                    _uiState.update { state ->
                        val updated = state.transfers.map {
                            if (it.transferId == transferId) {
                                it.copy(
                                    bytesTransferred = bytesTransferred,
                                    totalBytes = actualTotal,
                                    progress = actualProgress
                                )
                            } else it
                        }
                        state.copy(transfers = updated)
                    }
                }

                if (success) {
                    successCount++
                    _uiState.update { state ->
                        val updated = state.transfers.map {
                            if (it.transferId == transferId) it.copy(status = TransferStatus.COMPLETED, progress = 1.0f) else it
                        }
                        state.copy(transfers = updated)
                    }
                } else {
                    _uiState.update { state ->
                        val updated = state.transfers.map {
                            if (it.transferId == transferId) it.copy(status = TransferStatus.FAILED, errorMessage = "Transfer failed") else it
                        }
                        state.copy(transfers = updated)
                    }
                }
            }

            _uiState.update {
                it.copy(
                    selectedTrackIds = emptySet(),
                    isBatchTransferring = false,
                    userMessage = "Transferred $successCount of ${tracksToTransfer.size} tracks to watch"
                )
            }

            fetchWatchTracks(node.id)
            fetchWatchDirectoryContent(node.id)
        }
    }

    fun deleteWatchTrack(fileName: String) {
        val node = _uiState.value.connectedNode
        if (node == null) {
            _uiState.update { it.copy(userMessage = "No watch connected to delete track") }
            return
        }

        viewModelScope.launch {
            val success = transferManager.deleteWatchTrack(node.id, fileName)
            if (success) {
                _uiState.update { it.copy(userMessage = "Deleted $fileName on watch") }
                _uiState.update { state ->
                    val updatedWatchTracks = state.watchTracks.filter { it.fileName != fileName }
                    state.copy(watchTracks = updatedWatchTracks)
                }
                fetchWatchTracks(node.id)
                fetchWatchDirectoryContent(node.id)
            } else {
                _uiState.update { it.copy(userMessage = "Failed to delete $fileName on watch") }
            }
        }
    }

    fun clearTransfers() {
        _uiState.update { it.copy(transfers = emptyList()) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.releasePlayer()
    }
}