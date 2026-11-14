package com.example.ui.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.RahmaCatalog
import com.example.data.RahmaRepository
import com.example.data.SettingsManager
import com.example.media.MediaSessionMetadataFactory
import com.example.media.PlaybackHeaderRegistry
import com.example.media.RahmaPlaybackService
import com.google.common.util.concurrent.ListenableFuture
import com.example.data.firebase.FirebaseRepository
import com.example.data.local.AppDatabase
import com.example.data.local.ImportedMediaEntity
import com.example.data.local.LocalFileEntity
import com.example.data.local.LocalMediaScanner
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.data.remote.GroundedSearchUiState
import com.example.data.remote.LinkCheckStatus
import com.example.data.remote.LinkDiagnosticResult
import com.example.data.remote.LinkSourceDetection
import com.example.data.remote.LinkValidator
import com.example.data.remote.MediaStreamResolver
import com.example.data.remote.SearchGroundingService
import com.example.model.AppAccentColor
import com.example.model.AppNotification
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.DownloadStorageLocation
import com.example.model.DownloadType
import com.example.model.DownloadedMedia
import com.example.model.LinkSourceConfig
import com.example.model.LinkSourceType
import com.example.model.LocalMediaType
import com.example.model.PlayerEngineMode
import com.example.model.PreferredQuality
import com.example.model.RahmaReel
import com.example.model.ReelComment
import com.example.model.Song
import com.example.model.SongCategory
import com.example.model.VideoAspectMode
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.UUID

enum class ScreenTab {
    HOME,
    SHORTS,
    FAVORITES,
    DOWNLOADS,
    NOTIFICATIONS,
    ABOUT,
    SETTINGS
}

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isResolvingStream: Boolean = false,
    val playbackError: String? = null,
    val recoveryStatusMessage: String? = null,
    val fallbackEmbedUrl: String? = null,
    val snifferTargetUrl: String? = null,
    val useWebFallback: Boolean = false,
    val activeEngineMode: PlayerEngineMode = PlayerEngineMode.EXOPLAYER,
    val activeQualityOverride: PreferredQuality? = null,
    val retryAttempt: Int = 0,
    val isVideoMode: Boolean = true,
    val isFullscreen: Boolean = false,
    val currentPositionSeconds: Int = 0,
    val durationSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val isExpanded: Boolean = false,
    val isLyricsExpanded: Boolean = false
)

sealed interface LinkPreviewState {
    data object Idle : LinkPreviewState
    data class Loading(val url: String) : LinkPreviewState
    data class Ready(
        val correctedUrl: String,
        val title: String,
        val thumbnailUrl: String?,
        val platformLabel: String,
        val channelName: String,
        val suggestedCategory: SongCategory,
        val suggestedMediaType: LocalMediaType,
        val isWebOnly: Boolean,
        val isDuplicate: Boolean
    ) : LinkPreviewState

    data class Error(val message: String) : LinkPreviewState
}

data class NotificationCountdownState(
    val notification: AppNotification,
    val secondsRemaining: Int
)

