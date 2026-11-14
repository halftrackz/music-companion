package com.apk.music_phone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WatchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.apk.music_phone.ui.MainUiState
import com.apk.music_phone.ui.components.WatchFileExplorer
import com.apk.music_phone.ui.components.WatchTrackItem
import java.util.Locale

@Composable
fun WatchStorageScreen(
    state: MainUiState,
    onRefreshWatchTracks: () -> Unit,
    onDeleteWatchTrack: (fileName: String) -> Unit,
    onToggleViewMode: () -> Unit,
    onNavigateToDirectory: (path: String) -> Unit,
    onCreateDirectory: (newFolderPath: String) -> Unit,
    onMoveFile: (sourcePath: String, targetPath: String) -> Unit,
    onDeleteFile: (path: String) -> Unit,
    onRefreshDirectory: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.connectedNode == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WatchOff,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
                Text(
                    text = "No Smartwatch Connected",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pair a Wear OS smartwatch running the Standalone Music Player app to inspect and manage its storage library.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val totalSizeBytes = state.watchTracks.sumOf { it.sizeBytes }
    val formattedTotalMb = if (totalSizeBytes > 0) {
        String.format(Locale.US, "%.1f MB", totalSizeBytes / (1024.0 * 1024.0))
    } else ""

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Watch Storage",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                    Column {
                        Text(
                            text = "${state.connectedNode.displayName} Library",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${state.watchTracks.size} Tracks" + if (formattedTotalMb.isNotBlank()) " • $formattedTotalMb" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }

                IconButton(onClick = onRefreshWatchTracks) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Watch Library",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = !state.showFileExplorer,
                onClick = { if (state.showFileExplorer) onToggleViewMode() },
                label = { Text("Music Library") },
                leadingIcon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) }
            )

            FilterChip(
                selected = state.showFileExplorer,
                onClick = { if (!state.showFileExplorer) onToggleViewMode() },
                label = { Text("Watch File Explorer") },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
            )
        }

        if (state.showFileExplorer) {
            WatchFileExplorer(
                directoryContent = state.watchDirectoryContent,
                onNavigateToDirectory = onNavigateToDirectory,
                onCreateDirectory = onCreateDirectory,
                onMoveFile = onMoveFile,
                onDeleteFile = onDeleteFile,
                onRefreshDirectory = onRefreshDirectory
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tracks Stored on Watch (/sdcard/Music/Transferred)",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (state.isLoadingWatchTracks) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        strokeWidth = 2.dp
                    )
                }
            }

            if (state.watchTracks.isEmpty() && !state.isLoadingWatchTracks) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Column {
                            Text(
                                text = "Keep Watch App Open for Syncing",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open the Standalone Music Player app on your watch to receive transferred music and display watch library items.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(
                        items = state.watchTracks,
                        key = { it.id.ifBlank { it.fileName } }
                    ) { watchTrack ->
                        WatchTrackItem(
                            track = watchTrack,
                            onDeleteTrack = onDeleteWatchTrack
                        )
                    }
                }
            }
        }
    }
}
