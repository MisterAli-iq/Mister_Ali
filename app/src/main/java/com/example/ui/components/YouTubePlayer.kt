package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
import com.example.data.local.PlaylistEntity
import com.example.model.ReelComment
import kotlinx.coroutines.flow.Flow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.example.R
import com.example.data.RahmaCatalog
import com.example.model.PlayerEngineMode
import com.example.model.PreferredQuality
import com.example.model.Song
import com.example.model.VideoAspectMode
import com.example.ui.theme.DarkChipBg
import com.example.ui.theme.YouTubeBlack
import com.example.ui.theme.YouTubeBorder
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeSurfaceVariant
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary

@Composable
fun YouTubePlayer(
    song: Song,
    exoPlayer: Player? = null,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    isResolvingStream: Boolean = false,
    playbackError: String? = null,
    recoveryStatusMessage: String? = null,
    fallbackEmbedUrl: String? = null,
    useWebFallback: Boolean = false,
    snifferTargetUrl: String? = null,
    isFullscreen: Boolean = false,
    activeQuality: PreferredQuality = PreferredQuality.HIGH,
    activeEngineMode: PlayerEngineMode = PlayerEngineMode.EXOPLAYER,
    isVideoMode: Boolean,
    currentPositionSeconds: Int,
    durationSeconds: Int,
    playbackSpeed: Float,
    isLooping: Boolean,
    isLyricsExpanded: Boolean,
    isFavorite: Boolean,
    videoAspectMode: VideoAspectMode = VideoAspectMode.FIT,
    playlists: List<PlaylistEntity> = emptyList(),
    upNextSongs: List<Song> = emptyList(),
    likedIds: Set<String> = emptySet(),
    commentsFlow: Flow<List<ReelComment>>? = null,
    onClose: () -> Unit,
    onToggleFullscreen: () -> Unit = {},
    onRetryPlayback: () -> Unit = {},
    onChangeQuality: (PreferredQuality) -> Unit = {},
    onLowerQuality: () -> Unit = {},
    onClearCacheAndRetry: () -> Unit = {},
    onSwitchAlternativePlayer: () -> Unit = {},
    onStreamCaptured: (String, Map<String, String>) -> Unit = { _, _ -> },
    onSnifferFailed: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Int) -> Unit,
    onSkipForward: () -> Unit,
    onSkipBackward: () -> Unit,
    onPlayNext: () -> Unit,
    onPlayPrevious: () -> Unit,
    onToggleVideoAudio: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleLoop: () -> Unit,
    onToggleLyrics: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onToggleLike: (String) -> Unit = {},
    onAddComment: (String, String) -> Unit = { _, _ -> },
    onOpenExternalUrl: (String) -> Unit = {},
    onToggleFeatured: () -> Unit = {},
    onDownloadClick: () -> Unit,
    onAddToPlaylist: (playlistId: String, songId: String) -> Unit = { _, _ -> },
    onCreatePlaylist: (name: String) -> Unit = {},
    onSelectUpNextSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isSubscribed by remember { mutableStateOf(true) }
    var selectedPlayerTab by remember { mutableIntStateOf(0) } // 0: Up Next, 1: Lyrics, 2: Info, 3: Comments
    var lyricsFontSizeSp by remember { mutableStateOf(16) }
    var lyricsCopied by remember { mutableStateOf(false) }
    var isCleanScreenMode by remember { mutableStateOf(false) }
    var showVideoOverlayControls by remember { mutableStateOf(true) }
    var showPlaylistDialog by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var isCreatingNewPlaylistInDialog by remember { mutableStateOf(false) }
    var newCommentText by remember { mutableStateOf("") }
    val songComments by remember(song.id, commentsFlow) {
        commentsFlow ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }.collectAsState(initial = emptyList())
    val isSongLiked = likedIds.contains(song.id)

    val effectiveFullscreen = isFullscreen || isCleanScreenMode

    // System Back Handler: exits fullscreen/clean mode if active, else collapses player
    BackHandler {
        when {
            isCleanScreenMode -> isCleanScreenMode = false
            isFullscreen -> onToggleFullscreen()
            else -> onClose()
        }
    }

    // Infinite rotation for vinyl disc in audio mode
    val infiniteTransition = rememberInfiniteTransition(label = "player_anim")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rotation"
    )

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeBlack)
            .let { if (effectiveFullscreen) it else it.statusBarsPadding() },
        color = YouTubeBlack
    ) {
        if (effectiveFullscreen) {
            // Fullscreen Built-in Video Player with Play, Pause, Seek, Quality & Exit Fullscreen overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showVideoOverlayControls = !showVideoOverlayControls }
                    .testTag("player_fullscreen_container")
            ) {
                NativeMediaPlayerView(
                    player = exoPlayer,
                    isResolvingStream = isResolvingStream,
                    isBuffering = isBuffering,
                    errorMessage = playbackError,
                    recoveryStatusMessage = recoveryStatusMessage,
                    fallbackEmbedUrl = fallbackEmbedUrl,
                    useWebFallback = useWebFallback,
                    snifferTargetUrl = snifferTargetUrl,
                    thumbnailUrl = song.thumbnailUrl,
                    contentScale = if (videoAspectMode == VideoAspectMode.CROP) ContentScale.Crop else ContentScale.Fit,
                    onRetry = onRetryPlayback,
                    onLowerQuality = onLowerQuality,
                    onClearCache = onClearCacheAndRetry,
                    onSwitchAlternativePlayer = onSwitchAlternativePlayer,
                    onStreamCaptured = onStreamCaptured,
                    onSnifferFailed = onSnifferFailed,
                    modifier = Modifier.fillMaxSize()
                )

                if (!useWebFallback && (showVideoOverlayControls || !isPlaying) && !isResolvingStream && !isBuffering && playbackError == null) {
                    // Top bar in Fullscreen: Title + Quality + Exit Fullscreen
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                            .align(Alignment.TopCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = song.arabicTitle,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0x99000000))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color(0xBB0A0A0A))
                                    .border(0.8.dp, YouTubeGold.copy(alpha = 0.65f), RoundedCornerShape(18.dp))
                                    .clickable { showQualityDialog = true }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                                    .testTag("player_overlay_quality_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = "تغيير الجودة",
                                        tint = YouTubeGold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${activeQuality.maxHeightPx}p",
                                        color = YouTubeGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    if (isCleanScreenMode) isCleanScreenMode = false
                                    if (isFullscreen) onToggleFullscreen()
                                },
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xBB0A0A0A))
                                    .testTag("player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FullscreenExit,
                                    contentDescription = "خروج من ملء الشاشة",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Center Play/Pause + ±10s controls
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onSkipBackward,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "تأخير 10 ثوان",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        IconButton(
                            onClick = onTogglePlayPause,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                                .background(YouTubeRed)
                                .testTag("player_center_play_pause")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        IconButton(
                            onClick = onSkipForward,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(0x88000000))
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "تقديم 10 ثوان",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Bottom Seekbar in Fullscreen
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .background(Color(0x99000000))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Slider(
                            value = currentPositionSeconds.toFloat(),
                            onValueChange = { onSeekTo(it.toInt()) },
                            valueRange = 0f..durationSeconds.toFloat().coerceAtLeast(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = YouTubeRed,
                                activeTrackColor = YouTubeRed,
                                inactiveTrackColor = Color(0xFF555555)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val totalText = if (durationSeconds > 0) {
                                String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60)
                            } else song.durationFormatted
                            Text(
                                text = String.format("%d:%02d", currentPositionSeconds / 60, currentPositionSeconds % 60),
                                color = Color.White,
                                fontSize = 11.sp
                            )
                            Text(
                                text = totalText,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else if (!useWebFallback && isCleanScreenMode) {
                    // Subtle floating exit button if controls are hidden in clean mode
                    Box(
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(14.dp)
                            .align(Alignment.TopEnd)
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xCC0A0A0A))
                            .border(1.dp, YouTubeGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                            .clickable { isCleanScreenMode = false }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("exit_clean_screen_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "خروج من الشاشة الصافية",
                                tint = YouTubeGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "إظهار أزرار التحكم",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                // Top Bar with Minimize Button & Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("player_minimize_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "تصغير المشغل",
                            tint = YouTubeTextPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Video / Audio Toggle Switch (like YouTube Music / YouTube)
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(YouTubeSurfaceVariant)
                                .padding(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isVideoMode) YouTubeRed else Color.Transparent)
                                    .clickable { if (!isVideoMode) onToggleVideoAudio() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Videocam,
                                        contentDescription = null,
                                        tint = if (isVideoMode) Color.White else YouTubeTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "فيديو",
                                        color = if (isVideoMode) Color.White else YouTubeTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (!isVideoMode) YouTubeRed else Color.Transparent)
                                    .clickable { if (isVideoMode) onToggleVideoAudio() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (!isVideoMode) Color.White else YouTubeTextSecondary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "صوت",
                                        color = if (!isVideoMode) Color.White else YouTubeTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Clean Screen Mode Button (شاشة صافية)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(YouTubeSurfaceVariant)
                                .clickable { isCleanScreenMode = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                .testTag("clean_screen_toggle_button")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "شاشة صافية",
                                    tint = YouTubeGold,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "شاشة صافية",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Developer Brand Icon
                    Text(
                        text = "✪Mister Ali",
                        color = YouTubeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
                    .clickable { showVideoOverlayControls = !showVideoOverlayControls }
            ) {
                if (isVideoMode) {
                    // Built-in Media3 ExoPlayer Stream Player + Hidden WebView Stream Capture Backup
                    NativeMediaPlayerView(
                        player = exoPlayer,
                        isResolvingStream = isResolvingStream,
                        isBuffering = isBuffering,
                        errorMessage = playbackError,
                        recoveryStatusMessage = recoveryStatusMessage,
                        fallbackEmbedUrl = fallbackEmbedUrl,
                        useWebFallback = useWebFallback,
                        snifferTargetUrl = snifferTargetUrl,
                        thumbnailUrl = song.thumbnailUrl,
                        contentScale = if (videoAspectMode == VideoAspectMode.CROP) ContentScale.Crop else ContentScale.Fit,
                        onRetry = onRetryPlayback,
                        onLowerQuality = onLowerQuality,
                        onClearCache = onClearCacheAndRetry,
                        onSwitchAlternativePlayer = onSwitchAlternativePlayer,
                        onStreamCaptured = onStreamCaptured,
                        onSnifferFailed = onSnifferFailed,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Audio Mode Canvas: Glowing vinyl disk with artist avatar
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color(song.gradientStartHex),
                                        Color(0xFF140509),
                                        Color.Black
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .rotate(if (isPlaying) rotationAngle else 0f)
                                .clip(CircleShape)
                                .border(4.dp, Color(0xFF333333), CircleShape)
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.rahma_avatar),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                            )
                        }
                    }
                }

                // Center playback control overlays (hidden in silent web fallback so embedded controls remain uncluttered)
                if (!useWebFallback && (!isVideoMode || !isPlaying || showVideoOverlayControls)) {
                    if (!isResolvingStream && !isBuffering && playbackError == null) {
                        Row(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onSkipBackward,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x77000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "تأخير 10 ثوان",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(YouTubeRed)
                                    .testTag("player_center_play_pause")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "إيقاف مؤقت" else "تشغيل",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            IconButton(
                                onClick = onSkipForward,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x77000000))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "تقديم 10 ثوان",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    // Bottom-end overlay inside video box: Quality pill + Dedicated Fullscreen button
                    if (isVideoMode && !isResolvingStream && playbackError == null) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xAA000000))
                                    .border(0.6.dp, YouTubeGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                                    .clickable { showQualityDialog = true }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("player_overlay_quality_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${activeQuality.maxHeightPx}p",
                                    color = YouTubeGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onToggleFullscreen,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xAA000000))
                                    .testTag("player_fullscreen_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "ملء الشاشة",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Seekbar & Timing Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 2.dp)
            ) {
                Slider(
                    value = currentPositionSeconds.toFloat(),
                    onValueChange = { onSeekTo(it.toInt()) },
                    valueRange = 0f..durationSeconds.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = YouTubeRed,
                        activeTrackColor = YouTubeRed,
                        inactiveTrackColor = Color(0xFF444444)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .testTag("player_seek_slider")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentMins = currentPositionSeconds / 60
                    val currentSecs = currentPositionSeconds % 60
                    val currentFormatted = String.format("%d:%02d", currentMins, currentSecs)

                    val totalText = if (durationSeconds > 0) {
                        String.format("%d:%02d", durationSeconds / 60, durationSeconds % 60)
                    } else {
                        song.durationFormatted
                    }

                    Text(
                        text = currentFormatted,
                        color = YouTubeTextSecondary,
                        fontSize = 11.sp
                    )

                    Text(
                        text = totalText,
                        color = YouTubeTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            // Scrollable Content Below Video (Info, Controls, Channel, Lyrics, Up Next)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                // Song Title & Native Playback Card (Zero YouTube Branding)
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    val displayTitle = if (song.title.isNotBlank() && song.title != song.arabicTitle) {
                        "${song.arabicTitle} (${song.title})"
                    } else {
                        song.arabicTitle
                    }
                    Text(
                        text = displayTitle,
                        color = YouTubeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val isOfficialSong = RahmaCatalog.isOfficialRahmaChannel(song)
                    val realChannelDisplay = RahmaCatalog.displayChannelName(song)
                    val resolvedSourceLink = song.externalUrl?.takeIf { it.isNotBlank() }
                        ?: if (song.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${song.youtubeVideoId}" else null

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.rahma_avatar),
                            contentDescription = realChannelDisplay,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .border(1.dp, YouTubeGold.copy(alpha = 0.6f), CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isOfficialSong) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = "قناة موثقة",
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        val metaParts = listOfNotNull(
                            realChannelDisplay,
                            song.channelHandle.takeIf { it.isNotBlank() && !realChannelDisplay.contains(it, ignoreCase = true) },
                            song.publishDate.takeIf { it.isNotBlank() },
                            song.viewsCount.takeIf { it.isNotBlank() && it != song.sourcePlatform && it != realChannelDisplay }
                        ).distinct()

                        Text(
                            text = metaParts.joinToString(" • "),
                            color = if (isOfficialSong) YouTubeGold else YouTubeTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isOfficialSong) FontWeight.SemiBold else FontWeight.Normal,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!resolvedSourceLink.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1A1E24))
                                .border(0.6.dp, Color(0xFF395066), RoundedCornerShape(8.dp))
                                .clickable { onOpenExternalUrl(resolvedSourceLink) }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("player_external_link_badge")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Link,
                                contentDescription = "فتح الرابط في المتصفح",
                                tint = YouTubeGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = resolvedSourceLink,
                                color = Color(0xFF64B5F6),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "فتح في Chrome",
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Native App Player Status Strip
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF333333))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(YouTubeRed),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                val publisherHandle = song.channelHandle
                                    .takeIf { it.isNotBlank() && !realChannelDisplay.contains(it, ignoreCase = true) }
                                val publisherDomain = resolvedSourceLink
                                    ?.let { runCatching { Uri.parse(it).host?.removePrefix("www.") }.getOrNull() }
                                    ?.takeIf { it.isNotBlank() }
                                val publisherDetails = listOfNotNull(
                                    publisherHandle,
                                    publisherDomain,
                                    song.publishDate.takeIf { it.isNotBlank() }
                                ).distinct().joinToString(" • ")

                                Column {
                                    Text(
                                        text = "الناشر: $realChannelDisplay",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.testTag("player_real_publisher_name")
                                    )
                                    if (publisherDetails.isNotBlank()) {
                                        Text(
                                            text = publisherDetails,
                                            color = YouTubeTextSecondary,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E2000))
                                    .border(0.5.dp, YouTubeGold.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (isVideoMode) "فيديو HD" else "صوت نقي",
                                    color = YouTubeGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Action Buttons Row (Like, Featured, Download, Share, Playlist, Lyrics, Speed, Loop)
                item {
                    val actionScrollState = rememberScrollState()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(actionScrollState)
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Persistent Like Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onToggleLike(song.id) }
                                .padding(horizontal = 4.dp)
                                .testTag("player_like_button")
                        ) {
                            Icon(
                                imageVector = if (isSongLiked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                contentDescription = "إعجاب",
                                tint = if (isSongLiked) YouTubeGold else YouTubeTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isSongLiked) "أعجبني 👍" else "إعجاب",
                                color = if (isSongLiked) YouTubeGold else YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Favorite / Heart
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onFavoriteToggle() }
                                .padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) YouTubeRed else YouTubeTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFavorite) "بالمفضلة" else "مفضلة",
                                color = if (isFavorite) YouTubeRed else YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Featured / Star Pin
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onToggleFeatured() }
                                .padding(horizontal = 4.dp)
                                .testTag("player_toggle_featured_button")
                        ) {
                            Icon(
                                imageVector = if (song.isFeatured) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "تمييز الأغنية",
                                tint = if (song.isFeatured) YouTubeGold else YouTubeTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (song.isFeatured) "مميزة ⭐" else "تمييز",
                                color = if (song.isFeatured) YouTubeGold else YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Download
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onDownloadClick() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = YouTubeTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "تحميل",
                                color = YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Share
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { ShareHelper.shareSong(context, song) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = YouTubeTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مشاركة",
                                color = YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Add to Playlist Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showPlaylistDialog = true }
                                .testTag("player_add_playlist_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "إضافة لقائمة",
                                tint = YouTubeGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "قائمة",
                                color = YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Lyrics Quick Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { selectedPlayerTab = 1 }
                                .testTag("player_lyrics_quick_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = "كلمات الأغنية",
                                tint = if (selectedPlayerTab == 1) YouTubeRed else YouTubeTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "الكلمات",
                                color = if (selectedPlayerTab == 1) YouTubeRed else YouTubeTextSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (selectedPlayerTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }

                        // Speed
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onCycleSpeed() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = YouTubeGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${playbackSpeed}x",
                                color = YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Loop
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onToggleLoop() }
                        ) {
                            Icon(
                                imageVector = if (isLooping) Icons.Default.RepeatOne else Icons.Default.Repeat,
                                contentDescription = null,
                                tint = if (isLooping) YouTubeRed else YouTubeTextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isLooping) "مكرر" else "تكرار",
                                color = if (isLooping) YouTubeRed else YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Quality Selector
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { showQualityDialog = true }
                                .testTag("player_quality_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = "تغيير الجودة",
                                tint = YouTubeGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${activeQuality.maxHeightPx}p",
                                color = YouTubeGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Clear Cache & Retry
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onClearCacheAndRetry() }
                                .testTag("player_clear_cache_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.CleaningServices,
                                contentDescription = "مسح الكاش",
                                tint = YouTubeTextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "مسح الكاش",
                                color = YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        // Switch Alternative Player Engine
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onSwitchAlternativePlayer() }
                                .testTag("player_switch_engine_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "التبديل لمشغل بديل",
                                tint = if (activeEngineMode == PlayerEngineMode.WEB_ALTERNATIVE) YouTubeRed else YouTubeGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (activeEngineMode == PlayerEngineMode.WEB_ALTERNATIVE) "مشغل أصلي" else "مشغل بديل",
                                color = if (activeEngineMode == PlayerEngineMode.WEB_ALTERNATIVE) YouTubeRed else YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = YouTubeBorder, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // YouTube-style TabRow: [Up Next | Lyrics | Info]
                item {
                    TabRow(
                        selectedTabIndex = selectedPlayerTab,
                        containerColor = YouTubeBlack,
                        contentColor = Color.White,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedPlayerTab]),
                                color = YouTubeRed
                            )
                        },
                        divider = {
                            HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("player_tabs_row")
                    ) {
                        Tab(
                            selected = selectedPlayerTab == 0,
                            onClick = { selectedPlayerTab = 0 },
                            text = {
                                Text(
                                    text = "التالي",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedPlayerTab == 0) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPlayerTab == 0) Color.White else YouTubeTextSecondary
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = null,
                                    tint = if (selectedPlayerTab == 0) YouTubeRed else YouTubeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_up_next")
                        )

                        Tab(
                            selected = selectedPlayerTab == 1,
                            onClick = { selectedPlayerTab = 1 },
                            text = {
                                Text(
                                    text = "الكلمات (Lyrics)",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedPlayerTab == 1) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPlayerTab == 1) Color.White else YouTubeTextSecondary
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = if (selectedPlayerTab == 1) YouTubeRed else YouTubeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_lyrics")
                        )

                        Tab(
                            selected = selectedPlayerTab == 2,
                            onClick = { selectedPlayerTab = 2 },
                            text = {
                                Text(
                                    text = "معلومات",
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedPlayerTab == 2) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPlayerTab == 2) Color.White else YouTubeTextSecondary
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = if (selectedPlayerTab == 2) YouTubeRed else YouTubeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_info")
                        )

                        Tab(
                            selected = selectedPlayerTab == 3,
                            onClick = { selectedPlayerTab = 3 },
                            text = {
                                Text(
                                    text = "التعليقات (${songComments.size})",
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedPlayerTab == 3) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedPlayerTab == 3) Color.White else YouTubeTextSecondary
                                )
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (selectedPlayerTab == 3) YouTubeRed else YouTubeTextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag("tab_comments")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // TAB 1: DEDICATED LYRICS TAB
                if (selectedPlayerTab == 1) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("lyrics_container_card"),
                            colors = CardDefaults.cardColors(containerColor = YouTubeSurface),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Header: Song Title & Fetch Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = YouTubeRed,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "كلمات: «${song.arabicTitle}»",
                                                color = YouTubeTextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = "غناء: رحمة رياض • كلمات: ${song.lyricist} • ألحان: ${song.composer}",
                                            color = YouTubeGold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1B5E20))
                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "كلمات موثقة ✓",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = YouTubeBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                // Lyrics Tools Bar: Copy, Share, Font Size (A- / A+)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Copy Button
                                        Button(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(song.lyrics))
                                                lyricsCopied = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (lyricsCopied) Color(0xFF2E7D32) else DarkChipBg
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            modifier = Modifier
                                                .height(32.dp)
                                                .testTag("copy_lyrics_button")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (lyricsCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (lyricsCopied) "تم النسخ ✓" else "نسخ الكلمات",
                                                    color = Color.White,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Share Lyrics Button
                                        IconButton(
                                            onClick = {
                                                val shareLyricsText = """
🎵 كلمات رائعة الفنانة رحمة رياض:
« ${song.arabicTitle} »

${song.lyrics}

━━━━━━━━━━━━━━━━━━━━
تطبيق RahmaRiad • إشراف ✪Mister Ali
━━━━━━━━━━━━━━━━━━━━
                                                """.trimIndent()
                                                val intent = android.content.Intent().apply {
                                                    action = android.content.Intent.ACTION_SEND
                                                    putExtra(android.content.Intent.EXTRA_TEXT, shareLyricsText)
                                                    type = "text/plain"
                                                }
                                                val chooser = android.content.Intent.createChooser(intent, "مشاركة كلمات ${song.arabicTitle} عبر:")
                                                chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(chooser)
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Share,
                                                contentDescription = "مشاركة الكلمات",
                                                tint = YouTubeTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    // Font Size Adjusters
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(DarkChipBg)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(
                                            onClick = { if (lyricsFontSizeSp > 12) lyricsFontSizeSp -= 2 },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text(
                                                text = "A-",
                                                color = YouTubeTextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Text(
                                            text = "${lyricsFontSizeSp}",
                                            color = YouTubeTextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        )

                                        IconButton(
                                            onClick = { if (lyricsFontSizeSp < 26) lyricsFontSizeSp += 2 },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Text(
                                                text = "A+",
                                                color = YouTubeTextSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Interactive synchronized verses list
                                val lines = song.lyrics.lines().filter { it.isNotBlank() }
                                val activeLineIndex = if (lines.isNotEmpty() && durationSeconds > 0) {
                                    val lineDuration = (durationSeconds.toFloat() / lines.size).coerceAtLeast(4f)
                                    (currentPositionSeconds / lineDuration.toInt()).coerceIn(0, lines.size - 1)
                                } else 0

                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    lines.forEachIndexed { index, line ->
                                        val isActive = index == activeLineIndex
                                        val lineTargetSeconds = (index * (durationSeconds.toFloat() / lines.size)).toInt()

                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                    if (isActive) Color(0x33FF0033) else Color.Transparent
                                                )
                                                .border(
                                                    width = if (isActive) 1.dp else 0.dp,
                                                    color = if (isActive) YouTubeRed.copy(alpha = 0.6f) else Color.Transparent,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable { onSeekTo(lineTargetSeconds) }
                                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (isActive) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .clip(CircleShape)
                                                            .background(YouTubeRed)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                }

                                                Text(
                                                    text = line,
                                                    color = if (isActive) Color.White else YouTubeTextPrimary.copy(alpha = 0.8f),
                                                    fontSize = lyricsFontSizeSp.sp,
                                                    fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                                                    lineHeight = (lyricsFontSizeSp + 10).sp,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                if (isActive) {
                                                    Text(
                                                        text = "▶",
                                                        color = YouTubeRed,
                                                        fontSize = 10.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = YouTubeBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "المصدر: أرشيف الفنانة رحمة رياض الرسمي",
                                        color = YouTubeTextSecondary,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "تطبيق ✪Mister Ali",
                                        color = YouTubeGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 0: UP NEXT PLAYLIST TAB
                if (selectedPlayerTab == 0) {
                    item {
                        Text(
                            text = "الأغاني التالية في القائمة",
                            color = YouTubeTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    val filteredUpNext = upNextSongs.filter { it.id != song.id }
                    items(filteredUpNext) { nextSong ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectUpNextSong(nextSong) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp, 52.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF222222))
                            ) {
                                if (!nextSong.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = nextSong.thumbnailUrl,
                                        contentDescription = nextSong.arabicTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        error = painterResource(id = R.drawable.rahma_hero_banner)
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.rahma_hero_banner),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        alpha = 0.6f
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(3.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xDD000000))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = nextSong.durationFormatted,
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nextSong.arabicTitle,
                                    color = YouTubeTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                val nextChannel = RahmaCatalog.displayChannelName(nextSong)
                                Text(
                                    text = buildString {
                                        append(nextChannel)
                                        if (nextSong.viewsCount.isNotBlank() && nextSong.viewsCount != nextChannel) {
                                            append(" • ")
                                            append(nextSong.viewsCount)
                                        }
                                    },
                                    color = if (RahmaCatalog.isOfficialRahmaChannel(nextSong)) YouTubeGold else YouTubeTextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // TAB 2: INFO & DETAILS TAB
                if (selectedPlayerTab == 2) {
                    // Channel Card
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(YouTubeSurface)
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.rahma_avatar),
                                contentDescription = "قناة رحمة رياض",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            val isOfficialChannelCard = RahmaCatalog.isOfficialRahmaChannel(song)
                            val cardChannelTitle = RahmaCatalog.displayChannelName(song)
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = cardChannelTitle,
                                        color = YouTubeTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isOfficialChannelCard) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "قناة موثقة",
                                            tint = Color(0xFF64B5F6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = if (isOfficialChannelCard) {
                                        "القناة الرسمية الرئيسية • @RahmaRiad"
                                    } else {
                                        listOfNotNull(
                                            song.channelHandle.takeIf { it.isNotBlank() },
                                            "قناة حقيقية على المنصة"
                                        ).joinToString(" • ")
                                    },
                                    color = if (isOfficialChannelCard) YouTubeGold else YouTubeTextSecondary,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { isSubscribed = !isSubscribed },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSubscribed) DarkChipBg else YouTubeRed
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = if (isSubscribed) "مشترك ✓" else "اشتراك",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Song Description Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = YouTubeSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "عن العمل:",
                                    color = YouTubeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = song.description,
                                    color = YouTubeTextSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = YouTubeBorder, thickness = 0.5.dp)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("تاريخ الإصدار", color = YouTubeTextSecondary, fontSize = 11.sp)
                                        Text(song.releaseYear, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("ألحان", color = YouTubeTextSecondary, fontSize = 11.sp)
                                        Text(song.composer, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Column {
                                        Text("كلمات", color = YouTubeTextSecondary, fontSize = 11.sp)
                                        Text(song.lyricist, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Dedicated Developer Credit Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            Color(0xFF220505),
                                            Color(0xFF1E1E1E),
                                            Color(0xFF151000)
                                        )
                                    )
                                )
                                .border(1.dp, YouTubeGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column {
                                    Text(
                                        text = "حقوق وتصميم وتطوير التطبيق",
                                        color = YouTubeTextSecondary,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = "✪Mister Ali",
                                        color = YouTubeGold,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(YouTubeGold.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Official Edition",
                                        color = YouTubeGold,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                // TAB 3: REAL PERSISTED COMMENTS TAB
                if (selectedPlayerTab == 3) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("player_comments_card"),
                            colors = CardDefaults.cardColors(containerColor = YouTubeSurface),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "التعليقات (${songComments.size})",
                                    color = YouTubeTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = newCommentText,
                                        onValueChange = { newCommentText = it },
                                        placeholder = {
                                            Text("أضف تعليقك...", color = YouTubeTextSecondary, fontSize = 12.sp)
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(20.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = YouTubeRed,
                                            unfocusedBorderColor = Color(0xFF444444),
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedContainerColor = Color(0xFF202020),
                                            unfocusedContainerColor = Color(0xFF181818)
                                        ),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (newCommentText.isNotBlank()) {
                                                onAddComment(song.id, newCommentText)
                                                newCommentText = ""
                                            }
                                        },
                                        enabled = newCommentText.isNotBlank(),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "إرسال التعليق",
                                            tint = if (newCommentText.isNotBlank()) YouTubeRed else Color.Gray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                if (songComments.isEmpty()) {
                                    Text(
                                        text = "لا توجد تعليقات بعد. كن أول من يعلّق!",
                                        color = YouTubeTextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    songComments.forEach { comment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(30.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2E2000)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = comment.authorName.take(1),
                                                    color = YouTubeGold,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${comment.authorName} • ${comment.timeAgo}",
                                                    color = YouTubeTextSecondary,
                                                    fontSize = 10.sp
                                                )
                                                Text(
                                                    text = comment.text,
                                                    color = Color.White,
                                                    fontSize = 12.sp,
                                                    lineHeight = 17.sp
                                                )
                                            }
                                        }
                                        HorizontalDivider(color = Color(0xFF262626), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

        // Playlist Selection / Creation Dialog
        if (showPlaylistDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPlaylistDialog = false
                    isCreatingNewPlaylistInDialog = false
                },
                containerColor = YouTubeSurface,
                title = {
                    Text(
                        text = "إضافة «${song.arabicTitle}» إلى قائمة تشغيل",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        if (isCreatingNewPlaylistInDialog) {
                            OutlinedTextField(
                                value = newPlaylistName,
                                onValueChange = { newPlaylistName = it },
                                label = { Text("اسم القائمة الجديدة", color = YouTubeTextSecondary) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = YouTubeRed,
                                    unfocusedBorderColor = Color(0xFF444444),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    if (newPlaylistName.isNotBlank()) {
                                        onCreatePlaylist(newPlaylistName.trim())
                                        newPlaylistName = ""
                                        isCreatingNewPlaylistInDialog = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("إنشاء القائمة", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { isCreatingNewPlaylistInDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = YouTubeGold)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ إنشاء قائمة جديدة", color = YouTubeGold, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (playlists.isEmpty()) {
                                Text(
                                    text = "لا توجد قوائم تشغيل مخصصة حالياً. أنشئ قائمتك الأولى لتنظيم أغانيك المفضلة.",
                                    color = YouTubeTextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                LazyColumn(modifier = Modifier.height(180.dp)) {
                                    items(playlists) { pl ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    onAddToPlaylist(pl.id, song.id)
                                                    showPlaylistDialog = false
                                                }
                                                .padding(vertical = 8.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(YouTubeGold)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = pl.name,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            Text(
                                                text = "+ إضافة",
                                                color = YouTubeRed,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showPlaylistDialog = false
                        isCreatingNewPlaylistInDialog = false
                    }) {
                        Text("إغلاق", color = Color.White)
                    }
                }
            )
        }

        // Quality Selector Dialog available in both Standard & Fullscreen modes
        if (showQualityDialog) {
            AlertDialog(
                onDismissRequest = { showQualityDialog = false },
                containerColor = YouTubeSurface,
                title = {
                    Text(
                        text = "اختر جودة التشغيل",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PreferredQuality.entries.forEach { q ->
                            val isCurrent = activeQuality == q
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isCurrent) YouTubeRed.copy(alpha = 0.2f) else YouTubeSurfaceVariant)
                                    .border(
                                        width = if (isCurrent) 1.dp else 0.dp,
                                        color = if (isCurrent) YouTubeRed else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        onChangeQuality(q)
                                        showQualityDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = q.titleArabic,
                                    color = if (isCurrent) Color.White else YouTubeTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                                )
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = YouTubeRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showQualityDialog = false }) {
                        Text("إغلاق", color = YouTubeGold)
                    }
                }
            )
        }
    }
}
