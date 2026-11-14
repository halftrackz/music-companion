package com.apk.music_phone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apk.music_phone.data.model.PhoneTrack
import com.apk.music_phone.data.model.WatchNode
import com.apk.music_phone.data.player.PlayerState
import com.apk.music_phone.ui.MainTab
import com.apk.music_phone.ui.MainUiState
import com.apk.music_phone.ui.MainViewModel
import com.apk.music_phone.ui.components.DeviceHeaderCard
import com.apk.music_phone.ui.components.FullPlayerSheet
import com.apk.music_phone.ui.components.MiniPlayerBar
import com.apk.music_phone.ui.screens.PhoneLibraryScreen
import com.apk.music_phone.ui.screens.TransfersScreen
import com.apk.music_phone.ui.screens.WatchStorageScreen
import com.apk.music_phone.ui.theme.MusicPhoneTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MusicPhoneTheme {
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val playerState by viewModel.playerState.collectAsStateWithLifecycle()
                val context = LocalContext.current
                val snackbarHostState = remember { SnackbarHostState() }

                var showFullPlayer by remember { mutableStateOf(false) }

                val permissionToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { isGranted ->
                    if (isGranted) {
                        viewModel.onPermissionGranted()
                    }
                }

                LaunchedEffect(Unit) {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        permissionToRequest
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        viewModel.onPermissionGranted()
                    } else {
                        permissionLauncher.launch(permissionToRequest)
                    }
                }

                LaunchedEffect(uiState.userMessage) {
                    uiState.userMessage?.let { msg ->
                        snackbarHostState.showSnackbar(msg)
                        viewModel.clearUserMessage()
                    }
                }

                MainScreenContent(
                    uiState = uiState,
                    playerState = playerState,
                    snackbarHostState = snackbarHostState,
                    onSelectTab = { viewModel.selectTab(it) },
                    onRefreshNodes = { viewModel.refreshWatchNodes() },
                    onSelectNode = { viewModel.selectNode(it) },
                    onPlayTrack = { viewModel.playPhoneTrack(it) },
                    onTogglePlayPause = { viewModel.togglePlayerPlayPause() },
                    onSkipNext = { viewModel.skipPlayerNext() },
                    onClickMiniPlayer = { showFullPlayer = true },
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onToggleSelectTrack = { viewModel.toggleTrackSelection(it) },
                    onSelectAll = { viewModel.selectAllTracks() },
                    onClearSelection = { viewModel.clearSelection() },
                    onSendToWatch = { viewModel.transferSelectedTracks() },
                    onRefreshWatchTracks = { viewModel.fetchWatchTracks() },
                    onDeleteWatchTrack = { viewModel.deleteWatchTrack(it) },
                    onToggleViewMode = { viewModel.toggleFileExplorerView() },
                    onNavigateToDirectory = { viewModel.fetchWatchDirectoryContent(directoryPath = it) },
                    onCreateDirectory = { viewModel.createWatchDirectory(it) },
                    onMoveFile = { src, dest -> viewModel.moveWatchFile(src, dest) },
                    onDeleteFile = { viewModel.deleteWatchFile(it) },
                    onRefreshDirectory = { viewModel.fetchWatchDirectoryContent() },
                    onClearTransfers = { viewModel.clearTransfers() },
                    onRequestPermission = { permissionLauncher.launch(permissionToRequest) },
                )

                if (showFullPlayer && (playerState.currentTrack != null)) {
                    FullPlayerSheet(
                        playerState = playerState,
                        onDismiss = { showFullPlayer = false },
                        onTogglePlayPause = { viewModel.togglePlayerPlayPause() },
                        onSkipNext = { viewModel.skipPlayerNext() },
                        onSkipPrevious = { viewModel.skipPlayerPrevious() },
                        onSeekTo = { viewModel.seekPlayerTo(it) },
                        onToggleShuffle = { viewModel.togglePlayerShuffle() },
                        onToggleRepeat = { viewModel.togglePlayerRepeat() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    uiState: MainUiState,
    playerState: PlayerState,
    snackbarHostState: SnackbarHostState,
    onSelectTab: (MainTab) -> Unit,
    onRefreshNodes: () -> Unit,
    onSelectNode: (WatchNode) -> Unit,
    onPlayTrack: (PhoneTrack) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onClickMiniPlayer: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onToggleSelectTrack: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onSendToWatch: () -> Unit,
    onRefreshWatchTracks: () -> Unit,
    onDeleteWatchTrack: (String) -> Unit,
    onToggleViewMode: () -> Unit,
    onNavigateToDirectory: (String) -> Unit,
    onCreateDirectory: (String) -> Unit,
    onMoveFile: (String, String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onRefreshDirectory: () -> Unit,
    onClearTransfers: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Music Phone Companion",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Column {
                if (playerState.currentTrack != null) {
                    MiniPlayerBar(
                        playerState = playerState,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onClickMiniPlayer = onClickMiniPlayer
                    )
                }

                NavigationBar {
                    NavigationBarItem(
                        selected = uiState.activeTab == MainTab.PHONE_LIBRARY,
                        onClick = { onSelectTab(MainTab.PHONE_LIBRARY) },
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        label = { Text("Phone Music") }
                    )
                    NavigationBarItem(
                        selected = uiState.activeTab == MainTab.WATCH_STORAGE,
                        onClick = { onSelectTab(MainTab.WATCH_STORAGE) },
                        icon = { Icon(Icons.Default.Storage, contentDescription = null) },
                        label = { Text("Watch Storage") }
                    )
                    NavigationBarItem(
                        selected = uiState.activeTab == MainTab.TRANSFERS,
                        onClick = { onSelectTab(MainTab.TRANSFERS) },
                        icon = { Icon(Icons.Default.Sync, contentDescription = null) },
                        label = { Text("Transfers") }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                DeviceHeaderCard(
                    connectedNode = uiState.connectedNode,
                    availableNodes = uiState.availableNodes,
                    isSearching = uiState.isSearchingNodes,
                    hasEverConnectedWatch = uiState.hasEverConnectedWatch,
                    lastWatchName = uiState.lastWatchName,
                    onRefreshNodes = onRefreshNodes,
                    onSelectNode = onSelectNode,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )

                when (uiState.activeTab) {
                    MainTab.PHONE_LIBRARY -> {
                        PhoneLibraryScreen(
                            state = uiState,
                            playerState = playerState,
                            onPlayTrack = onPlayTrack,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onToggleSelectTrack = onToggleSelectTrack,
                            onSelectAll = onSelectAll,
                            onClearSelection = onClearSelection,
                            onSendToWatch = onSendToWatch,
                            onRequestPermission = onRequestPermission
                        )
                    }
                    MainTab.WATCH_STORAGE -> {
                        WatchStorageScreen(
                            state = uiState,
                            onRefreshWatchTracks = onRefreshWatchTracks,
                            onDeleteWatchTrack = onDeleteWatchTrack,
                            onToggleViewMode = onToggleViewMode,
                            onNavigateToDirectory = onNavigateToDirectory,
                            onCreateDirectory = onCreateDirectory,
                            onMoveFile = onMoveFile,
                            onDeleteFile = onDeleteFile,
                            onRefreshDirectory = onRefreshDirectory
                        )
                    }
                    MainTab.TRANSFERS -> {
                        TransfersScreen(
                            state = uiState,
                            onClearTransfers = onClearTransfers
                        )
                    }
                }
            }
        }
    }
}
