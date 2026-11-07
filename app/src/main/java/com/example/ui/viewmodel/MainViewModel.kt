package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.RahmaCatalog
import com.example.data.RahmaRepository
import com.example.data.local.AppDatabase
import com.example.model.AppNotification
import com.example.model.DownloadType
import com.example.model.DownloadedMedia
import com.example.model.Song
import com.example.model.SongCategory
import android.util.Log
import com.example.data.SettingsManager
import com.example.data.firebase.FirebaseRepository
import com.example.data.local.LocalFileEntity
import com.example.data.local.LocalMediaScanner
import com.example.model.AppAccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.PreferredQuality
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val isVideoMode: Boolean = true,
    val currentPositionSeconds: Int = 0,
    val playbackSpeed: Float = 1.0f,
    val isLooping: Boolean = false,
    val isExpanded: Boolean = false,
    val isLyricsExpanded: Boolean = false
)

data class UiState(
    val currentTab: ScreenTab = ScreenTab.HOME,
    val selectedCategory: SongCategory = SongCategory.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val songs: List<Song> = RahmaCatalog.songs,
    val favoriteIds: Set<String> = emptySet(),
    val downloads: List<DownloadedMedia> = emptyList(),
    val localFiles: List<LocalFileEntity> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val notifications: List<AppNotification> = emptyList(),
    val unreadNotificationsCount: Int = 0,
    val activeDownloadSongId: String? = null,
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

    private var playbackJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = RahmaRepository(application, database.appDao())

        // Initial setup
        viewModelScope.launch {
            repository.seedInitialNotificationsIfNeeded()
        }

        // Collect favorites from local Room database
        viewModelScope.launch {
            repository.getFavoriteSongIds().collectLatest { ids ->
                _uiState.update { it.copy(favoriteIds = ids) }
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

        // Collect downloads
        viewModelScope.launch {
            repository.getAllDownloads().collectLatest { downloadsList ->
                _uiState.update { it.copy(downloads = downloadsList) }
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

        // Collect settings
        viewModelScope.launch {
            settingsManager.settings.collectLatest { s ->
                _uiState.update { it.copy(settings = s) }
                if (s.autoRepeat) {
                    _playerState.update { it.copy(isLooping = true) }
                }
            }
        }

        // Collect local files and auto-scan if empty
        viewModelScope.launch {
            repository.getAllLocalFiles().collectLatest { files ->
                _uiState.update { it.copy(localFiles = files) }
                if (files.isEmpty() && settingsManager.settings.value.showLocalDeviceFiles) {
                    val scanned = LocalMediaScanner.scanAllDeviceMedia(application)
                    repository.saveLocalFiles(scanned)
                }
            }
        }
    }

    fun selectTab(tab: ScreenTab) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun selectCategory(category: SongCategory) {
        _uiState.update { current ->
            val filtered = if (current.searchQuery.isNotBlank()) {
                RahmaCatalog.searchSongs(current.searchQuery)
            } else {
                RahmaCatalog.filterByCategory(category)
            }
            current.copy(selectedCategory = category, songs = filtered)
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { current ->
            val filtered = if (query.isBlank()) {
                RahmaCatalog.filterByCategory(current.selectedCategory)
            } else {
                RahmaCatalog.searchSongs(query)
            }
            current.copy(searchQuery = query, songs = filtered)
        }
    }

    fun setSearchActive(active: Boolean) {
        _uiState.update {
            if (!active) {
                it.copy(
                    isSearchActive = false,
                    searchQuery = "",
                    songs = RahmaCatalog.filterByCategory(it.selectedCategory)
                )
            } else {
                it.copy(isSearchActive = true)
            }
        }
    }

    // Player Actions
    fun playSong(song: Song, expandPlayer: Boolean = true) {
        _playerState.update {
            it.copy(
                currentSong = song,
                isPlaying = true,
                currentPositionSeconds = 0,
                isExpanded = expandPlayer
            )
        }
        startPlaybackTimer(song.durationSeconds)
    }

    fun togglePlayPause() {
        val current = _playerState.value
        if (current.currentSong == null) return

        if (current.isPlaying) {
            playbackJob?.cancel()
            _playerState.update { it.copy(isPlaying = false) }
        } else {
            _playerState.update { it.copy(isPlaying = true) }
            startPlaybackTimer(current.currentSong.durationSeconds)
        }
    }

    fun seekTo(seconds: Int) {
        val current = _playerState.value
        val maxDuration = current.currentSong?.durationSeconds ?: 100
        val target = seconds.coerceIn(0, maxDuration)
        _playerState.update { it.copy(currentPositionSeconds = target) }
    }

    fun skipForward10() {
        val current = _playerState.value
        val maxDuration = current.currentSong?.durationSeconds ?: 100
        seekTo((current.currentPositionSeconds + 10).coerceAtMost(maxDuration))
    }

    fun skipBackward10() {
        val current = _playerState.value
        seekTo((current.currentPositionSeconds - 10).coerceAtLeast(0))
    }

    fun setExpanded(expanded: Boolean) {
        _playerState.update { it.copy(isExpanded = expanded) }
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
        _playerState.update { it.copy(playbackSpeed = speeds[nextIndex]) }
    }

    fun toggleLoop() {
        _playerState.update { it.copy(isLooping = !it.isLooping) }
    }

    fun playNextSong() {
        val current = _playerState.value.currentSong ?: return
        val allSongs = RahmaCatalog.songs
        val currentIndex = allSongs.indexOfFirst { it.id == current.id }
        if (currentIndex != -1 && currentIndex + 1 < allSongs.size) {
            playSong(allSongs[currentIndex + 1], expandPlayer = _playerState.value.isExpanded)
        } else if (allSongs.isNotEmpty()) {
            playSong(allSongs[0], expandPlayer = _playerState.value.isExpanded)
        }
    }

    fun playPreviousSong() {
        val current = _playerState.value.currentSong ?: return
        val allSongs = RahmaCatalog.songs
        val currentIndex = allSongs.indexOfFirst { it.id == current.id }
        if (currentIndex > 0) {
            playSong(allSongs[currentIndex - 1], expandPlayer = _playerState.value.isExpanded)
        }
    }

    fun closePlayer() {
        playbackJob?.cancel()
        _playerState.update {
            PlayerUiState(currentSong = null, isPlaying = false, isExpanded = false)
        }
    }

    private fun startPlaybackTimer(totalDurationSeconds: Int) {
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val currentSec = _playerState.value.currentPositionSeconds
                if (currentSec >= totalDurationSeconds) {
                    if (_playerState.value.isLooping || _uiState.value.settings.autoRepeat) {
                        _playerState.update { it.copy(currentPositionSeconds = 0) }
                    } else if (_uiState.value.settings.autoPlayNext) {
                        playNextSong()
                        break
                    } else {
                        _playerState.update { it.copy(isPlaying = false) }
                        break
                    }
                } else {
                    _playerState.update { it.copy(currentPositionSeconds = currentSec + 1) }
                }
            }
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

    // Downloads
    fun promptDownload(song: Song) {
        _uiState.update { it.copy(songToDownload = song) }
    }

    fun dismissDownloadPrompt() {
        _uiState.update { it.copy(songToDownload = null) }
    }

    fun startDownload(song: Song, type: DownloadType) {
        _uiState.update {
            it.copy(
                songToDownload = null,
                activeDownloadSongId = song.id,
                activeDownloadProgress = 0
            )
        }

        viewModelScope.launch {
            repository.startDownload(song, type)
                .catch {
                    _uiState.update {
                        it.copy(
                            activeDownloadSongId = null,
                            activeDownloadProgress = 0
                        )
                    }
                    showSnackbar("فشل التنزيل، يرجى المحاولة لاحقاً")
                }
                .collect { progress ->
                    _uiState.update { it.copy(activeDownloadProgress = progress) }
                    if (progress == 100) {
                        delay(500)
                        _uiState.update {
                            it.copy(
                                activeDownloadSongId = null,
                                activeDownloadProgress = 0
                            )
                        }
                        showSnackbar("تم تنزيل '${song.arabicTitle}' بنجاح! متاح للاستماع أوفلاين 🎉")
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

    // Notifications
    fun markNotificationAsRead(notification: AppNotification) {
        viewModelScope.launch {
            repository.markAsRead(notification.id)
            if (notification.songId != null) {
                val song = RahmaCatalog.getSongById(notification.songId)
                if (song != null) {
                    playSong(song, expandPlayer = true)
                }
            }
        }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // Settings actions
    fun updateThemeMode(mode: AppThemeMode) {
        settingsManager.updateThemeMode(mode)
    }

    fun updateAccentColor(accent: AppAccentColor) {
        settingsManager.updateAccentColor(accent)
    }

    fun updateShowLocalFiles(show: Boolean) {
        settingsManager.updateShowLocalFiles(show)
        if (show) {
            scanDeviceMedia()
        }
    }

    fun scanDeviceMedia() {
        viewModelScope.launch {
            val scanned = LocalMediaScanner.scanAllDeviceMedia(getApplication())
            repository.saveLocalFiles(scanned)
            showSnackbar("تم فحص وتحديث وسائط الجهاز تلقائياً (${scanned.size} ملف) 📱")
        }
    }

    fun updateAutoPlayNext(enabled: Boolean) {
        settingsManager.updateAutoPlayNext(enabled)
    }

    fun updateRememberPosition(enabled: Boolean) {
        settingsManager.updateRememberPosition(enabled)
    }

    fun updateQuality(quality: PreferredQuality) {
        settingsManager.updateQuality(quality)
    }

    fun updateAutoRepeat(enabled: Boolean) {
        settingsManager.updateAutoRepeat(enabled)
        _playerState.update { it.copy(isLooping = enabled) }
    }

    fun updateLyricsSync(enabled: Boolean) {
        settingsManager.updateLyricsSync(enabled)
    }

    fun updateWifiOnly(enabled: Boolean) {
        settingsManager.updateWifiOnly(enabled)
    }

    fun updateNotifications(enabled: Boolean) {
        settingsManager.updateNotifications(enabled)
    }

    fun updateVibrate(enabled: Boolean) {
        settingsManager.updateVibrate(enabled)
    }

    private var sleepTimerJob: Job? = null

    fun updateSleepTimer(minutes: Int) {
        settingsManager.updateSleepTimer(minutes)
        sleepTimerJob?.cancel()
        if (minutes > 0) {
            sleepTimerJob = viewModelScope.launch {
                delay(minutes * 60 * 1000L)
                _playerState.update { it.copy(isPlaying = false) }
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
                cacheDir.deleteRecursively()
                cacheDir.mkdirs()
                showSnackbar("تم تنظيف ذاكرة التخزين المؤقت بنجاح 🧹")
            } catch (e: Exception) {
                showSnackbar("تم فحص وتنظيف الذاكرة المؤقتة")
            }
        }
    }

    // Local files actions
    fun importLocalFiles(files: List<LocalFileEntity>) {
        viewModelScope.launch {
            repository.saveLocalFiles(files)
            showSnackbar("تم استيراد ${files.size} ملف من ذاكرة الهاتف بنجاح 📁")
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
        val localSong = Song(
            id = "local_${file.id}",
            title = file.displayName,
            arabicTitle = file.displayName,
            category = SongCategory.ALL,
            durationSeconds = 180,
            durationFormatted = file.durationFormatted,
            viewsCount = "ملف محلي",
            releaseYear = "الهاتف",
            description = "ملف من ذاكرة الجهاز: ${file.sizeFormatted}",
            lyrics = "ملف صوتي/مرئي مستورد من جهازك الشخصي.\n\nاسم الملف:\n${file.displayName}\n\nنوع المحتوى:\n${file.mimeType}\n\nالحجم:\n${file.sizeFormatted}\n\nالمسار:\n${file.uriString}",
            composer = "ملف محلي",
            lyricist = "ذاكرة الجهاز",
            isVideoAvailable = file.mimeType.startsWith("video")
        )
        playSong(localSong, expandPlayer = true)
    }

    override fun onCleared() {
        super.onCleared()
        playbackJob?.cancel()
        sleepTimerJob?.cancel()
    }
}
