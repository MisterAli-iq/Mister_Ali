package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.OfflinePin
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.firebase.configureAppCheck
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
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.RahmaRiadTheme
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeTextSecondary
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.viewmodel.NotificationCountdownState
import com.example.ui.viewmodel.ScreenTab

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()
        setContent {
            val uiState by viewModel.uiState.collectAsState()
            RahmaRiadTheme(
                themeMode = uiState.settings.themeMode,
                accentColor = uiState.settings.accentColor
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    var showSplash by remember { mutableStateOf(true) }

                    if (showSplash) {
                        SplashScreen(
                            onContinue = { showSplash = false }
                        )
                    } else {
                        // Direct immediate entry into the app with NO login or account gate
                        RahmaRiadApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun RahmaRiadApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val activePlayer by viewModel.activePlayer.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasRequestedNotificationPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission handled gracefully */ }

    LaunchedEffect(playerState.currentSong?.id, uiState.settings.newReleasesNotification) {
        if (playerState.currentSong != null &&
            uiState.settings.newReleasesNotification &&
            !hasRequestedNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            hasRequestedNotificationPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.dismissSnackbar()
        }
    }

    // Back handler on secondary screens when player is minimized
    BackHandler(enabled = !playerState.isExpanded && (uiState.currentTab == ScreenTab.NOTIFICATIONS || uiState.currentTab == ScreenTab.ABOUT)) {
        if (uiState.currentTab == ScreenTab.ABOUT) {
            viewModel.selectTab(ScreenTab.SETTINGS)
        } else {
            viewModel.selectTab(ScreenTab.HOME)
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
                if (!playerState.isExpanded) {
                    YouTubeTopBar(
                        unreadNotificationsCount = uiState.unreadNotificationsCount,
                        isGridView = uiState.settings.isGridView,
                        onNotificationClick = { viewModel.selectTab(ScreenTab.NOTIFICATIONS) },
                        onToggleGridView = viewModel::toggleGridView
                    )
                }
            },
            bottomBar = {
                if (!playerState.isExpanded) {
                    Column(
                        modifier = Modifier
                            .background(customTheme.background)
                            .navigationBarsPadding()
                    ) {
                        if (playerState.currentSong != null) {
                            MiniPlayer(
                                song = playerState.currentSong!!,
                                isPlaying = playerState.isPlaying,
                                currentPositionSeconds = playerState.currentPositionSeconds,
                                durationSeconds = playerState.durationSeconds.coerceAtLeast(playerState.currentSong!!.durationSeconds),
                                onExpand = { viewModel.setExpanded(true) },
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onClose = viewModel::closePlayer
                            )
                        }

                        // Clean 5-Item Bottom Navigation Bar: Home, Shorts, Favorites, Library, Settings
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
                                selected = uiState.currentTab == ScreenTab.SHORTS,
                                onClick = { viewModel.selectTab(ScreenTab.SHORTS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == ScreenTab.SHORTS) Icons.Filled.PlayArrow else Icons.Outlined.PlayArrow,
                                        contentDescription = "شورتس"
                                    )
                                },
                                label = { Text("شورتس", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = accentColor,
                                    unselectedIconColor = customTheme.textSecondary,
                                    unselectedTextColor = customTheme.textSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_shorts")
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
                                        contentDescription = "المكتبة"
                                    )
                                },
                                label = { Text("المكتبة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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
                                selected = uiState.currentTab == ScreenTab.SETTINGS,
                                onClick = { viewModel.selectTab(ScreenTab.SETTINGS) },
                                icon = {
                                    Icon(
                                        imageVector = if (uiState.currentTab == ScreenTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = "الإعدادات"
                                    )
                                },
                                label = { Text("الإعدادات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    indicatorColor = accentColor,
                                    unselectedIconColor = customTheme.textSecondary,
                                    unselectedTextColor = customTheme.textSecondary
                                ),
                                modifier = Modifier.testTag("nav_item_settings")
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
                            allSongs = uiState.allSongs,
                            featuredSongs = uiState.featuredSongs,
                            songs = uiState.songs,
                            reels = uiState.reels,
                            isLoadingFeed = uiState.isLoadingFeed,
                            selectedCategory = uiState.selectedCategory,
                            favoriteIds = uiState.favoriteIds,
                            downloadedIds = downloadedSongIds,
                            activeDownloadSongId = uiState.activeDownloadSongId,
                            activeDownloadTitle = uiState.activeDownloadTitle,
                            activeDownloadProgress = uiState.activeDownloadProgress,
                            searchQuery = uiState.searchQuery,
                            isSearchingOnline = uiState.isSearchingOnline,
                            isLoadingMoreChannel = uiState.isLoadingMoreChannel,
                            hasMoreChannelMedia = uiState.hasMoreChannelMedia,
                            isGridView = uiState.settings.isGridView,
                            hideCategoriesBar = uiState.settings.hideCategoriesBar,
                            hiddenCategories = uiState.settings.hiddenCategories,
                            groundedSearchState = uiState.groundedSearchState,
                            onRunGroundedSearch = viewModel::runGroundedSearch,
                            onDismissGroundedSearch = viewModel::dismissGroundedSearch,
                            onSearchQueryChange = viewModel::onSearchQueryChanged,
                            onTriggerSmartSearch = viewModel::triggerSmartSearch,
                            onCategorySelected = viewModel::selectCategory,
                            onSongClick = { song -> viewModel.playSong(song, expandPlayer = true) },
                            onFavoriteToggle = viewModel::toggleFavorite,
                            onToggleFeatured = viewModel::toggleFeatured,
                            onDownloadClick = viewModel::promptDownload,
                            onPlayExternalUrl = { url -> viewModel.playExternalUrl(url, saveAsReel = false, playImmediately = true) },
                            onOpenExternalUrl = viewModel::openUrlInExternalBrowser,
                            onRefreshFeed = viewModel::refreshChannelFeed,
                            onLoadMoreChannel = viewModel::loadMoreChannelMedia,
                            onOpenShorts = { viewModel.selectTab(ScreenTab.SHORTS) }
                        )
                    }

                    ScreenTab.SHORTS -> {
                        ShortsScreen(
                            reels = uiState.reels,
                            preferredQuality = uiState.settings.preferredQuality,
                            videoAspectMode = uiState.settings.videoAspectMode,
                            likedReelIds = uiState.likedIds,
                            onToggleLike = viewModel::toggleReelOrSongLike,
                            onOpenExternalUrl = viewModel::openUrlInExternalBrowser,
                            observeComments = viewModel::observeReelComments,
                            onAddComment = viewModel::addReelComment
                        )
                    }

                    ScreenTab.FAVORITES -> {
                        FavoritesScreen(
                            allSongs = uiState.allSongs,
                            favoriteIds = uiState.favoriteIds,
                            onSongClick = { song -> viewModel.playSong(song, expandPlayer = true) },
                            onRemoveFavorite = viewModel::toggleFavorite,
                            onExploreSongs = { viewModel.selectTab(ScreenTab.HOME) }
                        )
                    }

                    ScreenTab.DOWNLOADS -> {
                        DownloadsScreen(
                            downloads = uiState.downloads,
                            allSongs = uiState.allSongs,
                            localFiles = uiState.localFiles,
                            grantedFolderUris = uiState.settings.grantedLocalFolderUris.toSet(),
                            selectedTopTab = uiState.selectedLibraryTab,
                            onSelectTopTab = viewModel::setLibraryTab,
                            playlists = uiState.playlists,
                            playlistItems = uiState.playlistItems,
                            recentPlayed = uiState.recentPlayed,
                            showLocalFiles = uiState.settings.showLocalDeviceFiles,
                            downloadsStorageSizeFormatted = uiState.downloadsStorageSizeFormatted,
                            downloadStorageLocationTitle = uiState.settings.downloadStorageLocation.titleArabic,
                            onOpenDownloadSettings = { viewModel.selectTab(ScreenTab.SETTINGS) },
                            onPlayOffline = { song -> viewModel.playSong(song, expandPlayer = true) },
                            onPlaySong = { song -> viewModel.playSong(song, expandPlayer = true) },
                            onPlayLocalFile = viewModel::playLocalFile,
                            onDeleteDownload = viewModel::deleteDownload,
                            onDeleteLocalFile = viewModel::deleteLocalFile,
                            onGrantFolderUri = viewModel::onLocalFolderGranted,
                            onRemoveFolderUri = viewModel::removeGrantedLocalFolder,
                            onImportPickedFiles = viewModel::importLocalFiles,
                            onCreatePlaylist = viewModel::createPlaylist,
                            onDeletePlaylist = viewModel::deletePlaylist,
                            onRemoveSongFromPlaylist = viewModel::removeSongFromPlaylist,
                            onPlayPlaylist = viewModel::playPlaylist,
                            onClearRecentHistory = viewModel::clearRecentHistory,
                            onImportLocalFiles = { viewModel.selectTab(ScreenTab.SETTINGS) },
                            onScanDeviceMedia = viewModel::scanDeviceMedia,
                            onGoToHome = { viewModel.selectTab(ScreenTab.HOME) }
                        )
                    }

                    ScreenTab.NOTIFICATIONS -> {
                        NotificationsScreen(
                            notifications = uiState.notifications,
                            onNotificationClick = viewModel::startNotificationCountdown
                        )
                    }

                    ScreenTab.ABOUT -> {
                        AboutScreen(
                            onNavigateToSettings = { viewModel.selectTab(ScreenTab.SETTINGS) }
                        )
                    }

                    ScreenTab.SETTINGS -> {
                        SettingsScreen(
                            settings = uiState.settings,
                            localFiles = uiState.localFiles,
                            downloadsStorageSizeFormatted = uiState.downloadsStorageSizeFormatted,
                            linkPreviewState = uiState.linkPreviewState,
                            isImportingLink = uiState.isImportingLink,
                            onSaveAllSettings = viewModel::applyAllSettings,
                            onResetAllSettings = viewModel::resetAllSettings,
                            onPreviewImportLink = viewModel::previewImportLink,
                            onClearLinkPreview = viewModel::clearLinkPreview,
                            onImportAdminLink = viewModel::importLinkAsAdmin,
                            onAddCustomLinkDomain = viewModel::addCustomLinkDomain,
                            onRemoveCustomLinkDomain = viewModel::removeCustomLinkDomain,
                            onImportLocalFiles = viewModel::importLocalFiles,
                            onDeleteLocalFile = viewModel::deleteLocalFile,
                            onClearAllLocalFiles = viewModel::clearAllLocalFiles,
                            onPlayLocalFile = viewModel::playLocalFile,
                            onOpenLocalMediaHub = viewModel::openLocalMediaTab,
                            onClearAllDownloads = viewModel::clearAllDownloads,
                            onClearCache = viewModel::clearCache,
                            onOpenAbout = { viewModel.selectTab(ScreenTab.ABOUT) },
                            onBack = { viewModel.selectTab(ScreenTab.HOME) }
                        )
                    }
                }
            }
        }

        // Visible 6-Second Notification Countdown Banner (with Cancel & Open Now buttons)
        AnimatedVisibility(
            visible = uiState.notificationCountdown != null,
            enter = slideInVertically(initialOffsetY = { -it }),
            exit = slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            uiState.notificationCountdown?.let { countdown ->
                NotificationCountdownBanner(
                    state = countdown,
                    onOpenNow = viewModel::confirmNotificationCountdownNow,
                    onCancel = viewModel::cancelNotificationCountdown
                )
            }
        }

        // Download Choice Dialog
        if (uiState.songToDownload != null) {
            DownloadDialog(
                song = uiState.songToDownload!!,
                settings = uiState.settings,
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
                    exoPlayer = activePlayer,
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isBuffering,
                    isResolvingStream = playerState.isResolvingStream,
                    playbackError = playerState.playbackError,
                    recoveryStatusMessage = playerState.recoveryStatusMessage,
                    fallbackEmbedUrl = playerState.fallbackEmbedUrl,
                    useWebFallback = playerState.useWebFallback,
                    snifferTargetUrl = playerState.snifferTargetUrl,
                    isFullscreen = playerState.isFullscreen,
                    activeQuality = playerState.activeQualityOverride ?: uiState.settings.preferredQuality,
                    activeEngineMode = playerState.activeEngineMode,
                    isVideoMode = playerState.isVideoMode,
                    currentPositionSeconds = playerState.currentPositionSeconds,
                    durationSeconds = playerState.durationSeconds.coerceAtLeast(activeSong.durationSeconds),
                    playbackSpeed = playerState.playbackSpeed,
                    isLooping = playerState.isLooping,
                    isLyricsExpanded = playerState.isLyricsExpanded,
                    isFavorite = uiState.favoriteIds.contains(activeSong.id),
                    videoAspectMode = uiState.settings.videoAspectMode,
                    playlists = uiState.playlists,
                    upNextSongs = uiState.allSongs,
                    likedIds = uiState.likedIds,
                    commentsFlow = viewModel.observeReelComments(activeSong.id),
                    onClose = { viewModel.setExpanded(false) },
                    onToggleFullscreen = viewModel::toggleFullscreen,
                    onRetryPlayback = viewModel::retryCurrentPlayback,
                    onChangeQuality = viewModel::changeActivePlaybackQuality,
                    onLowerQuality = viewModel::lowerPlaybackQualityOneStep,
                    onClearCacheAndRetry = viewModel::clearPlaybackCacheAndRetry,
                    onSwitchAlternativePlayer = viewModel::switchPlayerEngineMode,
                    onStreamCaptured = viewModel::onSniffedStreamCaptured,
                    onSnifferFailed = viewModel::onSnifferFailed,
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
                    onToggleLike = viewModel::toggleReelOrSongLike,
                    onAddComment = viewModel::addReelComment,
                    onOpenExternalUrl = viewModel::openUrlInExternalBrowser,
                    onToggleFeatured = { viewModel.toggleFeatured(activeSong) },
                    onDownloadClick = { viewModel.promptDownload(activeSong) },
                    onAddToPlaylist = viewModel::addSongToPlaylist,
                    onCreatePlaylist = { name -> viewModel.createPlaylist(name) },
                    onSelectUpNextSong = { nextSong -> viewModel.playSong(nextSong, expandPlayer = true) }
                )
            }
        }
    }
}

@Composable
private fun NotificationCountdownBanner(
    state: NotificationCountdownState,
    onOpenNow: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1F1215)),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, YouTubeGold.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("notification_countdown_banner")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = YouTubeGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.notification.title,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "سيتم التشغيل/الفتح تلقائياً خلال ${state.secondsRemaining} ثوان...",
                        color = YouTubeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { state.secondsRemaining / 6f },
                color = YouTubeRed,
                trackColor = Color(0xFF3A2429),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onOpenNow,
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("countdown_open_now_button")
                ) {
                    Text("فتح الآن", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(0.8.dp, Color.Gray),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("countdown_cancel_button")
                ) {
                    Text("إلغاء", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