data class UiState(
    val currentTab: ScreenTab = ScreenTab.HOME,
    val selectedLibraryTab: Int = 0,
    val selectedCategory: SongCategory = SongCategory.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val isSearchingOnline: Boolean = false,
    val groundedSearchState: GroundedSearchUiState = GroundedSearchUiState.Idle,
    val allSongs: List<Song> = emptyList(),
    val featuredSongs: List<Song> = emptyList(),
    val smartSearchResults: List<Song> = emptyList(),
    val songs: List<Song> = emptyList(),
    val reels: List<RahmaReel> = emptyList(),
    val isLoadingFeed: Boolean = true,
    val isLoadingMoreChannel: Boolean = false,
    val hasMoreChannelMedia: Boolean = true,
    val isImportingLink: Boolean = false,
    val linkCheckState: LinkDiagnosticResult = LinkDiagnosticResult(),
    val linkPreviewState: LinkPreviewState = LinkPreviewState.Idle,
    val favoriteIds: Set<String> = emptySet(),
    val likedIds: Set<String> = emptySet(),
    val downloads: List<DownloadedMedia> = emptyList(),
    val downloadsStorageSizeFormatted: String = "0 MB",
    val localFiles: List<LocalFileEntity> = emptyList(),
    val playlists: List<PlaylistEntity> = emptyList(),
    val playlistItems: List<PlaylistItemEntity> = emptyList(),
    val recentPlayed: List<Song> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val notifications: List<AppNotification> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val notificationCountdown: NotificationCountdownState? = null,
    val activeDownloadSongId: String? = null,
    val activeDownloadTitle: String? = null,
    val activeDownloadProgress: Int = 0,
    val songToDownload: Song? = null,
    val snackbarMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RahmaRepository
    private val firebaseRepository: FirebaseRepository = FirebaseRepository(application)
    private val settingsManager: SettingsManager = SettingsManager(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState.asStateFlow()

    private val _activePlayer = MutableStateFlow<Player?>(null)
    val activePlayer: StateFlow<Player?> = _activePlayer.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var sessionPlayerListener: Player.Listener? = null
    private val fallbackArtworkBytes: ByteArray? by lazy {
        MediaSessionMetadataFactory.loadFallbackArtworkBytes(application)
    }

    private var positionSyncJob: Job? = null
    private var streamResolutionJob: Job? = null
    private var bufferingWatchdogJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var smartSearchJob: Job? = null
    private var groundedSearchJob: Job? = null
    private var localRefreshJob: Job? = null
    private var notificationCountdownJob: Job? = null
    private var mediaContentObserver: ContentObserver? = null
    private var nextChannelContinuationToken: String? = null
    private var snifferAttemptedForSongId: String? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RahmaRepository(application, database.appDao())

        connectMediaController()
        startPositionSyncLoop()

        // Fetch unlimited official @RahmaRiad channel media on startup
        refreshChannelFeed()

        // Observe live channel media + imported media + featured prefs -> drive Home, Featured & Reels feeds
        viewModelScope.launch {
            combine(
                repository.getCachedChannelMedia(),
                repository.getImportedSongs(),
                repository.getImportedReels(),
                repository.getFeaturedPreferences()
            ) { channelSongs, importedSongs, importedReels, featuredPrefs ->
                var autoFeaturedCount = 0
                val rawCombined = (importedSongs + channelSongs).sortedByDescending { song ->
                    when {
                        RahmaCatalog.isOfficialRahmaChannel(song) -> 2
                        song.isUserImported -> 1
                        else -> 0
                    }
                }
                val combinedSongs = rawCombined.map { song ->
                    val explicitPref = featuredPrefs[song.id]
                    val isFeat = explicitPref ?: if (!song.isShort && !song.isUserImported && RahmaCatalog.isOfficialRahmaChannel(song) && autoFeaturedCount < 6) {
                        autoFeaturedCount++
                        true
                    } else {
                        false
                    }
                    song.copy(isFeatured = isFeat)
                }
                val featuredOnly = combinedSongs.filter { it.isFeatured }
                val channelShortsAsReels = channelSongs
                    .filter { it.isShort }
                    .map { s ->
                        val isIg = s.channelHandle.equals(RahmaCatalog.OFFICIAL_INSTAGRAM_HANDLE, ignoreCase = true) ||
                            s.externalUrl?.contains("instagram.com", ignoreCase = true) == true
                        RahmaReel(
                            id = s.id,
                            title = s.arabicTitle,
                            caption = s.description.ifBlank { s.arabicTitle },
                            sourceUrl = s.externalUrl ?: "",
                            youtubeVideoId = s.youtubeVideoId,
                            thumbnailUrl = s.thumbnailUrl,
                            platformLabel = if (isIg) "انستغرام • @rahmariadh" else s.sourcePlatform,
                            songTitle = s.arabicTitle,
                            dateText = s.publishDate,
                            isUserImported = false,
                            tags = if (isIg) listOf("رحمة_رياض", "انستغرام", "ريلز") else listOf("رحمة_رياض", "ريلز")
                        )
                    }
                val combinedReels = importedReels + channelShortsAsReels
                listOf(combinedSongs, featuredOnly, combinedReels)
            }.collectLatest { results ->
                @Suppress("UNCHECKED_CAST")
                val combinedSongs = results[0] as List<Song>
                @Suppress("UNCHECKED_CAST")
                val featuredOnly = results[1] as List<Song>
                @Suppress("UNCHECKED_CAST")
                val combinedReels = results[2] as List<RahmaReel>
                _uiState.update { current ->
                    current.copy(
                        allSongs = combinedSongs,
                        featuredSongs = featuredOnly,
                        songs = applyFilterAndSearch(
                            allSongs = combinedSongs,
                            smartRemoteSongs = current.smartSearchResults,
                            category = current.selectedCategory,
                            query = current.searchQuery
                        ),
                        reels = combinedReels
                    )
                }
            }
        }

        // Collect favorites from Room
        viewModelScope.launch {
            repository.getFavoriteSongIds().collectLatest { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
            }
        }

        // Collect persistent likes for Reels and Player
        viewModelScope.launch {
            repository.getLikedReelIds().collectLatest { ids ->
                _uiState.update { it.copy(likedIds = ids) }
            }
        }

        // Sync with Firebase Firestore if user is signed in
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            viewModelScope.launch {
                try {
                    firebaseRepository.syncUserProfile(
                        userId = currentUser.uid,
                        displayName = currentUser.displayName ?: "مستخدم رحمة رياض",
                        email = currentUser.email ?: "",
                        photoUrl = currentUser.photoUrl?.toString()
                    )
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Failed to sync user profile: ${e.message}")
                }
            }

            viewModelScope.launch {
                firebaseRepository.observeFavorites(currentUser.uid)
                    .catch { error ->
                        Log.w("MainViewModel", "Failed observing cloud favorites: ${error.message}")
                    }
                    .collectLatest { cloudFavorites ->
                        for (fav in cloudFavorites) {
                            repository.toggleFavorite(fav.songId, isCurrentlyFavorite = false)
                        }
                    }
            }
        }

        // Collect downloads & storage stats
        viewModelScope.launch {
            repository.getAllDownloads().collectLatest { downloadsList ->
                _uiState.update {
                    it.copy(
                        downloads = downloadsList,
                        downloadsStorageSizeFormatted = repository.calculateDownloadsStorageFormatted()
                    )
                }
            }
        }

        // Collect notifications
        viewModelScope.launch {
            repository.getNotifications().collectLatest { list ->
                _uiState.update { it.copy(notifications = list) }
            }
        }

        // Collect unread count
        viewModelScope.launch {
            repository.getUnreadCount().collectLatest { count ->
                _uiState.update { it.copy(unreadNotificationsCount = count) }
            }
        }

        // Collect settings and sync with ExoPlayer & local media observers
        viewModelScope.launch {
            settingsManager.settings.collectLatest { s ->
                _uiState.update { current ->
                    current.copy(
                        settings = s,
                        songs = applyFilterAndSearch(
                            allSongs = current.allSongs,
                            smartRemoteSongs = current.smartSearchResults,
                            category = current.selectedCategory,
                            query = current.searchQuery,
                            settings = s
                        )
                    )
                }
                applyExoPlayerSettings(s)
                registerLocalMediaObservers(s.grantedLocalFolderUris.toSet())
            }
        }

        // Collect local files
        viewModelScope.launch {
            repository.getAllLocalFiles().collectLatest { files ->
                _uiState.update { it.copy(localFiles = files) }
                if (files.isEmpty() && settingsManager.settings.value.showLocalDeviceFiles) {
                    val scanned = LocalMediaScanner.scanAllDeviceMedia(
                        context = application,
                        grantedFolderUris = settingsManager.settings.value.grantedLocalFolderUris.toSet()
                    )
                    if (scanned.isNotEmpty()) {
                        repository.saveLocalFiles(scanned)
                    }
                }
            }
        }

        // Collect custom playlists
        viewModelScope.launch {
            repository.getAllPlaylists().collectLatest { playlistsList ->
                _uiState.update { it.copy(playlists = playlistsList) }
            }
        }

        // Collect playlist items
        viewModelScope.launch {
            repository.getAllPlaylistItems().collectLatest { itemsList ->
                _uiState.update { it.copy(playlistItems = itemsList) }
            }
        }

        // Collect recent played songs
        viewModelScope.launch {
            combine(
                repository.getRecentPlayed(),
                _uiState
            ) { recentList, state ->
                recentList.mapNotNull { recentEntity ->
                    state.allSongs.find { it.id == recentEntity.songId }
                }
            }.collectLatest { mappedSongs ->
                _uiState.update { it.copy(recentPlayed = mappedSongs) }
            }
        }
    }

    private fun connectMediaController() {
        val context = getApplication<Application>()
        val sessionToken = SessionToken(context, ComponentName(context, RahmaPlaybackService::class.java))
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future
        future.addListener({
            runCatching {
                val controller = future.get()
                _activePlayer.value = controller
                attachPlayerListener(controller)
                applyExoPlayerSettings(_uiState.value.settings)
            }.onFailure { err ->
                Log.w("MainViewModel", "Failed connecting MediaController to RahmaPlaybackService: ${err.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private suspend fun awaitActivePlayer(): Player? {
        _activePlayer.value?.let { return it }
        if (controllerFuture == null) {
            connectMediaController()
        }
        return withTimeoutOrNull(4_000L) {
            while (_activePlayer.value == null) {
                delay(25)
            }
            _activePlayer.value
        }
    }

    private inline fun withActivePlayer(action: Player.() -> Unit) {
        _activePlayer.value?.action()
    }

    private fun applyExoPlayerSettings(settings: AppSettings) {
        withActivePlayer {
            repeatMode = if (settings.autoRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
        val effectiveQuality = _playerState.value.activeQualityOverride ?: settings.preferredQuality
        applyQualityTrackParameters(effectiveQuality)
        _playerState.update { it.copy(isLooping = settings.autoRepeat) }
    }

    private fun applyQualityTrackParameters(quality: PreferredQuality) {
        val trackParams = if (quality == PreferredQuality.AUTO) {
            TrackSelectionParameters.Builder(getApplication())
                .clearVideoSizeConstraints()
                .build()
        } else {
            TrackSelectionParameters.Builder(getApplication())
                .setMaxVideoSize(Int.MAX_VALUE, quality.maxHeightPx)
                .build()
        }
        withActivePlayer {
            trackSelectionParameters = trackParams
        }
    }

    private fun isWeakNetwork(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return true
            val caps = cm.getNetworkCapabilities(network) ?: return true
            val downKbps = caps.linkDownstreamBandwidthKbps
            !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) || (downKbps in 1..1499)
        } catch (_: Exception) {
            false
        }
    }

    private fun attachPlayerListener(player: Player) {
        sessionPlayerListener?.let { old -> player.removeListener(old) }
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _playerState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val durSec = if (player.duration > 0) (player.duration / 1000).toInt() else 0
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_ENDED || playbackState == Player.STATE_IDLE) {
                    bufferingWatchdogJob?.cancel()
                }
                if (playbackState == Player.STATE_BUFFERING &&
                    !_playerState.value.useWebFallback &&
                    !_playerState.value.isResolvingStream &&
                    _playerState.value.snifferTargetUrl == null
                ) {
                    startBufferingWatchdog()
                }
                _playerState.update {
                    it.copy(
                        isBuffering = playbackState == Player.STATE_BUFFERING,
                        durationSeconds = if (durSec > 0) durSec else it.durationSeconds,
                        recoveryStatusMessage = if (playbackState == Player.STATE_READY) null else it.recoveryStatusMessage,
                        retryAttempt = if (playbackState == Player.STATE_READY) 0 else it.retryAttempt,
                        playbackError = if (playbackState == Player.STATE_READY) null else it.playbackError
                    )
                }
                if (playbackState == Player.STATE_ENDED) {
                    _playerState.value.currentSong?.let { song ->
                        settingsManager.savePlaybackPosition(song.id, 0)
                    }
                    if (_uiState.value.settings.autoPlayNext && !_playerState.value.isLooping) {
                        playNextSong()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                bufferingWatchdogJob?.cancel()
                Log.w("MainViewModel", "ExoPlayer playback error: ${error.errorCodeName} - ${error.message}")
                val state = _playerState.value
                val song = state.currentSong
                if (song != null) {
                    val src = song.externalUrl ?: if (song.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${song.youtubeVideoId}" else ""
                    MediaStreamResolver.invalidateCachedStream(src, song.youtubeVideoId)
                }
                val currentQuality = state.activeQualityOverride ?: _uiState.value.settings.preferredQuality
                val nextQuality = nextLowerQuality(currentQuality)

                // 1. Automatic retry with progressive quality step-down before giving up
                if (song != null && !state.useWebFallback && state.retryAttempt < 2 &&
                    !song.id.startsWith("local_")
                ) {
                    val nextAttempt = state.retryAttempt + 1
                    viewModelScope.launch {
                        _playerState.update {
                            it.copy(
                                isBuffering = true,
                                playbackError = null,
                                retryAttempt = nextAttempt,
                                activeQualityOverride = nextQuality,
                                recoveryStatusMessage = "جارٍ التحويل التلقائي لجودة ${nextQuality.titleArabic}..."
                            )
                        }
                        delay(400)
                        playSongInternal(
                            song = song.copy(resolvedStreamUrl = null),
                            expandPlayer = _playerState.value.isExpanded,
                            qualityOverride = nextQuality,
                            retryAttempt = nextAttempt,
                            engineModeOverride = PlayerEngineMode.EXOPLAYER,
                            bypassCache = true
                        )
                    }
                    return
                }

                // 2. Hidden WebView stream capture backup before showing Retry UI
                val snifferCandidate = MediaStreamResolver.buildPageSnifferUrl(
                    rawUrl = song?.externalUrl.orEmpty(),
                    videoIdHint = song?.youtubeVideoId.orEmpty()
                ) ?: state.fallbackEmbedUrl
                    ?: song?.externalUrl?.takeIf { it.startsWith("http", ignoreCase = true) && !MediaStreamResolver.isDirectMediaUri(it) }
                if (song != null && !snifferCandidate.isNullOrBlank() && snifferAttemptedForSongId != song.id) {
                    snifferAttemptedForSongId = song.id
                    withActivePlayer {
                        stop()
                        clearMediaItems()
                    }
                    _playerState.update {
                        it.copy(
                            isBuffering = false,
                            isResolvingStream = true,
                            isPlaying = false,
                            snifferTargetUrl = snifferCandidate,
                            useWebFallback = false,
                            recoveryStatusMessage = "جارٍ استخراج رابط البث المباشر...",
                            playbackError = null
                        )
                    }
                    return
                }

                _playerState.update {
                    it.copy(
                        isBuffering = false,
                        isResolvingStream = false,
                        isPlaying = false,
                        snifferTargetUrl = null,
                        recoveryStatusMessage = null,
                        playbackError = mapPlaybackErrorToArabic(error)
                    )
                }
            }
        }
        sessionPlayerListener = listener
        player.addListener(listener)
    }

    private fun startBufferingWatchdog() {
        bufferingWatchdogJob?.cancel()
        bufferingWatchdogJob = viewModelScope.launch {
            delay(MediaStreamResolver.MAX_RESOLUTION_TIMEOUT_MS)
            val state = _playerState.value
            val song = state.currentSong ?: return@launch
            val player = _activePlayer.value
            if (player?.playbackState == Player.STATE_BUFFERING && !state.useWebFallback && state.playbackError == null) {
                val src = song.externalUrl ?: if (song.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${song.youtubeVideoId}" else ""
                MediaStreamResolver.invalidateCachedStream(src, song.youtubeVideoId)
                val currentQuality = state.activeQualityOverride ?: _uiState.value.settings.preferredQuality
                val nextQuality = nextLowerQuality(currentQuality)
                if (!song.id.startsWith("local_") && state.retryAttempt < 2 && currentQuality != PreferredQuality.DATA_SAVER) {
                    val nextAttempt = state.retryAttempt + 1
                    playSongInternal(
                        song = song.copy(resolvedStreamUrl = null),
                        expandPlayer = state.isExpanded,
                        qualityOverride = nextQuality,
                        retryAttempt = nextAttempt,
                        engineModeOverride = PlayerEngineMode.EXOPLAYER,
                        bypassCache = true
                    )
                } else {
                    val snifferCandidate = MediaStreamResolver.buildPageSnifferUrl(
                        rawUrl = song.externalUrl.orEmpty(),
                        videoIdHint = song.youtubeVideoId
                    ) ?: state.fallbackEmbedUrl
                        ?: song.externalUrl?.takeIf { it.startsWith("http", ignoreCase = true) && !MediaStreamResolver.isDirectMediaUri(it) }
                    if (!snifferCandidate.isNullOrBlank() && snifferAttemptedForSongId != song.id) {
                        snifferAttemptedForSongId = song.id
                        player.stop()
                        player.clearMediaItems()
                        _playerState.update {
                            it.copy(
                                isBuffering = false,
                                isResolvingStream = true,
                                snifferTargetUrl = snifferCandidate,
                                recoveryStatusMessage = "جارٍ استخراج رابط البث المباشر...",
                                playbackError = null
                            )
                        }
                    } else {
                        player.stop()
                        player.clearMediaItems()
                        _playerState.update {
                            it.copy(
                                isBuffering = false,
                                isResolvingStream = false,
                                isPlaying = false,
                                snifferTargetUrl = null,
                                recoveryStatusMessage = null,
                                playbackError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                            )
                        }
                    }
                }
            }
        }
    }

    private fun startPositionSyncLoop() {
        positionSyncJob?.cancel()
        var persistTick = 0
        positionSyncJob = viewModelScope.launch {
            while (isActive) {
                delay(500)
                val player = _activePlayer.value
                if (player != null && (player.isPlaying || player.playbackState == Player.STATE_READY)) {
                    val posSec = (player.currentPosition / 1000).toInt().coerceAtLeast(0)
                    val durSec = if (player.duration > 0) (player.duration / 1000).toInt() else _playerState.value.durationSeconds
                    _playerState.update {
                        it.copy(
                            currentPositionSeconds = posSec,
                            durationSeconds = durSec
                        )
                    }
                    persistTick++
                    if (persistTick % 4 == 0 && posSec > 2) {
                        _playerState.value.currentSong?.let { song ->
                            settingsManager.savePlaybackPosition(song.id, posSec)
                        }
                    }
                }
            }
        }
    }

    fun refreshChannelFeed() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFeed = true) }
            try {
                val page = repository.syncOfficialChannelPage(continuationToken = null)
                nextChannelContinuationToken = page.nextContinuationToken
                _uiState.update {
                    it.copy(hasMoreChannelMedia = !page.nextContinuationToken.isNullOrBlank())
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Feed sync error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoadingFeed = false) }
            }
        }
    }

    fun loadMoreChannelMedia() {
        val token = nextChannelContinuationToken
        if (_uiState.value.isLoadingMoreChannel) return
        if (token.isNullOrBlank()) {
            _uiState.update { it.copy(hasMoreChannelMedia = false) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreChannel = true) }
            try {
                val page = repository.syncOfficialChannelPage(token)
                nextChannelContinuationToken = page.nextContinuationToken
                _uiState.update {
                    it.copy(hasMoreChannelMedia = !page.nextContinuationToken.isNullOrBlank())
                }
                if (page.items.isNotEmpty()) {
                    showSnackbar("تم جلب ${page.items.size} عملاً إضافياً من قناة @RahmaRiad ✨")
                }
            } catch (e: Exception) {
                Log.w("MainViewModel", "Load more error: ${e.message}")
            } finally {
                _uiState.update { it.copy(isLoadingMoreChannel = false) }
            }
        }
    }

    private fun applyFilterAndSearch(
        allSongs: List<Song>,
        smartRemoteSongs: List<Song>,
        category: SongCategory,
        query: String,
        settings: AppSettings = _uiState.value.settings
    ): List<Song> {
        val catFiltered = RahmaCatalog.filterByCategory(allSongs, category)
        if (query.isBlank()) {
            return catFiltered
        }
        val scopedLocal = RahmaCatalog.filterByEnabledSources(catFiltered, settings)
        val remoteFiltered = if (category == SongCategory.ALL) {
            RahmaCatalog.filterByEnabledSources(smartRemoteSongs, settings)
        } else {
            emptyList()
        }
        return RahmaCatalog.mergeAndRankSmartSearch(scopedLocal, remoteFiltered, query)
    }

    fun selectTab(tab: ScreenTab) {
        if (tab == ScreenTab.SHORTS && _activePlayer.value?.isPlaying == true) {
            withActivePlayer { pause() }
        }
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun setLibraryTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedLibraryTab = tabIndex.coerceIn(0, 3)) }
    }

    fun openLocalMediaTab() {
        if (!_uiState.value.settings.showLocalDeviceFiles) {
            settingsManager.updateShowLocalFiles(true)
        }
        if (_activePlayer.value?.isPlaying == true && _uiState.value.currentTab == ScreenTab.SHORTS) {
            withActivePlayer { pause() }
        }
        _uiState.update {
            it.copy(
                currentTab = ScreenTab.DOWNLOADS,
                selectedLibraryTab = 3
            )
        }
        scanDeviceMedia(silent = true)
    }

    fun selectCategory(category: SongCategory) {
        _uiState.update { current ->
            current.copy(
                selectedCategory = category,
                songs = applyFilterAndSearch(current.allSongs, current.smartSearchResults, category, current.searchQuery, current.settings)
            )
        }
    }

    /**
     * Manual search input: updates typed text ONLY without triggering automatic network/AI searches while typing.
     */
    fun onSearchQueryChanged(query: String) {
        if (query.isBlank()) {
            smartSearchJob?.cancel()
            groundedSearchJob?.cancel()
            _uiState.update { current ->
                current.copy(
                    searchQuery = "",
                    isSearchingOnline = false,
                    smartSearchResults = emptyList(),
                    groundedSearchState = GroundedSearchUiState.Idle,
                    songs = applyFilterAndSearch(current.allSongs, emptyList(), current.selectedCategory, "", current.settings)
                )
            }
        } else {
            _uiState.update { current ->
                current.copy(searchQuery = query)
            }
        }
    }

    /**
     * Triggered explicitly when user taps the Search button or IME Search action.
     * Searches using fuzzy/close Arabic matching inside the sources enabled in Settings.
     */
    fun triggerSmartSearch(query: String = _uiState.value.searchQuery) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _uiState.update { current ->
                current.copy(
                    searchQuery = "",
                    isSearchingOnline = false,
                    smartSearchResults = emptyList(),
                    songs = applyFilterAndSearch(current.allSongs, emptyList(), current.selectedCategory, "", current.settings)
                )
            }
            return
        }
        smartSearchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = trimmed,
                selectedCategory = SongCategory.ALL,
                isSearchingOnline = true,
                songs = applyFilterAndSearch(it.allSongs, it.smartSearchResults, SongCategory.ALL, trimmed, it.settings)
            )
        }
        smartSearchJob = viewModelScope.launch {
            executeSmartSearch(trimmed)
        }
    }

    fun runGroundedSearch(query: String = _uiState.value.searchQuery) {
        val targetQuery = query.trim().ifBlank { "أحدث أغاني وحفلات وأخبار رحمة رياض @RahmaRiad" }
        val currentSettings = _uiState.value.settings
        val enabledTypes = LinkSourceType.twelvePlatforms.filter { currentSettings.configFor(it).enabled }
        groundedSearchJob?.cancel()
        groundedSearchJob = viewModelScope.launch {
            SearchGroundingService.streamGroundedSearch(
                rawQuery = targetQuery,
                enabledSources = enabledTypes
            ).collect { state ->
                _uiState.update { it.copy(groundedSearchState = state) }
            }
        }
    }

    fun dismissGroundedSearch() {
        groundedSearchJob?.cancel()
        _uiState.update { it.copy(groundedSearchState = GroundedSearchUiState.Idle) }
    }

    private suspend fun executeSmartSearch(query: String) {
        try {
            val currentSettings = _uiState.value.settings
            val remoteResults = repository.smartSearchOnline(query)
            val scopedRemote = RahmaCatalog.filterByEnabledSources(remoteResults, currentSettings)
            _uiState.update { current ->
                if (current.searchQuery.trim() != query) {
                    current.copy(isSearchingOnline = false)
                } else {
                    current.copy(
                        isSearchingOnline = false,
                        smartSearchResults = scopedRemote,
                        songs = applyFilterAndSearch(
                            allSongs = current.allSongs,
                            smartRemoteSongs = scopedRemote,
                            category = current.selectedCategory,
                            query = current.searchQuery,
                            settings = current.settings
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w("MainViewModel", "Smart search failed: ${e.message}")
            _uiState.update { it.copy(isSearchingOnline = false) }
        }
    }

    fun setSearchActive(active: Boolean) {
        smartSearchJob?.cancel()
        if (!active) {
            groundedSearchJob?.cancel()
        }
        _uiState.update {
            if (!active) {
                it.copy(
                    isSearchActive = false,
                    isSearchingOnline = false,
                    searchQuery = "",
                    smartSearchResults = emptyList(),
                    groundedSearchState = GroundedSearchUiState.Idle,
                    songs = applyFilterAndSearch(it.allSongs, emptyList(), it.selectedCategory, "", it.settings)
                )
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    // Native ExoPlayer Playback Actions + 10s Timeout + Auto Quality Step-Down + Verified Direct Streams
    fun playSong(song: Song, expandPlayer: Boolean = true) {
        val rawUrl = song.externalUrl?.let { LinkValidator.unwrapRedirectWrapperUrl(it) }.orEmpty()
        val isLocal = song.id.startsWith("local_") || rawUrl.startsWith("content://") || rawUrl.startsWith("file://")
        if (!isLocal &&
            rawUrl.startsWith("http", ignoreCase = true) &&
            song.youtubeVideoId.isBlank() &&
            !MediaStreamResolver.isDirectMediaUri(rawUrl) &&
            LinkValidator.isWebOnlyNonPlayableUrl(rawUrl)
        ) {
            openUrlInExternalBrowser(rawUrl)
            return
        }
        snifferAttemptedForSongId = null
        playSongInternal(
            song = song,
            expandPlayer = expandPlayer,
            qualityOverride = null,
            retryAttempt = 0,
            engineModeOverride = PlayerEngineMode.EXOPLAYER,
            bypassCache = false
        )
    }

    private fun playSongInternal(
        song: Song,
        expandPlayer: Boolean,
        qualityOverride: PreferredQuality?,
        retryAttempt: Int,
        engineModeOverride: PlayerEngineMode,
        bypassCache: Boolean = false
    ) {
        streamResolutionJob?.cancel()
        bufferingWatchdogJob?.cancel()
        withActivePlayer {
            stop()
            clearMediaItems()
        }

        val currentSettings = _uiState.value.settings
        val resumeSeconds = if (currentSettings.rememberPlaybackPosition) {
            settingsManager.getPlaybackPosition(song.id)
        } else 0

        val rawExternal = song.externalUrl?.let { LinkValidator.unwrapRedirectWrapperUrl(it) }
        val extractedYtId = song.youtubeVideoId.ifBlank {
            rawExternal?.let { MediaStreamResolver.extractYouTubeId(it) }.orEmpty()
        }
        val sourceUrl = rawExternal
            ?: if (extractedYtId.isNotBlank()) "https://www.youtube.com/watch?v=$extractedYtId" else ""

        // Pick quality respecting per-source settings & auto weak-network protection
        val sourceDetection = if (sourceUrl.startsWith("http", ignoreCase = true)) {
            LinkValidator.detectSource(sourceUrl, currentSettings.customLinkDomains)
        } else null
        val sourceQuality = sourceDetection?.let {
            LinkValidator.getSourceConfig(it, currentSettings).quality
        }
        val baseQuality = qualityOverride ?: sourceQuality ?: currentSettings.preferredQuality
        val weakNet = isWeakNetwork()
        val effectiveQuality = if (weakNet && (baseQuality == PreferredQuality.AUTO || baseQuality == PreferredQuality.HIGH) &&
            !song.id.startsWith("local_")
        ) {
            PreferredQuality.DATA_SAVER
        } else {
            baseQuality
        }
        applyQualityTrackParameters(effectiveQuality)

        val initialFallback = if (song.youtubeVideoId.isNotBlank()) {
            MediaStreamResolver.buildSilentYouTubeEmbedUrl(song.youtubeVideoId)
        } else {
            song.externalUrl?.takeIf { it.startsWith("http", ignoreCase = true) }
        }

        val isSameSong = _playerState.value.currentSong?.id == song.id
        val nextVideoMode = if (isSameSong) _playerState.value.isVideoMode else currentSettings.defaultVideoMode
        val defaultSpeed = currentSettings.defaultPlaybackSpeed
        withActivePlayer { setPlaybackSpeed(defaultSpeed) }

        val weakNetworkHint = if (weakNet && qualityOverride == null && !song.id.startsWith("local_")) {
            "شبكة ضعيفة: تم تقليل الجودة تلقائياً إلى 360p لضمان استمرار التشغيل"
        } else if (retryAttempt > 0) {
            "جارٍ إعادة المحاولة بجودة ${effectiveQuality.titleArabic}..."
        } else {
            null
        }

        if (engineModeOverride == PlayerEngineMode.WEB_ALTERNATIVE && !initialFallback.isNullOrBlank()) {
            _playerState.update {
                it.copy(
                    currentSong = song,
                    isPlaying = true,
                    isBuffering = false,
                    isResolvingStream = false,
                    playbackError = null,
                    recoveryStatusMessage = null,
                    fallbackEmbedUrl = initialFallback,
                    snifferTargetUrl = null,
                    useWebFallback = true,
                    activeEngineMode = PlayerEngineMode.WEB_ALTERNATIVE,
                    activeQualityOverride = effectiveQuality,
                    retryAttempt = retryAttempt,
                    isVideoMode = nextVideoMode,
                    playbackSpeed = defaultSpeed,
                    isLooping = currentSettings.autoRepeat,
                    currentPositionSeconds = resumeSeconds,
                    durationSeconds = song.durationSeconds,
                    isExpanded = expandPlayer
                )
            }
            return
        }

        _playerState.update {
            it.copy(
                currentSong = song,
                isPlaying = false,
                isBuffering = true,
                isResolvingStream = true,
                playbackError = null,
                recoveryStatusMessage = weakNetworkHint,
                fallbackEmbedUrl = initialFallback,
                snifferTargetUrl = null,
                useWebFallback = false,
                activeEngineMode = PlayerEngineMode.EXOPLAYER,
                activeQualityOverride = effectiveQuality,
                retryAttempt = retryAttempt,
                isVideoMode = nextVideoMode,
                playbackSpeed = defaultSpeed,
                isLooping = currentSettings.autoRepeat,
                currentPositionSeconds = resumeSeconds,
                durationSeconds = song.durationSeconds,
                isExpanded = expandPlayer
            )
        }

        streamResolutionJob = viewModelScope.launch {
            // 1. Offline downloaded file first (instant start)
            val downloaded = _uiState.value.downloads.find { it.songId == song.id }
            val offlineFile = downloaded?.let { File(it.localFilePath) }
            if (offlineFile != null && offlineFile.exists() && offlineFile.length() > 1024) {
                startExoPlayerWithUri(
                    uriString = Uri.fromFile(offlineFile).toString(),
                    song = song,
                    resumePositionSeconds = resumeSeconds
                )
                _playerState.update { it.copy(isResolvingStream = false, recoveryStatusMessage = null) }
                return@launch
            }

            // 2. Strictly verified direct / local media URI only (NEVER pass raw page URLs to ExoPlayer)
            val verifiedPreResolved = song.resolvedStreamUrl?.takeIf { MediaStreamResolver.isDirectMediaUri(it) }
            val directExternal = song.externalUrl?.takeIf { MediaStreamResolver.isDirectMediaUri(it) }
            val directCandidate = verifiedPreResolved ?: directExternal
            if (!bypassCache && !directCandidate.isNullOrBlank()) {
                startExoPlayerWithUri(
                    uriString = directCandidate,
                    song = song,
                    resumePositionSeconds = resumeSeconds
                )
                _playerState.update { it.copy(isResolvingStream = false, recoveryStatusMessage = null) }
                return@launch
            }

            // 3. In-memory verified direct-stream cache check (< 1ms)
            val enforceVideoTrack = song.isShort || nextVideoMode
            if (!bypassCache) {
                MediaStreamResolver.getCachedStream(sourceUrl, song.youtubeVideoId, effectiveQuality)?.let { cached ->
                    val cachedUrl = cached.streamUrl
                    val lowerCached = cachedUrl.orEmpty().lowercase()
                    val isAudioOnlyCached = lowerCached.endsWith(".m4a") || lowerCached.endsWith(".mp3") || lowerCached.contains("mime=audio")
                    if (!cachedUrl.isNullOrBlank() && MediaStreamResolver.isDirectMediaUri(cachedUrl) && (!enforceVideoTrack || !isAudioOnlyCached)) {
                        val cachedSong = song.copy(resolvedStreamUrl = cachedUrl)
                        _playerState.update {
                            it.copy(
                                currentSong = cachedSong,
                                isResolvingStream = false,
                                recoveryStatusMessage = null,
                                fallbackEmbedUrl = cached.fallbackEmbedUrl ?: initialFallback,
                                snifferTargetUrl = null,
                                playbackError = null
                            )
                        }
                        startExoPlayerWithUri(
                            uriString = cachedUrl,
                            song = cachedSong,
                            requestHeaders = cached.requestHeaders,
                            resumePositionSeconds = resumeSeconds
                        )
                        return@launch
                    }
                }
            }

            // 4. Hard 10-second direct stream resolution with automatic lower-quality retry
            var result = withTimeoutOrNull(MediaStreamResolver.MAX_RESOLUTION_TIMEOUT_MS) {
                val primary = MediaStreamResolver.resolvePlayableStream(
                    sourceUrl = sourceUrl,
                    videoIdHint = song.youtubeVideoId,
                    preferredQuality = effectiveQuality,
                    bypassCache = bypassCache,
                    requireVideoTrack = enforceVideoTrack
                )
                if (primary.streamUrl == null && effectiveQuality != PreferredQuality.DATA_SAVER && retryAttempt < 1) {
                    val steppedDown = nextLowerQuality(effectiveQuality)
                    _playerState.update {
                        it.copy(
                            activeQualityOverride = steppedDown,
                            retryAttempt = retryAttempt + 1,
                            recoveryStatusMessage = "جارٍ جلب البث بجودة ${steppedDown.titleArabic}..."
                        )
                    }
                    MediaStreamResolver.resolvePlayableStream(
                        sourceUrl = sourceUrl,
                        videoIdHint = song.youtubeVideoId,
                        preferredQuality = steppedDown,
                        bypassCache = bypassCache,
                        requireVideoTrack = enforceVideoTrack
                    )
                } else {
                    primary
                }
            }

            if (result == null) {
                result = com.example.data.remote.ResolvedStreamResult(
                    streamUrl = null,
                    pageUrl = sourceUrl.ifBlank { null },
                    fallbackEmbedUrl = initialFallback
                )
            }

            val directPlayUrl = result.streamUrl?.takeIf { MediaStreamResolver.isDirectMediaUri(it) }
            val resolvedChannelName = result.channelName?.takeIf { it.isNotBlank() }
            val resolvedChannelHandle = result.channelHandle?.takeIf { it.isNotBlank() }
                ?: if (resolvedChannelName != null && !RahmaCatalog.isOfficialRahmaChannelName(resolvedChannelName, "")) {
                    ""
                } else {
                    song.channelHandle
                }
            val updatedSong = song.copy(
                resolvedStreamUrl = directPlayUrl,
                durationSeconds = if (result.durationSeconds > 0) result.durationSeconds else song.durationSeconds,
                arabicTitle = if (song.arabicTitle.startsWith("تشغيل مباشر") && !result.title.isNullOrBlank()) result.title else song.arabicTitle,
                thumbnailUrl = song.thumbnailUrl ?: result.thumbnailUrl,
                sourcePlatform = resolvedChannelName ?: song.sourcePlatform,
                channelHandle = resolvedChannelHandle
            )
            val snifferUrl = result.snifferUrl
                ?: MediaStreamResolver.buildPageSnifferUrl(sourceUrl, extractedYtId)
                ?: result.fallbackEmbedUrl
                ?: initialFallback
            val webFallbackUrl = result.fallbackEmbedUrl ?: initialFallback

            if (directPlayUrl != null) {
                _playerState.update {
                    it.copy(
                        currentSong = updatedSong,
                        isResolvingStream = false,
                        fallbackEmbedUrl = webFallbackUrl,
                        snifferTargetUrl = null,
                        useWebFallback = false,
                        recoveryStatusMessage = null,
                        playbackError = null
                    )
                }
                startExoPlayerWithUri(
                    uriString = directPlayUrl,
                    song = updatedSong,
                    requestHeaders = result.requestHeaders,
                    resumePositionSeconds = resumeSeconds
                )
            } else if (!snifferUrl.isNullOrBlank() && snifferAttemptedForSongId != song.id) {
                // Hidden headless WebView stream-capture backup (captures .mp4/.m3u8 back into native ExoPlayer)
                snifferAttemptedForSongId = song.id
                _playerState.update {
                    it.copy(
                        currentSong = updatedSong,
                        isResolvingStream = true,
                        isBuffering = false,
                        isPlaying = false,
                        fallbackEmbedUrl = webFallbackUrl,
                        snifferTargetUrl = snifferUrl,
                        useWebFallback = false,
                        recoveryStatusMessage = "جارٍ استخراج رابط البث المباشر...",
                        playbackError = null
                    )
                }
            } else {
                _playerState.update {
                    it.copy(
                        isResolvingStream = false,
                        isBuffering = false,
                        isPlaying = false,
                        snifferTargetUrl = null,
                        recoveryStatusMessage = null,
                        playbackError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                    )
                }
            }
        }

        if (!song.id.startsWith("local_")) {
            viewModelScope.launch {
                repository.recordSongPlay(song.id)
            }
        }
    }

    fun onSniffedStreamCaptured(capturedUrl: String, headers: Map<String, String>) {
        val currentState = _playerState.value
        val currentSong = currentState.currentSong ?: return
        if (!MediaStreamResolver.isDirectMediaUri(capturedUrl)) {
            onSnifferFailed()
            return
        }
        val activeQuality = currentState.activeQualityOverride ?: _uiState.value.settings.preferredQuality
        val sourceUrl = currentSong.externalUrl
            ?: if (currentSong.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${currentSong.youtubeVideoId}" else ""

        viewModelScope.launch {
            val verified = MediaStreamResolver.verifyAndCacheSniffedStream(
                originalPageUrl = sourceUrl.ifBlank { capturedUrl },
                videoIdHint = currentSong.youtubeVideoId,
                sniffedMediaUrl = capturedUrl,
                requestHeaders = headers,
                titleHint = currentSong.arabicTitle,
                preferredQuality = activeQuality
            )
            val streamToPlay = verified?.streamUrl ?: capturedUrl
            val updatedSniffedSong = currentSong.copy(resolvedStreamUrl = streamToPlay)
            val resumeSeconds = if (_uiState.value.settings.rememberPlaybackPosition) {
                settingsManager.getPlaybackPosition(currentSong.id)
            } else 0
            _playerState.update {
                it.copy(
                    currentSong = updatedSniffedSong,
                    snifferTargetUrl = null,
                    isResolvingStream = false,
                    isBuffering = true,
                    recoveryStatusMessage = null,
                    playbackError = null,
                    useWebFallback = false,
                    activeEngineMode = PlayerEngineMode.EXOPLAYER
                )
            }
            startExoPlayerWithUri(
                uriString = streamToPlay,
                song = updatedSniffedSong,
                requestHeaders = verified?.requestHeaders ?: headers,
                resumePositionSeconds = resumeSeconds
            )
        }
    }

    fun onSnifferFailed() {
        withActivePlayer {
            stop()
            clearMediaItems()
        }
        _playerState.update {
            it.copy(
                snifferTargetUrl = null,
                isResolvingStream = false,
                isBuffering = false,
                isPlaying = false,
                recoveryStatusMessage = null,
                playbackError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
            )
        }
    }

    fun toggleFullscreen() {
        _playerState.update { it.copy(isFullscreen = !it.isFullscreen) }
    }

    @OptIn(UnstableApi::class)
    private suspend fun startExoPlayerWithUri(
        uriString: String,
        song: Song,
        requestHeaders: Map<String, String> = emptyMap(),
        resumePositionSeconds: Int = 0
    ) {
        if (!MediaStreamResolver.isDirectMediaUri(uriString)) {
            _playerState.update {
                it.copy(
                    isResolvingStream = false,
                    isBuffering = false,
                    isPlaying = false,
                    playbackError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                )
            }
            return
        }

        PlaybackHeaderRegistry.registerHeaders(uriString, requestHeaders)
        val isHls = MediaStreamResolver.isHlsStreamUrl(uriString)
        val mediaItem = MediaSessionMetadataFactory.buildMediaItem(
            song = song,
            streamUri = uriString,
            isHls = isHls,
            context = getApplication(),
            fallbackArtworkBytes = fallbackArtworkBytes
        )

        val player = awaitActivePlayer()
        if (player == null) {
            _playerState.update {
                it.copy(
                    isResolvingStream = false,
                    isBuffering = false,
                    isPlaying = false,
                    playbackError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                )
            }
            return
        }

        val currentQuality = _playerState.value.activeQualityOverride ?: _uiState.value.settings.preferredQuality
        applyQualityTrackParameters(currentQuality)
        player.repeatMode = if (_uiState.value.settings.autoRepeat) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        player.setPlaybackSpeed(_playerState.value.playbackSpeed)
        player.setMediaItem(mediaItem)
        player.prepare()
        if (resumePositionSeconds > 3) {
            player.seekTo(resumePositionSeconds * 1000L)
        }
        player.playWhenReady = true
        startBufferingWatchdog()
    }

    fun retryCurrentPlayback() {
        val current = _playerState.value.currentSong ?: return
        snifferAttemptedForSongId = null
        val src = current.externalUrl ?: if (current.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${current.youtubeVideoId}" else ""
        MediaStreamResolver.invalidateCachedStream(src, current.youtubeVideoId)
        playSongInternal(
            song = current.copy(resolvedStreamUrl = null),
            expandPlayer = _playerState.value.isExpanded,
            qualityOverride = _playerState.value.activeQualityOverride,
            retryAttempt = 0,
            engineModeOverride = PlayerEngineMode.EXOPLAYER,
            bypassCache = true
        )
    }

    fun changeActivePlaybackQuality(quality: PreferredQuality) {
        val current = _playerState.value.currentSong ?: return
        snifferAttemptedForSongId = null
        _playerState.update { it.copy(activeQualityOverride = quality) }
        applyQualityTrackParameters(quality)
        showSnackbar("تم ضبط جودة التشغيل على: ${quality.titleArabic} 🎬")
        if (!current.id.startsWith("local_")) {
            playSongInternal(
                song = current.copy(resolvedStreamUrl = null),
                expandPlayer = _playerState.value.isExpanded,
                qualityOverride = quality,
                retryAttempt = 0,
                engineModeOverride = PlayerEngineMode.EXOPLAYER,
                bypassCache = false
            )
        }
    }

    fun lowerPlaybackQualityOneStep() {
        val currentQuality = _playerState.value.activeQualityOverride ?: _uiState.value.settings.preferredQuality
        val lowered = nextLowerQuality(currentQuality)
        changeActivePlaybackQuality(lowered)
    }

    fun clearPlaybackCacheAndRetry() {
        viewModelScope.launch {
            MediaStreamResolver.clearStreamCache()
            runCatching {
                getApplication<Application>().cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            }
            showSnackbar("تم مسح الكاش المؤقت وإعادة تحميل البث 🧹")
            retryCurrentPlayback()
        }
    }

    fun switchPlayerEngineMode() {
        val current = _playerState.value.currentSong ?: return
        val currentlyWeb = _playerState.value.useWebFallback || _playerState.value.activeEngineMode == PlayerEngineMode.WEB_ALTERNATIVE
        if (currentlyWeb) {
            showSnackbar("التبديل إلى مشغل ExoPlayer الأصلي 🎬")
            playSongInternal(
                song = current.copy(resolvedStreamUrl = null),
                expandPlayer = _playerState.value.isExpanded,
                qualityOverride = _playerState.value.activeQualityOverride,
                retryAttempt = 0,
                engineModeOverride = PlayerEngineMode.EXOPLAYER
            )
        } else {
            val embedCandidate = _playerState.value.fallbackEmbedUrl
                ?: if (current.youtubeVideoId.isNotBlank()) MediaStreamResolver.buildSilentYouTubeEmbedUrl(current.youtubeVideoId)
                else current.externalUrl?.takeIf { it.startsWith("http", ignoreCase = true) }

            if (embedCandidate.isNullOrBlank()) {
                showSnackbar("لا يتوفر رابط ويب بديل لهذا الملف المحلي؛ جرب خفض الجودة")
                return
            }
            withActivePlayer {
                stop()
                clearMediaItems()
            }
            _playerState.update {
                it.copy(
                    fallbackEmbedUrl = embedCandidate,
                    useWebFallback = true,
                    activeEngineMode = PlayerEngineMode.WEB_ALTERNATIVE,
                    isBuffering = false,
                    isResolvingStream = false,
                    isPlaying = true,
                    recoveryStatusMessage = null,
                    playbackError = null
                )
            }
            showSnackbar("تم التبديل للمشغل البديل المدمج 🔄")
        }
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.currentSong == null) return
        val player = _activePlayer.value
        if (player == null) {
            retryCurrentPlayback()
            return
        }
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE || current.playbackError != null) {
                retryCurrentPlayback()
            } else {
                player.play()
            }
        }
    }

    fun seekTo(seconds: Int) {
        val maxDuration = _playerState.value.durationSeconds.coerceAtLeast(1)
        val clamped = seconds.coerceIn(0, maxDuration)
        withActivePlayer { seekTo(clamped * 1000L) }
        _playerState.update { it.copy(currentPositionSeconds = clamped) }
        _playerState.value.currentSong?.let { song ->
            settingsManager.savePlaybackPosition(song.id, clamped)
        }
    }

    fun skipForward10() {
        seekTo(_playerState.value.currentPositionSeconds + 10)
    }

    fun skipBackward10() {
        seekTo((_playerState.value.currentPositionSeconds - 10).coerceAtLeast(0))
    }

    fun setExpanded(expanded: Boolean) {
        _playerState.update {
            it.copy(
                isExpanded = expanded,
                isFullscreen = if (expanded) it.isFullscreen else false
            )
        }
    }

    fun toggleVideoAudioMode() {
        _playerState.update { it.copy(isVideoMode = !it.isVideoMode) }
    }

    fun toggleLyricsExpanded() {
        _playerState.update { it.copy(isLyricsExpanded = !it.isLyricsExpanded) }
    }

    fun cyclePlaybackSpeed() {
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f)
        val currentSpeed = _playerState.value.playbackSpeed
        val nextIndex = (speeds.indexOf(currentSpeed) + 1) % speeds.size
        val nextSpeed = speeds[nextIndex]
        withActivePlayer { setPlaybackSpeed(nextSpeed) }
        _playerState.update { it.copy(playbackSpeed = nextSpeed) }
    }

    fun toggleLoop() {
        val nextLoop = !_playerState.value.isLooping
        withActivePlayer {
            repeatMode = if (nextLoop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
        _playerState.update { it.copy(isLooping = nextLoop) }
    }

    fun playNextSong() {
        val current = _playerState.value.currentSong ?: return
        val allSongs = _uiState.value.songs.ifEmpty { _uiState.value.allSongs }
        if (allSongs.isEmpty()) return
        val currentIndex = allSongs.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex + 1 < allSongs.size) {
            playSong(allSongs[currentIndex + 1], expandPlayer = _playerState.value.isExpanded)
        } else {
            playSong(allSongs[0], expandPlayer = _playerState.value.isExpanded)
        }
    }

    fun playPreviousSong() {
        val current = _playerState.value.currentSong ?: return
        val allSongs = _uiState.value.songs.ifEmpty { _uiState.value.allSongs }
        if (allSongs.isEmpty()) return
        val currentIndex = allSongs.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playSong(allSongs[currentIndex - 1], expandPlayer = _playerState.value.isExpanded)
        }
    }

    fun closePlayer() {
        _playerState.value.currentSong?.let { song ->
            settingsManager.savePlaybackPosition(song.id, _playerState.value.currentPositionSeconds)
        }
        streamResolutionJob?.cancel()
        bufferingWatchdogJob?.cancel()
        withActivePlayer {
            stop()
            clearMediaItems()
        }
        _playerState.update {
            PlayerUiState(currentSong = null, isPlaying = false, isExpanded = false)
        }
    }

    // Link Inspection, Auto-Correction & Pre-Flight Validation
    fun inspectPastedLink(rawInput: String) {
        val diag = LinkValidator.inspectLocally(rawInput, _uiState.value.settings)
        _uiState.update { it.copy(linkCheckState = diag) }
    }

    fun checkLinkBeforePlay(rawInput: String) {
        if (rawInput.isBlank()) {
            _uiState.update {
                it.copy(
                    linkCheckState = LinkDiagnosticResult(
                        status = LinkCheckStatus.INVALID,
                        exactReason = "يرجى لصق الرابط أولاً ليتم فحصه."
                    )
                )
            }
            return
        }
        val settings = _uiState.value.settings
        val local = LinkValidator.inspectLocally(rawInput, settings)
        _uiState.update {
            it.copy(
                linkCheckState = local.copy(status = LinkCheckStatus.CHECKING, exactReason = null)
            )
        }
        viewModelScope.launch {
            val checked = LinkValidator.checkLink(rawInput, _uiState.value.settings)
            _uiState.update { it.copy(linkCheckState = checked) }
        }
    }

    fun clearLinkCheckState() {
        _uiState.update { it.copy(linkCheckState = LinkDiagnosticResult()) }
    }

    fun allowCustomDomainFromLinkCheck(domain: String) {
        if (domain.isBlank()) return
        val added = settingsManager.addCustomLinkDomain(domain)
        if (added != null) {
            val currentCustom = _uiState.value.settings.customSourceConfig
            if (!currentCustom.enabled) {
                settingsManager.updateCustomSourceConfig(currentCustom.copy(enabled = true))
            }
            showSnackbar("تمت إضافة النطاق «$added» للمصادر المدعومة ✅")
            val candidate = _uiState.value.linkCheckState.correctedUrl.ifBlank {
                _uiState.value.linkCheckState.rawInput
            }
            if (candidate.isNotBlank()) {
                inspectPastedLink(candidate)
            }
        }
    }

    fun enableLinkSourceNow(detection: LinkSourceDetection) {
        if (detection.isCustomDomainMatch) {
            val current = _uiState.value.settings.customSourceConfig
            settingsManager.updateCustomSourceConfig(current.copy(enabled = true))
        } else {
            settingsManager.setLinkSourceEnabled(detection.sourceType, true)
        }
        showSnackbar("تم تفعيل مصدر «${detection.sourceLabelArabic}» ✅")
        val candidate = _uiState.value.linkCheckState.correctedUrl.ifBlank {
            _uiState.value.linkCheckState.rawInput
        }
        if (candidate.isNotBlank()) {
            checkLinkBeforePlay(candidate)
        }
    }

    // External Browser (Chrome / Default Browser) Launcher
    fun openUrlInExternalBrowser(rawUrl: String) {
        val cleanUrl = LinkValidator.unwrapRedirectWrapperUrl(rawUrl).trim().let {
            if (it.startsWith("http://", true) || it.startsWith("https://", true)) it else "https://$it"
        }
        val uri = runCatching { Uri.parse(cleanUrl) }.getOrNull() ?: return
        val context = getApplication<Application>()
        val chromeIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setPackage("com.android.chrome")
        }
        val launchedChrome = runCatching {
            context.startActivity(chromeIntent)
            true
        }.getOrDefault(false)
        if (!launchedChrome) {
            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(fallbackIntent) }
                .onFailure { showSnackbar("تعذر العثور على متصفح لفتح الرابط") }
        }
    }

    fun openRahmaGoogleNewsInBrowser() {
        openUrlInExternalBrowser("https://news.google.com/search?q=%D8%B1%D8%AD%D9%85%D8%A9+%D8%B1%D9%8A%D8%A7%D8%B6&hl=ar")
    }

    // Admin Link Inspection, Live Preview & One-Time Import inside Settings
    fun previewImportLink(rawInput: String) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(linkPreviewState = LinkPreviewState.Error("يرجى لصق الرابط أولاً للمعاينة")) }
            return
        }
        val settings = _uiState.value.settings
        val diagnostic = LinkValidator.inspectLocally(trimmed, settings)
        _uiState.update { it.copy(linkCheckState = diagnostic) }
        if (diagnostic.status == LinkCheckStatus.INVALID || diagnostic.status == LinkCheckStatus.UNSUPPORTED) {
            _uiState.update {
                it.copy(linkPreviewState = LinkPreviewState.Error(diagnostic.exactReason ?: "الرابط غير صالح"))
            }
            return
        }
        if (!diagnostic.isSourceEnabled) {
            _uiState.update {
                it.copy(
                    linkPreviewState = LinkPreviewState.Error(
                        "مصدر «${diagnostic.detection.sourceLabelArabic}» معطّل في الإعدادات"
                    )
                )
            }
            return
        }
        val corrected = diagnostic.correctedUrl
        _uiState.update { it.copy(linkPreviewState = LinkPreviewState.Loading(corrected)) }

        viewModelScope.launch {
            val meta = MediaStreamResolver.fetchLinkPreviewMetadata(corrected)
            val sourceConfig = LinkValidator.getSourceConfig(diagnostic.detection, settings)
            val isWebOnly = LinkValidator.isWebOnlyNonPlayableUrl(corrected) &&
                MediaStreamResolver.extractYouTubeId(corrected).isBlank() &&
                !MediaStreamResolver.isDirectMediaUri(corrected)
            val ytId = MediaStreamResolver.extractYouTubeId(corrected)
            val alreadyExists = _uiState.value.allSongs.any { existing ->
                val exUrl = existing.externalUrl?.let { LinkValidator.unwrapRedirectWrapperUrl(it) }.orEmpty()
                (ytId.isNotBlank() && existing.youtubeVideoId.equals(ytId, ignoreCase = true)) ||
                    (exUrl.isNotBlank() && exUrl.equals(corrected, ignoreCase = true))
            }
            val suggestedCategory = when {
                corrected.contains("/shorts/", ignoreCase = true) ||
                    corrected.contains("/reel/", ignoreCase = true) ||
                    diagnostic.detection.sourceType == LinkSourceType.TIKTOK -> SongCategory.REELS
                else -> sourceConfig.targetCategory
            }
            _uiState.update {
                it.copy(
                    linkPreviewState = LinkPreviewState.Ready(
                        correctedUrl = corrected,
                        title = meta.title?.takeIf { t -> t.isNotBlank() }
                            ?: "محتوى رحمة رياض (${diagnostic.detection.sourceLabelArabic})",
                        thumbnailUrl = meta.thumbnailUrl
                            ?: if (ytId.isNotBlank()) "https://i.ytimg.com/vi/$ytId/hqdefault.jpg" else null,
                        platformLabel = meta.platformLabel.ifBlank { diagnostic.detection.sourceLabelArabic },
                        channelName = meta.channelName?.takeIf { c -> c.isNotBlank() }
                            ?: RahmaCatalog.OFFICIAL_CHANNEL_AR_NAME,
                        suggestedCategory = suggestedCategory,
                        suggestedMediaType = sourceConfig.mediaType,
                        isWebOnly = isWebOnly,
                        isDuplicate = alreadyExists
                    )
                )
            }
        }
    }

    fun clearLinkPreview() {
        _uiState.update { it.copy(linkPreviewState = LinkPreviewState.Idle, linkCheckState = LinkDiagnosticResult()) }
    }

    fun importLinkAsAdmin(
        rawUrl: String,
        customTitle: String,
        targetCategory: SongCategory,
        mediaType: LocalMediaType
    ) {
        val settings = _uiState.value.settings
        val diagnostic = LinkValidator.inspectLocally(rawUrl, settings)
        if (diagnostic.status == LinkCheckStatus.INVALID || diagnostic.status == LinkCheckStatus.UNSUPPORTED) {
            showSnackbar(diagnostic.exactReason ?: "الرابط غير صالح")
            return
        }
        val normalized = diagnostic.correctedUrl
        val ytId = MediaStreamResolver.extractYouTubeId(normalized)
        val duplicate = _uiState.value.allSongs.any { existing ->
            val exUrl = existing.externalUrl?.let { LinkValidator.unwrapRedirectWrapperUrl(it) }.orEmpty()
            (ytId.isNotBlank() && existing.youtubeVideoId.equals(ytId, ignoreCase = true)) ||
                (exUrl.isNotBlank() && exUrl.equals(normalized, ignoreCase = true))
        }
        if (duplicate) {
            showSnackbar("هذا الرابط مضاف مسبقاً ولن يتم تكراره أو إعادة رفعه ✅")
            return
        }

        val readyPreview = _uiState.value.linkPreviewState as? LinkPreviewState.Ready
        val finalTitle = customTitle.trim()
            .ifBlank { readyPreview?.title?.takeIf { it.isNotBlank() } ?: "عمل رحمة رياض (${diagnostic.detection.sourceLabelArabic})" }
        val isShort = targetCategory == SongCategory.REELS ||
            normalized.contains("/shorts/", ignoreCase = true) ||
            normalized.contains("/reel/", ignoreCase = true)

        viewModelScope.launch {
            _uiState.update { it.copy(isImportingLink = true) }
            val meta = if (readyPreview != null && readyPreview.correctedUrl == normalized) {
                null
            } else {
                MediaStreamResolver.fetchLinkPreviewMetadata(normalized)
            }
            val thumb = readyPreview?.thumbnailUrl ?: meta?.thumbnailUrl
                ?: if (ytId.isNotBlank()) "https://i.ytimg.com/vi/$ytId/hqdefault.jpg" else null
            val channel = readyPreview?.channelName ?: meta?.channelName ?: diagnostic.detection.sourceLabelArabic

            val entity = ImportedMediaEntity(
                id = "imp_${UUID.randomUUID().toString().take(8)}",
                title = finalTitle,
                sourceUrl = normalized,
                thumbnailUrl = thumb,
                platform = diagnostic.detection.sourceLabelArabic,
                videoId = ytId,
                isShort = isShort,
                description = "إضافة الإدارة • $channel",
                channelName = channel,
                channelHandle = if (ytId.isNotBlank()) RahmaCatalog.OFFICIAL_CHANNEL_HANDLE else "",
                categoryName = targetCategory.name,
                mediaType = mediaType.name
            )
            repository.saveImportedMedia(entity)
            _uiState.update {
                it.copy(
                    isImportingLink = false,
                    linkPreviewState = LinkPreviewState.Idle,
                    linkCheckState = LinkDiagnosticResult()
                )
            }
            showSnackbar("تمت إضافة «$finalTitle» إلى قسم ${targetCategory.displayName} بنجاح ✅")
        }
    }

    // Real External Link & Reel Import (YouTube, Instagram, TikTok, Facebook, Custom Sites, Direct Video URLs)
    fun playExternalUrl(url: String, saveAsReel: Boolean = false, playImmediately: Boolean = true) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return

        val settings = _uiState.value.settings
        val looksLikeSearchText = !trimmed.contains("://") && !trimmed.contains(".") && !trimmed.startsWith("www.")
        if (looksLikeSearchText) {
            triggerSmartSearch(trimmed)
            return
        }

        val diagnostic = LinkValidator.inspectLocally(trimmed, settings)
        _uiState.update { it.copy(linkCheckState = diagnostic) }

        if (diagnostic.status == LinkCheckStatus.INVALID || diagnostic.status == LinkCheckStatus.UNSUPPORTED) {
            showSnackbar(diagnostic.exactReason ?: "الرابط غير صالح أو غير مدعوم")
            return
        }
        if (!diagnostic.isSourceEnabled) {
            showSnackbar("مصدر «${diagnostic.detection.sourceLabelArabic}» معطّل في إعدادات مصدر الروابط")
            return
        }

        val normalized = diagnostic.correctedUrl
        val ytId = MediaStreamResolver.extractYouTubeId(normalized)
        if (ytId.isBlank() && !MediaStreamResolver.isDirectMediaUri(normalized) && LinkValidator.isWebOnlyNonPlayableUrl(normalized)) {
            openUrlInExternalBrowser(normalized)
            return
        }

        val sourceConfig = LinkValidator.getSourceConfig(diagnostic.detection, settings)
        val shouldAutoPlay = playImmediately && sourceConfig.autoPlay

        val lower = normalized.lowercase()
        val platform = diagnostic.detection.sourceLabelArabic
        val isShort = saveAsReel ||
            lower.contains("/shorts/") ||
            lower.contains("/reel/") ||
            lower.contains("tiktok.com") ||
            lower.contains("/share/r/")
        val targetCat = if (isShort) SongCategory.REELS else sourceConfig.targetCategory

        viewModelScope.launch {
            _uiState.update { it.copy(isImportingLink = true) }
            val resolved = withTimeoutOrNull(MediaStreamResolver.MAX_RESOLUTION_TIMEOUT_MS) {
                MediaStreamResolver.resolvePlayableStream(
                    sourceUrl = normalized,
                    videoIdHint = ytId,
                    preferredQuality = sourceConfig.quality,
                    requireVideoTrack = isShort
                )
            } ?: com.example.data.remote.ResolvedStreamResult(
                pageUrl = normalized,
                fallbackEmbedUrl = if (ytId.isNotBlank()) MediaStreamResolver.buildSilentYouTubeEmbedUrl(ytId) else normalized
            )
            _uiState.update { it.copy(isImportingLink = false) }

            val itemTitle = resolved.title?.takeIf { it.isNotBlank() } ?: "مقطع $platform"
            val itemId = "imp_${UUID.randomUUID().toString().take(8)}"
            val realChannel = resolved.channelName?.takeIf { it.isNotBlank() } ?: resolved.platformLabel
            val realHandle = resolved.channelHandle.orEmpty()

            val existingDuplicate = _uiState.value.allSongs.find { existing ->
                val exUrl = existing.externalUrl?.let { LinkValidator.unwrapRedirectWrapperUrl(it) }.orEmpty()
                (ytId.isNotBlank() && existing.youtubeVideoId.equals(ytId, ignoreCase = true)) ||
                    (exUrl.isNotBlank() && exUrl.equals(normalized, ignoreCase = true))
            }

            if (existingDuplicate == null) {
                val entity = ImportedMediaEntity(
                    id = itemId,
                    title = itemTitle,
                    sourceUrl = normalized,
                    thumbnailUrl = resolved.thumbnailUrl ?: if (ytId.isNotBlank()) "https://i.ytimg.com/vi/$ytId/hqdefault.jpg" else null,
                    platform = resolved.platformLabel,
                    videoId = ytId,
                    isShort = isShort || resolved.isShort,
                    description = "مستورد من $realChannel",
                    channelName = realChannel,
                    channelHandle = realHandle,
                    categoryName = targetCat.name,
                    mediaType = sourceConfig.mediaType.name
                )
                repository.saveImportedMedia(entity)
            }

            if (shouldAutoPlay) {
                val directPlay = resolved.streamUrl?.takeIf { MediaStreamResolver.isDirectMediaUri(it) }
                val song = existingDuplicate ?: Song(
                    id = itemId,
                    title = itemTitle,
                    arabicTitle = itemTitle,
                    category = targetCat,
                    durationSeconds = resolved.durationSeconds,
                    durationFormatted = if (isShort) "ريلز" else "مباشر",
                    viewsCount = resolved.platformLabel,
                    releaseYear = "مستورد",
                    description = "تشغيل مباشر من $realChannel",
                    lyrics = "القناة: $realChannel\nالرابط الأصلي:\n$normalized",
                    composer = realChannel,
                    lyricist = if (realHandle.isNotBlank()) realHandle else resolved.platformLabel,
                    isNewRelease = true,
                    youtubeVideoId = ytId,
                    thumbnailUrl = resolved.thumbnailUrl ?: if (ytId.isNotBlank()) "https://i.ytimg.com/vi/$ytId/hqdefault.jpg" else null,
                    sourcePlatform = realChannel,
                    externalUrl = normalized,
                    resolvedStreamUrl = directPlay,
                    isShort = isShort || resolved.isShort,
                    isUserImported = true,
                    channelHandle = realHandle,
                    mediaType = sourceConfig.mediaType.name
                )
                snifferAttemptedForSongId = null
                playSongInternal(
                    song = song,
                    expandPlayer = true,
                    qualityOverride = sourceConfig.quality,
                    retryAttempt = 0,
                    engineModeOverride = PlayerEngineMode.EXOPLAYER,
                    bypassCache = false
                )
                if (directPlay != null) {
                    showSnackbar("تم استخراج وتشغيل «$itemTitle» (${sourceConfig.quality.titleArabic}) 🎬")
                } else {
                    showSnackbar("جارٍ استخراج رابط التشغيل المباشر...")
                }
            } else if (playImmediately && !sourceConfig.autoPlay) {
                showSnackbar("تم حفظ «$itemTitle» (التشغيل التلقائي معطّل لهذا المصدر في الإعدادات) ✅")
            } else {
                showSnackbar("تمت إضافة «$itemTitle» بنجاح ✅")
            }
        }
    }

    fun deleteImportedMedia(id: String) {
        viewModelScope.launch {
            repository.deleteImportedMedia(id)
            showSnackbar("تم حذف المقطع المستورد")
        }
    }

    // Real Reel Comments & Persistent Likes
    fun observeReelComments(reelId: String): Flow<List<ReelComment>> = repository.getCommentsForReel(reelId)

    fun addReelComment(reelId: String, text: String) {
        if (text.isBlank()) return
        val author = Firebase.auth.currentUser?.displayName ?: "مستخدم التطبيق"
        viewModelScope.launch {
            repository.addReelComment(reelId, author, text.trim())
        }
    }

    fun toggleReelOrSongLike(id: String) {
        viewModelScope.launch {
            val isCurrentlyLiked = _uiState.value.likedIds.contains(id)
            repository.toggleReelLike(id, isCurrentlyLiked)
        }
    }

    // Favorites
    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            val isFav = _uiState.value.favoriteIds.contains(song.id)
            repository.toggleFavorite(song.id, isFav)
            val currentUser = Firebase.auth.currentUser
            if (currentUser != null) {
                try {
                    if (isFav) {
                        firebaseRepository.removeFavorite(currentUser.uid, song.id)
                    } else {
                        firebaseRepository.addFavorite(currentUser.uid, song.id, song.arabicTitle)
                    }
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Failed to sync favorite with Firestore: ${e.message}")
                }
            }
            val msg = if (isFav) "تم إزالة '${song.arabicTitle}' من المفضلة" else "تم إضافة '${song.arabicTitle}' إلى المفضلة ❤️"
            showSnackbar(msg)
        }
    }

    // Featured Songs Pinning (Single Clean Definition)
    fun toggleFeatured(song: Song) {
        viewModelScope.launch {
            val nextFeatured = !song.isFeatured
            repository.setFeaturedPreference(song.id, nextFeatured)
            _playerState.update { current ->
                if (current.currentSong?.id == song.id) {
                    current.copy(currentSong = current.currentSong.copy(isFeatured = nextFeatured))
                } else current
            }
            val msg = if (nextFeatured) {
                "تمت إضافة «${song.arabicTitle}» إلى الأغاني المميزة ⭐"
            } else {
                "تمت إزالة «${song.arabicTitle}» من الأغاني المميزة"
            }
            showSnackbar(msg)
        }
    }

    // Real In-App Downloads with Wi-Fi Guard, Default Format, Quality, & Phone MediaStore Export
    private fun isWifiConnected(): Boolean {
        return try {
            val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        } catch (_: Exception) {
            true
        }
    }

    private fun isDownloadAllowedBySource(song: Song, settings: AppSettings): Boolean {
        val url = song.externalUrl ?: if (song.youtubeVideoId.isNotBlank()) {
            "https://www.youtube.com/watch?v=${song.youtubeVideoId}"
        } else null
        if (url.isNullOrBlank() || !url.startsWith("http", ignoreCase = true)) return true
        val detection = LinkValidator.detectSource(url, settings.customLinkDomains)
        val config = LinkValidator.getSourceConfig(detection, settings)
        if (!config.allowDownload) {
            showSnackbar("التحميل معطّل لمصدر «${detection.sourceLabelArabic}» من إعدادات مصدر الروابط 🔒")
            return false
        }
        return true
    }

    fun promptDownload(song: Song) {
        val currentSettings = _uiState.value.settings
        if (!isDownloadAllowedBySource(song, currentSettings)) return
        if (currentSettings.downloadOverWifiOnly && !isWifiConnected()) {
            showSnackbar("خيار التنزيل عبر Wi-Fi فقط مفعّل بالإعدادات 📶")
            return
        }
        when (currentSettings.defaultDownloadType) {
            "FULL_VIDEO" -> startDownload(song, DownloadType.FULL_VIDEO)
            "AUDIO_ONLY" -> startDownload(song, DownloadType.AUDIO_ONLY)
            else -> _uiState.update { it.copy(songToDownload = song) }
        }
    }

    fun dismissDownloadPrompt() {
        _uiState.update { it.copy(songToDownload = null) }
    }

    fun startDownload(song: Song, type: DownloadType) {
        val currentSettings = _uiState.value.settings
        if (!isDownloadAllowedBySource(song, currentSettings)) {
            _uiState.update { it.copy(songToDownload = null) }
            return
        }
        if (currentSettings.downloadOverWifiOnly && !isWifiConnected()) {
            _uiState.update { it.copy(songToDownload = null) }
            showSnackbar("خيار التنزيل عبر Wi-Fi فقط مفعّل بالإعدادات 📶")
            return
        }

        _uiState.update {
            it.copy(
                songToDownload = null,
                activeDownloadSongId = song.id,
                activeDownloadTitle = song.arabicTitle,
                activeDownloadProgress = 0
            )
        }

        viewModelScope.launch {
            repository.startDownload(
                song = song,
                type = type,
                downloadQuality = currentSettings.downloadQuality,
                storageLocation = currentSettings.downloadStorageLocation,
                autoRetry = currentSettings.autoRetryDownload
            )
                .catch { err ->
                    _uiState.update {
                        it.copy(
                            activeDownloadSongId = null,
                            activeDownloadTitle = null,
                            activeDownloadProgress = 0
                        )
                    }
                    showSnackbar(err.message ?: "فشل التنزيل، يرجى المحاولة لاحقاً")
                }
                .collect { progress ->
                    _uiState.update { it.copy(activeDownloadProgress = progress) }
                    if (progress == 100) {
                        delay(400)
                        _uiState.update {
                            it.copy(
                                activeDownloadSongId = null,
                                activeDownloadTitle = null,
                                activeDownloadProgress = 0
                            )
                        }
                        val where = if (currentSettings.downloadStorageLocation == DownloadStorageLocation.APP_AND_DEVICE) {
                            "تم الحفظ بالتطبيق ومجلد التنزيلات بالهاتف 🎉"
                        } else {
                            "تم الحفظ داخل التطبيق للتشغيل أوفلاين 🎉"
                        }
                        showSnackbar("تم تنزيل «${song.arabicTitle}» بنجاح! $where")
                    }
                }
        }
    }

    fun deleteDownload(media: DownloadedMedia) {
        viewModelScope.launch {
            repository.deleteDownload(media.id, media.localFilePath)
            showSnackbar("تم حذف الملف المنزّل: ${media.title}")
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            repository.clearAllDownloads()
            showSnackbar("تم مسح جميع الملفات المنزّلة من ذاكرة التطبيق 🗑️")
        }
    }

    // Notifications + 6-Second Countdown Timer Before Auto-Opening Target
    fun markNotificationAsRead(notification: AppNotification) {
        startNotificationCountdown(notification)
    }

    fun startNotificationCountdown(notification: AppNotification) {
        viewModelScope.launch {
            repository.markAsRead(notification.id)
        }
        notificationCountdownJob?.cancel()
        notificationCountdownJob = viewModelScope.launch {
            for (remaining in 6 downTo 1) {
                _uiState.update {
                    it.copy(notificationCountdown = NotificationCountdownState(notification, remaining))
                }
                delay(1_000L)
            }
            _uiState.update { it.copy(notificationCountdown = null) }
            executeNotificationTarget(notification)
        }
    }

    fun cancelNotificationCountdown() {
        notificationCountdownJob?.cancel()
        _uiState.update { it.copy(notificationCountdown = null) }
    }

    fun confirmNotificationCountdownNow() {
        val target = _uiState.value.notificationCountdown?.notification ?: return
        notificationCountdownJob?.cancel()
        _uiState.update { it.copy(notificationCountdown = null) }
        executeNotificationTarget(target)
    }

    private fun executeNotificationTarget(notification: AppNotification) {
        val matchedSong = notification.songId?.let { id ->
            _uiState.value.allSongs.find { it.id == id }
        }
        if (matchedSong != null) {
            playSong(matchedSong, expandPlayer = true)
            return
        }
        val url = notification.linkedUrl?.takeIf { it.isNotBlank() }
        if (url != null) {
            val ytId = MediaStreamResolver.extractYouTubeId(url)
            if (ytId.isBlank() && !MediaStreamResolver.isDirectMediaUri(url) && LinkValidator.isWebOnlyNonPlayableUrl(url)) {
                openUrlInExternalBrowser(url)
            } else {
                playExternalUrl(url, saveAsReel = false, playImmediately = true)
            }
        }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // Settings actions (Draft Commit, Display, Categories, Appearance, Local, Download Settings, Video Playback Settings)
    fun applyAllSettings(newSettings: AppSettings) {
        settingsManager.applyAllSettings(newSettings)
        applyExoPlayerSettings(newSettings)
        showSnackbar("تم حفظ الإعدادات وتطبيقها فوراً ✅")
    }

    fun resetAllSettings() {
        settingsManager.resetToDefaults()
        showSnackbar("تمت استعادة الإعدادات الافتراضية")
    }

    fun toggleGridView() {
        val next = !_uiState.value.settings.isGridView
        settingsManager.updateGridView(next)
    }

    fun updateHideCategoriesBar(hide: Boolean) = settingsManager.updateHideCategoriesBar(hide)
    fun setCategoryHidden(category: SongCategory, hidden: Boolean) = settingsManager.setCategoryHidden(category, hidden)
    fun updateThemeMode(mode: AppThemeMode) = settingsManager.updateThemeMode(mode)
    fun updateAccentColor(accent: AppAccentColor) = settingsManager.updateAccentColor(accent)

    fun updateShowLocalFiles(show: Boolean) {
        settingsManager.updateShowLocalFiles(show)
        if (show) scanDeviceMedia(silent = true)
    }

    // Link Sources Settings ("مصدر الروابط")
    fun setLinkSourceEnabled(source: LinkSourceType, enabled: Boolean) {
        settingsManager.setLinkSourceEnabled(source, enabled)
    }

    fun updateLinkSourceConfig(source: LinkSourceType, config: LinkSourceConfig) {
        settingsManager.updateLinkSourceConfig(source, config)
    }

    fun setAllLinkSourcesEnabled(enabled: Boolean) {
        settingsManager.setAllLinkSourcesEnabled(enabled)
        showSnackbar(if (enabled) "تم تفعيل جميع مصادر الروابط ✅" else "تم تعطيل جميع مصادر الروابط")
    }

    fun updateCustomSourceConfig(config: LinkSourceConfig) {
        settingsManager.updateCustomSourceConfig(config)
    }

    fun addCustomLinkDomain(rawDomain: String) {
        val added = settingsManager.addCustomLinkDomain(rawDomain)
        if (added != null) {
            showSnackbar("تمت إضافة النطاق المخصص: $added 🌐")
        } else {
            showSnackbar("يرجى إدخال اسم نطاق صالح مثل example.com")
        }
    }

    fun removeCustomLinkDomain(domain: String) {
        settingsManager.removeCustomLinkDomain(domain)
        showSnackbar("تم حذف النطاق: $domain")
    }

    // Local Media SAF Grants + Auto Refresh Observer
    fun onLocalFolderGranted(uri: Uri) {
        runCatching {
            getApplication<Application>().contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        settingsManager.addGrantedLocalFolderUri(uri.toString())
        scanDeviceMedia(silent = false)
    }

    fun removeGrantedLocalFolder(uriString: String) {
        settingsManager.removeGrantedLocalFolderUri(uriString)
        scanDeviceMedia(silent = true)
        showSnackbar("تمت إزالة مجلد الوسائط المحدد")
    }

    fun scanDeviceMedia(silent: Boolean = false) {
        viewModelScope.launch {
            val granted = _uiState.value.settings.grantedLocalFolderUris.toSet()
            val scanned = LocalMediaScanner.scanAllDeviceMedia(getApplication(), granted)
            repository.saveLocalFiles(scanned)
            if (!silent) {
                showSnackbar("تم تحديث الوسائط المحلية (${scanned.size} ملف) 📱")
            }
        }
    }

    private fun registerLocalMediaObservers(grantedFolders: Set<String>) {
        val resolver = getApplication<Application>().contentResolver
        mediaContentObserver?.let { existing ->
            runCatching { resolver.unregisterContentObserver(existing) }
        }
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                scheduleSilentLocalRescan()
            }
        }
        mediaContentObserver = observer
        runCatching {
            resolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer)
            resolver.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer)
            resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        }
        grantedFolders.forEach { uriStr ->
            runCatching {
                val treeUri = Uri.parse(uriStr)
                val docId = DocumentsContract.getTreeDocumentId(treeUri)
                val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                resolver.registerContentObserver(childUri, true, observer)
            }
        }
    }

    private fun scheduleSilentLocalRescan() {
        localRefreshJob?.cancel()
        localRefreshJob = viewModelScope.launch {
            delay(500)
            scanDeviceMedia(silent = true)
        }
    }

    fun updateAutoPlayNext(enabled: Boolean) = settingsManager.updateAutoPlayNext(enabled)
    fun updateRememberPosition(enabled: Boolean) = settingsManager.updateRememberPosition(enabled)
    fun updateQuality(quality: PreferredQuality) = settingsManager.updateQuality(quality)

    fun updateAutoRepeat(enabled: Boolean) {
        settingsManager.updateAutoRepeat(enabled)
        withActivePlayer {
            repeatMode = if (enabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
        }
        _playerState.update { it.copy(isLooping = enabled) }
    }

    fun updateLyricsSync(enabled: Boolean) = settingsManager.updateLyricsSync(enabled)
    fun updateWifiOnly(enabled: Boolean) = settingsManager.updateWifiOnly(enabled)
    fun updateDefaultDownloadType(type: String) = settingsManager.setDefaultDownloadType(type)
    fun updateDownloadQuality(quality: PreferredQuality) = settingsManager.setDownloadQuality(quality)
    fun updateDownloadStorageLocation(location: DownloadStorageLocation) = settingsManager.setDownloadStorageLocation(location)
    fun updateAutoRetryDownload(enabled: Boolean) = settingsManager.setAutoRetryDownload(enabled)

    fun updateDefaultVideoMode(videoMode: Boolean) {
        settingsManager.setDefaultVideoMode(videoMode)
        _playerState.update { it.copy(isVideoMode = videoMode) }
    }

    fun updateDefaultPlaybackSpeed(speed: Float) {
        settingsManager.setDefaultPlaybackSpeed(speed)
        withActivePlayer { setPlaybackSpeed(speed) }
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun updateVideoAspectMode(mode: VideoAspectMode) = settingsManager.setVideoAspectMode(mode)
    fun updateSilentWebFallbackEnabled(enabled: Boolean) = settingsManager.setSilentWebFallbackEnabled(enabled)

    fun updateNotifications(enabled: Boolean) = settingsManager.updateNotifications(enabled)
    fun updateVibrate(enabled: Boolean) = settingsManager.updateVibrate(enabled)

    fun updateSleepTimer(minutes: Int) {
        settingsManager.updateSleepTimer(minutes)
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                withActivePlayer { pause() }
                settingsManager.updateSleepTimer(0)
                showSnackbar("انتهى مؤقت النوم، تم إيقاف التشغيل 🌙")
            }
            showSnackbar("تم ضبط مؤقت النوم على $minutes دقيقة")
        } else {
            showSnackbar("تم إيقاف مؤقت النوم")
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            try {
                val cacheDir = getApplication<Application>().cacheDir
                cacheDir.listFiles()?.forEach { it.deleteRecursively() }
                showSnackbar("تم تنظيف ذاكرة التخزين المؤقت بنجاح 🧹")
            } catch (_: Exception) {
                showSnackbar("تم فحص الذاكرة المؤقتة")
            }
        }
    }

    // Local files actions
    fun importLocalFiles(files: List<LocalFileEntity>) {
        viewModelScope.launch {
            repository.saveLocalFiles(files)
            showSnackbar("تم استيراد ${files.size} ملف للوسائط المحلية بنجاح 📁")
        }
    }

    fun deleteLocalFile(id: String) {
        viewModelScope.launch {
            repository.deleteLocalFile(id)
            showSnackbar("تم حذف الملف من القائمة")
        }
    }

    fun clearAllLocalFiles() {
        viewModelScope.launch {
            repository.clearLocalFiles()
            showSnackbar("تم مسح جميع الملفات المحلية")
        }
    }

    fun playLocalFile(file: LocalFileEntity) {
        val mediaType = LocalMediaScanner.resolveEntityMediaType(file)
        if (mediaType == "IMAGE") {
            showSnackbar("معاينة الصور متاحة مباشرة داخل تبويب الوسائط المحلية 🖼️")
            return
        }
        val localSong = Song(
            id = "local_${file.id}",
            title = file.displayName,
            arabicTitle = file.displayName,
            category = SongCategory.ALL,
            durationSeconds = 0,
            durationFormatted = file.durationFormatted,
            viewsCount = file.folderName,
            releaseYear = "بدون إنترنت",
            description = "ملف محلي • المجلد: ${file.folderName} • ${file.sizeFormatted}",
            lyrics = "اسم الملف:\n${file.displayName}\n\nالمجلد:\n${file.folderName}\n\nالنوع:\n${file.mimeType}\n\nالحجم:\n${file.sizeFormatted}",
            composer = "ملف محلي",
            lyricist = file.folderName,
            sourcePlatform = "الوسائط المحلية",
            isVideoAvailable = mediaType == "VIDEO",
            externalUrl = file.uriString,
            resolvedStreamUrl = file.uriString
        )
        playSong(localSong, expandPlayer = true)
    }

    // Playlists & Recent Actions
    fun createPlaylist(name: String, description: String = "") {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createPlaylist(name.trim(), description.trim())
            showSnackbar("تم إنشاء قائمة التشغيل: '$name' بنجاح 🎶")
        }
    }

    fun deletePlaylist(playlistId: String) {
        viewModelScope.launch {
            repository.deletePlaylist(playlistId)
            showSnackbar("تم حذف قائمة التشغيل")
        }
    }

    fun addSongToPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.addSongToPlaylist(playlistId, songId)
            val song = _uiState.value.allSongs.find { it.id == songId }
            showSnackbar("تمت إضافة '${song?.arabicTitle ?: "المقطع"}' إلى القائمة بنجاح ✅")
        }
    }

    fun removeSongFromPlaylist(playlistId: String, songId: String) {
        viewModelScope.launch {
            repository.removeSongFromPlaylist(playlistId, songId)
            showSnackbar("تمت إزالة المقطع من القائمة")
        }
    }

    fun playPlaylist(playlistId: String) {
        val songIds = _uiState.value.playlistItems
            .filter { it.playlistId == playlistId }
            .map { it.songId }
        val playlistSongs = songIds.mapNotNull { id -> _uiState.value.allSongs.find { it.id == id } }
        if (playlistSongs.isNotEmpty()) {
            playSong(playlistSongs.first(), expandPlayer = true)
        } else {
            showSnackbar("قائمة التشغيل فارغة، أضف مقاطع إليها أولاً")
        }
    }

    fun clearRecentHistory() {
        viewModelScope.launch {
            repository.clearRecentHistory()
            showSnackbar("تم مسح سجل الاستماع")
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionSyncJob?.cancel()
        streamResolutionJob?.cancel()
        bufferingWatchdogJob?.cancel()
        sleepTimerJob?.cancel()
        smartSearchJob?.cancel()
        groundedSearchJob?.cancel()
        localRefreshJob?.cancel()
        notificationCountdownJob?.cancel()
        mediaContentObserver?.let { observer ->
            runCatching { getApplication<Application>().contentResolver.unregisterContentObserver(observer) }
        }
        sessionPlayerListener?.let { listener ->
            _activePlayer.value?.removeListener(listener)
        }
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        _activePlayer.value = null
    }

    companion object {
        fun nextLowerQuality(current: PreferredQuality): PreferredQuality {
            return when (current) {
                PreferredQuality.AUTO -> PreferredQuality.MEDIUM
                PreferredQuality.HIGH -> PreferredQuality.MEDIUM
                PreferredQuality.MEDIUM -> PreferredQuality.STANDARD_480
                PreferredQuality.STANDARD_480 -> PreferredQuality.LOW_360
                PreferredQuality.LOW_360 -> PreferredQuality.DATA_SAVER
                PreferredQuality.DATA_SAVER -> PreferredQuality.DATA_SAVER
            }
        }

        fun mapPlaybackErrorToArabic(error: PlaybackException): String {
            return when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "تعذر الاتصال بالإنترنت حالياً، تأكد من الشبكة وأعد المحاولة"
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    "تعذر تشغيل الفيديو من المصدر حالياً، اضغط إعادة المحاولة"
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED ->
                    "تعذر قراءة هذا الفيديو، جرب خفض الجودة أو إعادة المحاولة"
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_DECODING_FAILED ->
                    "تعذر عرض الفيديو بهذه الجودة، جرب جودة أقل"
                else ->
                    "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
            }
        }
    }
}
