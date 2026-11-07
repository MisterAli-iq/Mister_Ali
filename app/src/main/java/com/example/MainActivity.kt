package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DownloadDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.components.YouTubePlayer
import com.example.ui.components.YouTubeTopBar
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.RahmaRiadTheme
import com.example.ui.theme.YouTubeBlack
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.ScreenTab

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.ui.screens.AuthScreen
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            RahmaRiadTheme(
                themeMode = uiState.settings.themeMode,
                accentColor = uiState.settings.accentColor
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var isGuestMode by remember { mutableStateOf(false) }
                    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

                    DisposableEffect(Unit) {
                        val listener = FirebaseAuth.AuthStateListener { auth ->
                            currentUser = auth.currentUser
                            if (auth.currentUser != null) {
                                isGuestMode = false
                            }
                        }
                        Firebase.auth.addAuthStateListener(listener)
                        onDispose {
                            Firebase.auth.removeAuthStateListener(listener)
                        }
                    }

                    if (currentUser == null && !isGuestMode) {
                        AuthScreen(
                            onAuthSuccess = {
                                currentUser = Firebase.auth.currentUser
                                isGuestMode = false
                            },
                            onSkipSignIn = {
                                isGuestMode = true
                            }
                        )
                    } else {
                        val sessionKey = currentUser?.uid ?: "guest_session"
                        key(sessionKey) {
                            RahmaRiadApp(
                                viewModel = viewModel,
                                currentUser = currentUser,
                                onSignOut = {
                                    currentUser = null
                                    isGuestMode = false
                                },
                                onPromptSignIn = {
                                    isGuestMode = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RahmaRiadApp(
    viewModel: MainViewModel,
    currentUser: FirebaseUser? = null,
    onSignOut: () -> Unit = {},
    onPromptSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    val downloadedSongIds = remember(uiState.downloads) {
        uiState.downloads.map { it.songId }.toSet()
    }

    val customTheme = LocalCustomTheme.current
    val accentColor = Color(uiState.settings.accentColor.hex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(customTheme.background)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = customTheme.background,
            topBar = {
                // TopBar hidden when player is maximized in full screen
                if (!playerState.isExpanded) {
                    YouTubeTopBar(
                        isSearchActive = uiState.isSearchActive,
                        searchQuery = uiState.searchQuery,
                        unreadNotificationsCount = uiState.unreadNotificationsCount,
                        onSearchQueryChange = viewModel::onSearchQueryChanged,
                        onSearchToggle = viewModel::setSearchActive,
                        onNotificationClick = { viewModel.selectTab(ScreenTab.NOTIFICATIONS) },
                        onAboutClick = { viewModel.selectTab(ScreenTab.ABOUT) },
                        onSettingsClick = { viewModel.selectTab(ScreenTab.SETTINGS) }
                    )
                }
            },
            bottomBar = {
                // Bottom bar container with docked mini-player
                if (!playerState.isExpanded) {
                    Column(
                        modifier = Modifier
                            .background(customTheme.background)
                            .navigationBarsPadding()
                    ) {
                        // Docked MiniPlayer
                        if (playerState.currentSong != null) {
                            MiniPlayer(
                                song = playerState.currentSong!!,
                                isPlaying = playerState.isPlaying,
                                currentPositionSeconds = playerState.currentPositionSeconds,
                                durationSeconds = playerState.currentSong!!.durationSeconds,
                                onExpand = { viewModel.setExpanded(true) },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onClose = viewModel::closePlayer
                            )
                        }

                        // YouTube-style Bottom Navigation Bar
                        NavigationBar(
                            containerColor = customTheme.background,
                            contentColor = Color.White,
                            tonalElevation = 0.dp,
                            modifier = Modifier.testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = uiState.currentTab == ScreenTab.HOME,
                                onClick = { viewModel.selectTab(ScreenTab.HOME) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == ScreenTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                        contentDescription = "الرئيسية"
                                    )
                                },
                                label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = accentColor,
                                    unselectedIconColor = customTheme.textSecondary,
                                    unselectedTextColor = customTheme.textSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_home")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == ScreenTab.FAVORITES,
                                onClick = { viewModel.selectTab(ScreenTab.FAVORITES) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == ScreenTab.FAVORITES) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "المفضلة"
                                    )
                                },
                                label = { Text("المفضلة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = YouTubeRed,
                                    unselectedIconColor = YouTubeTextSecondary,
                                    unselectedTextColor = YouTubeTextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_favorites")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == ScreenTab.DOWNLOADS,
                                onClick = { viewModel.selectTab(ScreenTab.DOWNLOADS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == ScreenTab.DOWNLOADS) Icons.Filled.OfflinePin else Icons.Outlined.OfflinePin,
                                        contentDescription = "التنزيلات"
                                    )
                                },
                                label = { Text("التنزيلات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = YouTubeRed,
                                    unselectedIconColor = YouTubeTextSecondary,
                                    unselectedTextColor = YouTubeTextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_downloads")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == ScreenTab.NOTIFICATIONS,
                                onClick = { viewModel.selectTab(ScreenTab.NOTIFICATIONS) },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (uiState.unreadNotificationsCount > 0) {
                                                Badge(containerColor = YouTubeRed, contentColor = Color.White) {
                                                    Text(
                                                        text = if (uiState.unreadNotificationsCount > 9) "9+" else uiState.unreadNotificationsCount.toString(),
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (uiState.currentTab == ScreenTab.NOTIFICATIONS) Icons.Filled.Campaign else Icons.Outlined.Campaign,
                                            contentDescription = "التنبيهات"
                                        )
                                    }
                                },
                                label = { Text("التنبيهات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = YouTubeRed,
                                    unselectedIconColor = YouTubeTextSecondary,
                                    unselectedTextColor = YouTubeTextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_notifications")
                            )

                            NavigationBarItem(
                                selected = uiState.currentTab == ScreenTab.ABOUT,
                                onClick = { viewModel.selectTab(ScreenTab.ABOUT) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == ScreenTab.ABOUT) Icons.Filled.Info else Icons.Outlined.Info,
                                        contentDescription = "المطور"
                                    )
                                },
                                label = { Text("✪Mister Ali", fontSize = 10.sp, fontWeight = FontWeight.Black) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = YouTubeRed,
                                    unselectedIconColor = YouTubeTextSecondary,
                                    unselectedTextColor = YouTubeTextSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_about")
                            )
                        }
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (uiState.currentTab) {
                    ScreenTab.HOME -> {
                        HomeScreen(
                            songs = uiState.songs,
                            selectedCategory = uiState.selectedCategory,
                            favoriteIds = uiState.favoriteIds,
                            downloadedIds = downloadedSongIds,
                            activeDownloadSongId = uiState.activeDownloadSongId,
                            activeDownloadProgress = uiState.activeDownloadProgress,
                            onCategorySelected = viewModel::selectCategory,
                            onSongClick = { song -> viewModel.playSong(song, expandPlayer = true) },
                            onFavoriteToggle = viewModel::toggleFavorite,
                            onDownloadClick = viewModel::promptDownload
                        )
                    }

                    ScreenTab.FAVORITES -> {
                        FavoritesScreen(
                            favoriteIds = uiState.favoriteIds,
                            onSongClick = { song -> viewModel.playSong(song, expandPlayer = true) },
                            onRemoveFavorite = viewModel::toggleFavorite,
                            onExploreSongs = { viewModel.selectTab(ScreenTab.HOME) }
                        )
                    }

                    ScreenTab.DOWNLOADS -> {
                        DownloadsScreen(
                            downloads = uiState.downloads,
                            localFiles = uiState.localFiles,
                            showLocalFiles = uiState.settings.showLocalDeviceFiles,
                            onPlayOffline = { song -> viewModel.playSong(song, expandPlayer = true) },
                            onPlayLocalFile = viewModel::playLocalFile,
                            onDeleteDownload = viewModel::deleteDownload,
                            onDeleteLocalFile = viewModel::deleteLocalFile,
                            onImportLocalFiles = { viewModel.selectTab(ScreenTab.SETTINGS) },
                            onGoToHome = { viewModel.selectTab(ScreenTab.HOME) }
                        )
                    }

                    ScreenTab.NOTIFICATIONS -> {
                        NotificationsScreen(
                            notifications = uiState.notifications,
                            onNotificationClick = viewModel::markNotificationAsRead
                        )
                    }

                    ScreenTab.ABOUT -> {
                        AboutScreen(
                            currentUser = currentUser,
                            onSignOut = onSignOut,
                            onPromptSignIn = onPromptSignIn,
                            onNavigateToSettings = { viewModel.selectTab(ScreenTab.SETTINGS) }
                        )
                    }

                    ScreenTab.SETTINGS -> {
                        SettingsScreen(
                            settings = uiState.settings,
                            localFiles = uiState.localFiles,
                            onThemeModeChange = viewModel::updateThemeMode,
                            onAccentColorChange = viewModel::updateAccentColor,
                            onToggleShowLocalFiles = viewModel::updateShowLocalFiles,
                            onImportLocalFiles = viewModel::importLocalFiles,
                            onDeleteLocalFile = viewModel::deleteLocalFile,
                            onClearAllLocalFiles = viewModel::clearAllLocalFiles,
                            onPlayLocalFile = viewModel::playLocalFile,
                            onAutoPlayNextChange = viewModel::updateAutoPlayNext,
                            onRememberPositionChange = viewModel::updateRememberPosition,
                            onQualityChange = viewModel::updateQuality,
                            onAutoRepeatChange = viewModel::updateAutoRepeat,
                            onLyricsSyncChange = viewModel::updateLyricsSync,
                            onWifiOnlyChange = viewModel::updateWifiOnly,
                            onNotificationsChange = viewModel::updateNotifications,
                            onVibrateChange = viewModel::updateVibrate,
                            onSleepTimerChange = viewModel::updateSleepTimer,
                            onClearCache = viewModel::clearCache,
                            onBack = { viewModel.selectTab(ScreenTab.HOME) }
                        )
                    }
                }
            }
        }

        // Download Choice Dialog (Video vs Audio)
        if (uiState.songToDownload != null) {
            DownloadDialog(
                song = uiState.songToDownload!!,
                onDismiss = viewModel::dismissDownloadPrompt,
                onConfirmDownload = viewModel::startDownload
            )
        }

        // Full Screen YouTube Player Overlay
        AnimatedVisibility(
            visible = playerState.isExpanded && playerState.currentSong != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            playerState.currentSong?.let { activeSong ->
                YouTubePlayer(
                    song = activeSong,
                    isPlaying = playerState.isPlaying,
                    isVideoMode = playerState.isVideoMode,
                    currentPositionSeconds = playerState.currentPositionSeconds,
                    durationSeconds = activeSong.durationSeconds,
                    playbackSpeed = playerState.playbackSpeed,
                    isLooping = playerState.isLooping,
                    isLyricsExpanded = playerState.isLyricsExpanded,
                    isFavorite = uiState.favoriteIds.contains(activeSong.id),
                    onClose = { viewModel.setExpanded(false) },
                    onTogglePlayPause = viewModel::togglePlayPause,
                    onSeekTo = viewModel::seekTo,
                    onSkipForward = viewModel::skipForward10,
                    onSkipBackward = viewModel::skipBackward10,
                    onPlayNext = viewModel::playNextSong,
                    onPlayPrevious = viewModel::playPreviousSong,
                    onToggleVideoAudio = viewModel::toggleVideoAudioMode,
                    onCycleSpeed = viewModel::cyclePlaybackSpeed,
                    onToggleLoop = viewModel::toggleLoop,
                    onToggleLyrics = viewModel::toggleLyricsExpanded,
                    onFavoriteToggle = { viewModel.toggleFavorite(activeSong) },
                    onDownloadClick = { viewModel.promptDownload(activeSong) },
                    onSelectUpNextSong = { nextSong -> viewModel.playSong(nextSong, expandPlayer = true) }
                )
            }
        }
    }
}
