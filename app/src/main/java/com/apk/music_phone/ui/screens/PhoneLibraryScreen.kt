package com.apk.music_phone.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apk.music_phone.data.model.PhoneTrack
import com.apk.music_phone.data.player.PlayerState
import com.apk.music_phone.ui.MainUiState
import com.apk.music_phone.ui.components.PermissionRequestCard
import com.apk.music_phone.ui.components.PhoneTrackItem

@Composable
fun PhoneLibraryScreen(
    state: MainUiState,
    playerState: PlayerState,
    onPlayTrack: (PhoneTrack) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSelectTrack: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSendToWatch: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.hasStoragePermission) {
        PermissionRequestCard(onRequestPermission = onRequestPermission, modifier = modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = { Text("Search title, artist, album...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${state.filteredPhoneTracks.size} Songs on Phone",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row {
                    if (state.selectedTrackIds.isEmpty()) {
                        TextButton(onClick = onSelectAll) {
                            Text("Select All")
                        }
                    } else {
                        TextButton(onClick = onClearSelection) {
                            Text("Clear (${state.selectedTrackIds.size})")
                        }
                    }
                }
            }

            if (state.isScanningPhone) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Scanning local music files...")
                    }
                }
            } else if (state.filteredPhoneTracks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.padding(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (state.searchQuery.isNotEmpty()) "No songs match '${state.searchQuery}'"
                            else "No audio tracks found on device",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = if (state.selectedTrackIds.isNotEmpty()) 88.dp else 16.dp
                    )
                ) {
                    items(
                        items = state.filteredPhoneTracks,
                        key = { it.id }
                    ) { track ->
                        val isSelected = state.selectedTrackIds.contains(track.id)
                        val isOnWatch = state.isTrackOnWatch(track)
                        val isPlayingOnPhone = playerState.currentTrack?.id == track.id
                        PhoneTrackItem(
                            track = track,
                            isSelected = isSelected,
                            isOnWatch = isOnWatch,
                            isPlayingOnPhone = isPlayingOnPhone,
                            onPlayTrack = { onPlayTrack(track) },
                            onToggleSelect = { onToggleSelectTrack(track.id) }
                        )
                    }
                }
            }
        }

        if (state.selectedTrackIds.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onSendToWatch,
                icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null) },
                text = {
                    Text(
                        text = "Send to Watch (${state.selectedTrackIds.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}
