package com.apk.music_phone.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material.icons.filled.WatchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.apk.music_phone.data.manager.WearNodeManager
import com.apk.music_phone.data.model.WatchNode
import com.apk.music_phone.ui.theme.WatchBadgeGreen

@Composable
fun DeviceHeaderCard(
    connectedNode: WatchNode?,
    availableNodes: List<WatchNode>,
    isSearching: Boolean,
    hasEverConnectedWatch: Boolean,
    lastWatchName: String,
    onRefreshNodes: () -> Unit,
    onSelectNode: (WatchNode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showNodeMenu by remember { mutableStateOf(false) }

    if (connectedNode == null && !hasEverConnectedWatch) {
        return
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (connectedNode != null) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (connectedNode != null) Icons.Default.Watch else Icons.Default.WatchOff,
                            contentDescription = "Watch Status",
                            tint = if (connectedNode != null) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = connectedNode?.displayName
                                    ?: if (lastWatchName.isNotBlank()) "$lastWatchName Disconnected" else "Watch Disconnected",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (connectedNode?.isNearby == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(WatchBadgeGreen)
                                )
                            }
                        }

                        Text(
                            text = when {
                                isSearching -> "Searching for connected watch..."
                                connectedNode != null && connectedNode.hasApp -> "Connected & Ready for Music Sync"
                                connectedNode != null -> "Watch Connected • Open Music Player App on Watch"
                                else -> "Watch disconnected. Reconnect via Galaxy Wearable."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        IconButton(onClick = onRefreshNodes) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Watch Nodes",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            if (connectedNode != null && !connectedNode.hasApp) {
                Spacer(modifier = Modifier.padding(top = 10.dp))
                Button(
                    onClick = { WearNodeManager.openWearableApp(context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Open Galaxy Wearable / Watch Manager",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = availableNodes.size > 1) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Box {
                        OutlinedButton(
                            onClick = { showNodeMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Switch Watch (${availableNodes.size} available)")
                        }

                        DropdownMenu(
                            expanded = showNodeMenu,
                            onDismissRequest = { showNodeMenu = false }
                        ) {
                            availableNodes.forEach { node ->
                                DropdownMenuItem(
                                    text = {
                                        Text("${node.displayName} ${if (node.hasApp) "(App Ready)" else ""}")
                                    },
                                    onClick = {
                                        showNodeMenu = false
                                        onSelectNode(node)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
