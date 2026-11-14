package com.example.ui.screens

import android.content.Intent
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import coil.compose.AsyncImage
import com.example.R
import com.example.data.RahmaCatalog
import com.example.data.remote.MediaStreamResolver
import com.example.model.PreferredQuality
import com.example.model.RahmaReel
import com.example.model.ReelComment
import com.example.model.ReelFilter
import com.example.model.VideoAspectMode
import com.example.ui.components.NativeMediaPlayerView
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@kotlin.OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShortsScreen(
    reels: List<RahmaReel>,
    preferredQuality: PreferredQuality = PreferredQuality.HIGH,
    videoAspectMode: VideoAspectMode = VideoAspectMode.CROP,
    likedReelIds: Set<String> = emptySet(),
    onToggleLike: (String) -> Unit = {},
    onOpenExternalUrl: (String) -> Unit = {},
    observeComments: (String) -> Flow<List<ReelComment>>,
    onAddComment: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedFilter by remember { mutableStateOf(ReelFilter.ALL) }
    val savedReels = remember { mutableStateMapOf<String, Boolean>() }

    val savedIdsSnapshot = savedReels.filterValues { it }.keys
    val filteredReels = remember(reels, selectedFilter, savedIdsSnapshot.toList()) {
        RahmaCatalog.filterReels(reels, selectedFilter, savedIdsSnapshot)
    }

    val reelExoPlayer = remember(context) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    var isResolvingReel by remember { mutableStateOf(false) }
    var isBufferingReel by remember { mutableStateOf(false) }
    var reelError by remember { mutableStateOf<String?>(null) }
    var reelFallbackEmbedUrl by remember { mutableStateOf<String?>(null) }
    var reelSnifferTargetUrl by remember { mutableStateOf<String?>(null) }
    var useReelWebFallback by remember { mutableStateOf(false) }
    var reelQuality by remember { mutableStateOf(preferredQuality) }
    var reelAttempt by remember { mutableIntStateOf(0) }
    var reelSniffAttemptedId by remember { mutableStateOf<String?>(null) }
    var retryCounter by remember { mutableIntStateOf(0) }

    LaunchedEffect(preferredQuality) {
        if (reelQuality != preferredQuality) {
            reelQuality = preferredQuality
            retryCounter++
        }
    }

    // 10-second buffering watchdog for Reels so ExoPlayer never spins forever
    LaunchedEffect(isBufferingReel, retryCounter) {
        if (isBufferingReel && !useReelWebFallback && reelError == null && reelSnifferTargetUrl == null) {
            delay(MediaStreamResolver.MAX_RESOLUTION_TIMEOUT_MS)
            if (isBufferingReel && reelExoPlayer.playbackState == Player.STATE_BUFFERING) {
                val stepped = MainViewModel.nextLowerQuality(reelQuality)
                if (stepped != reelQuality) {
                    reelQuality = stepped
                    retryCounter++
                } else {
                    reelExoPlayer.stop()
                    isBufferingReel = false
                    reelError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                }
            }
        }
    }

    DisposableEffect(reelExoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                isBufferingReel = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    reelError = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                isBufferingReel = false
                val stepped = MainViewModel.nextLowerQuality(reelQuality)
                if (stepped != reelQuality && reelAttempt < 2) {
                    reelAttempt++
                    reelQuality = stepped
                    retryCounter++
                } else if (!reelFallbackEmbedUrl.isNullOrBlank() && reelSnifferTargetUrl == null && reelSniffAttemptedId != "tried") {
                    reelSniffAttemptedId = "tried"
                    reelSnifferTargetUrl = reelFallbackEmbedUrl
                    isResolvingReel = true
                    reelError = null
                } else {
                    reelError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                }
            }
        }
        reelExoPlayer.addListener(listener)
        onDispose {
            reelExoPlayer.removeListener(listener)
            reelExoPlayer.release()
        }
    }

    LaunchedEffect(filteredReels.isEmpty()) {
        if (filteredReels.isEmpty()) {
            reelExoPlayer.stop()
            reelExoPlayer.clearMediaItems()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("shorts_screen")
    ) {
        if (filteredReels.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = YouTubeGold,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (selectedFilter == ReelFilter.ALL) {
                            "لا توجد مقاطع ريلز حالياً"
                        } else {
                            "لا توجد مقاطع في فلتر «${selectedFilter.displayName}»"
                        },
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (selectedFilter == ReelFilter.ALL) {
                            "سيتم عرض جميع مقاطع الريلز الحقيقية لرحمة رياض هنا فور توفرها أو إضافتها من قسم الإدارة في الإعدادات."
                        } else {
                            "يمكنك العودة لعرض جميع المقاطع القصيرة."
                        },
                        color = YouTubeTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )
                    if (selectedFilter != ReelFilter.ALL) {
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { selectedFilter = ReelFilter.ALL },
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.testTag("reset_reel_filter_button")
                        ) {
                            Text("عرض الكل", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            key(selectedFilter) {
                val pagerState = rememberPagerState(initialPage = 0, pageCount = { filteredReels.size })
                var activeCommentReel by remember { mutableStateOf<RahmaReel?>(null) }
                var newCommentText by remember { mutableStateOf("") }
                val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

                val settledReel = filteredReels.getOrNull(pagerState.settledPage)
                LaunchedEffect(settledReel?.id, retryCounter) {
                    if (settledReel == null) return@LaunchedEffect
                    reelExoPlayer.stop()
                    reelExoPlayer.clearMediaItems()
                    isResolvingReel = true
                    reelError = null
                    useReelWebFallback = false
                    reelSnifferTargetUrl = null
                    if (retryCounter == 0) {
                        reelAttempt = 0
                        reelSniffAttemptedId = null
                    }
                    reelFallbackEmbedUrl = if (settledReel.youtubeVideoId.isNotBlank()) {
                        MediaStreamResolver.buildSilentYouTubeEmbedUrl(settledReel.youtubeVideoId)
                    } else {
                        settledReel.sourceUrl.takeIf { it.startsWith("http", ignoreCase = true) }
                    }

                    // Ensure we NEVER accept audio-only URLs for Reels
                    val preVerified = settledReel.resolvedStreamUrl?.takeIf { candidate ->
                        val lower = candidate.lowercase()
                        val audioOnly = lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.contains("mime=audio")
                        MediaStreamResolver.isDirectMediaUri(candidate) && !audioOnly
                    }
                    val result = if (preVerified != null) {
                        com.example.data.remote.ResolvedStreamResult(
                            streamUrl = preVerified,
                            fallbackEmbedUrl = reelFallbackEmbedUrl,
                            isDirectVerified = true
                        )
                    } else {
                        kotlinx.coroutines.withTimeoutOrNull(MediaStreamResolver.MAX_RESOLUTION_TIMEOUT_MS) {
                            val primary = MediaStreamResolver.resolvePlayableStream(
                                sourceUrl = settledReel.sourceUrl,
                                videoIdHint = settledReel.youtubeVideoId,
                                preferredQuality = reelQuality,
                                requireVideoTrack = true
                            )
                            val stepped = MainViewModel.nextLowerQuality(reelQuality)
                            if (primary.streamUrl == null && stepped != reelQuality) {
                                reelQuality = stepped
                                MediaStreamResolver.resolvePlayableStream(
                                    sourceUrl = settledReel.sourceUrl,
                                    videoIdHint = settledReel.youtubeVideoId,
                                    preferredQuality = stepped,
                                    requireVideoTrack = true
                                )
                            } else {
                                primary
                            }
                        } ?: com.example.data.remote.ResolvedStreamResult(
                            streamUrl = null,
                            fallbackEmbedUrl = reelFallbackEmbedUrl
                        )
                    }

                    val fallback = result.fallbackEmbedUrl ?: reelFallbackEmbedUrl
                    reelFallbackEmbedUrl = fallback
                    val directReelUrl = result.streamUrl?.takeIf { candidate ->
                        val lower = candidate.lowercase()
                        val audioOnly = lower.endsWith(".m4a") || lower.endsWith(".mp3") || lower.contains("mime=audio")
                        MediaStreamResolver.isDirectMediaUri(candidate) && !audioOnly
                    }

                    if (!directReelUrl.isNullOrBlank()) {
                        isResolvingReel = false
                        val mediaItemBuilder = MediaItem.Builder().setUri(directReelUrl)
                        if (MediaStreamResolver.isHlsStreamUrl(directReelUrl)) {
                            mediaItemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                        }
                        val mediaItem = mediaItemBuilder.build()

                        if (directReelUrl.startsWith("http", ignoreCase = true) && result.requestHeaders.isNotEmpty()) {
                            val httpFactory = DefaultHttpDataSource.Factory()
                                .setAllowCrossProtocolRedirects(true)
                                .setConnectTimeoutMs(6_000)
                                .setReadTimeoutMs(8_000)
                                .setDefaultRequestProperties(result.requestHeaders)
                            result.requestHeaders["User-Agent"]?.let { ua ->
                                httpFactory.setUserAgent(ua)
                            }
                            val dataSourceFactory = DefaultDataSource.Factory(context, httpFactory)
                            val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
                                .createMediaSource(mediaItem)
                            reelExoPlayer.setMediaSource(mediaSource)
                        } else {
                            reelExoPlayer.setMediaItem(mediaItem)
                        }
                        reelExoPlayer.prepare()
                        reelExoPlayer.playWhenReady = true
                    } else if (!fallback.isNullOrBlank() && reelSniffAttemptedId != settledReel.id) {
                        reelSniffAttemptedId = settledReel.id
                        reelSnifferTargetUrl = fallback
                        isResolvingReel = true
                    } else {
                        isResolvingReel = false
                        reelError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                    }
                }

                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { pageIndex ->
                    val reel = filteredReels[pageIndex]
                    val isSettledPage = pagerState.settledPage == pageIndex && !pagerState.isScrollInProgress
                    val isLiked = likedReelIds.contains(reel.id)
                    val isSaved = savedReels[reel.id] ?: false
                    val currentLikesCount = reel.initialLikes + if (isLiked) 1 else 0
                    var showHeartPopup by remember { mutableStateOf(false) }

                    val infiniteTransition = rememberInfiniteTransition(label = "disc")
                    val discRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(8000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "disc_anim"
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (isSettledPage && !useReelWebFallback) {
                                    if (reelExoPlayer.isPlaying) reelExoPlayer.pause() else reelExoPlayer.play()
                                }
                            }
                    ) {
                        if (isSettledPage) {
                            NativeMediaPlayerView(
                                player = reelExoPlayer,
                                isResolvingStream = isResolvingReel,
                                isBuffering = isBufferingReel,
                                errorMessage = reelError,
                                fallbackEmbedUrl = reelFallbackEmbedUrl,
                                useWebFallback = useReelWebFallback,
                                snifferTargetUrl = reelSnifferTargetUrl,
                                thumbnailUrl = reel.thumbnailUrl,
                                contentScale = if (videoAspectMode == VideoAspectMode.FIT) ContentScale.Fit else ContentScale.Crop,
                                onRetry = {
                                    MediaStreamResolver.invalidateCachedStream(reel.sourceUrl, reel.youtubeVideoId)
                                    reelSniffAttemptedId = null
                                    retryCounter++
                                },
                                onLowerQuality = {
                                    reelQuality = MainViewModel.nextLowerQuality(reelQuality)
                                    retryCounter++
                                },
                                onStreamCaptured = { capturedUrl, headers ->
                                    reelSnifferTargetUrl = null
                                    isResolvingReel = false
                                    reelError = null
                                    if (MediaStreamResolver.isDirectMediaUri(capturedUrl)) {
                                        val itemBuilder = MediaItem.Builder().setUri(capturedUrl)
                                        if (MediaStreamResolver.isHlsStreamUrl(capturedUrl)) {
                                            itemBuilder.setMimeType(MimeTypes.APPLICATION_M3U8)
                                        }
                                        val httpFactory = DefaultHttpDataSource.Factory()
                                            .setAllowCrossProtocolRedirects(true)
                                            .setConnectTimeoutMs(6_000)
                                            .setReadTimeoutMs(8_000)
                                            .setDefaultRequestProperties(headers)
                                        val dsFactory = DefaultDataSource.Factory(context, httpFactory)
                                        reelExoPlayer.setMediaSource(DefaultMediaSourceFactory(dsFactory).createMediaSource(itemBuilder.build()))
                                        reelExoPlayer.prepare()
                                        reelExoPlayer.playWhenReady = true
                                    } else {
                                        reelError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                                    }
                                },
                                onSnifferFailed = {
                                    reelSnifferTargetUrl = null
                                    isResolvingReel = false
                                    isBufferingReel = false
                                    reelError = "تعذر تشغيل الفيديو حالياً، اضغط إعادة المحاولة"
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF101010)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!reel.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = reel.thumbnailUrl,
                                        contentDescription = reel.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        alpha = 0.55f
                                    )
                                } else {
                                    Image(
                                        painter = painterResource(id = R.drawable.rahma_hero_banner),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                        alpha = 0.5f
                                    )
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xAA000000),
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color(0xCC000000)
                                        )
                                    )
                                )
                        )

                        AnimatedVisibility(
                            visible = showHeartPopup,
                            enter = scaleIn(tween(250, easing = FastOutSlowInEasing)) + fadeIn(),
                            exit = scaleOut(tween(250, easing = FastOutSlowInEasing)) + fadeOut(),
                            modifier = Modifier.align(Alignment.Center)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = YouTubeRed,
                                modifier = Modifier.size(110.dp)
                            )
                        }

                        // Right Side Action Bar (Persistent Like, Comments, Share, Save, Disc)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 90.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val heartScale by animateFloatAsState(
                                    targetValue = if (isLiked) 1.25f else 1.0f,
                                    animationSpec = tween(150),
                                    label = "like_scale"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66000000))
                                        .clickable {
                                            val willLike = !isLiked
                                            onToggleLike(reel.id)
                                            if (willLike) {
                                                showHeartPopup = true
                                                coroutineScope.launch {
                                                    delay(700)
                                                    showHeartPopup = false
                                                }
                                            }
                                        }
                                        .scale(heartScale)
                                        .testTag("reel_like_button_${reel.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = "إعجاب",
                                        tint = if (isLiked) YouTubeRed else Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = currentLikesCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66000000))
                                        .clickable { activeCommentReel = reel }
                                        .testTag("reel_comments_button_${reel.id}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChatBubble,
                                        contentDescription = "التعليقات",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "تعليق",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x66000000))
                                        .clickable {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "شاهد مقطع '${reel.title}' في مشغل ✪Mister Ali:\n${reel.sourceUrl}"
                                                )
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "مشاركة المقطع"))
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "مشاركة",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "مشاركة",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x66000000))
                                    .clickable { savedReels[reel.id] = !isSaved },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                    contentDescription = "حفظ",
                                    tint = if (isSaved) YouTubeGold else Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222222))
                                    .border(2.dp, YouTubeGold, CircleShape)
                                    .rotate(discRotation),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.rahma_avatar),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                )
                            }
                        }

                        // Bottom Info with Publisher Photo + Name + Clickable Link Badge
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth(0.78f)
                                .padding(start = 14.dp, bottom = 90.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.rahma_avatar),
                                    contentDescription = "رحمة رياض",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, YouTubeRed, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "رحمة رياض • ${reel.platformLabel}",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = null,
                                            tint = Color(0xFF64B5F6),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    if (reel.dateText.isNotBlank()) {
                                        Text(
                                            text = reel.dateText,
                                            color = YouTubeGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = reel.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (reel.sourceUrl.startsWith("http", ignoreCase = true)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x88000000))
                                        .clickable { onOpenExternalUrl(reel.sourceUrl) }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = "فتح الرابط في Chrome",
                                        tint = YouTubeGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = reel.sourceUrl,
                                        color = Color(0xFF64B5F6),
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "Chrome",
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0x66000000))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "جودة: ${reelQuality.titleArabic}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }

                // Real Persisted Comments Bottom Sheet
                activeCommentReel?.let { commentReel ->
                    val reelComments by remember(commentReel.id) {
                        observeComments(commentReel.id)
                    }.collectAsState(initial = emptyList())

                    ModalBottomSheet(
                        onDismissRequest = { activeCommentReel = null },
                        sheetState = bottomSheetState,
                        containerColor = YouTubeSurface,
                        dragHandle = {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 8.dp)
                                    .size(36.dp, 4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.Gray)
                            )
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .navigationBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "التعليقات (${reelComments.size})",
                                    color = YouTubeTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { activeCommentReel = null }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "إغلاق", tint = YouTubeTextSecondary)
                                }
                            }

                            HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)

                            if (reelComments.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "لا توجد تعليقات بعد. كن أول من يعلّق!",
                                        color = YouTubeTextSecondary,
                                        fontSize = 13.sp
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp)
                                        .padding(vertical = 8.dp)
                                ) {
                                    items(reelComments, key = { it.id }) { comment ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF333333)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = comment.authorName.take(1),
                                                    color = YouTubeGold,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${comment.authorName} • ${comment.timeAgo}",
                                                    color = YouTubeTextSecondary,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = comment.text,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = newCommentText,
                                    onValueChange = { newCommentText = it },
                                    placeholder = { Text("أضف تعليقك...", color = YouTubeTextSecondary, fontSize = 12.sp) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(24.dp),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = YouTubeRed,
                                        unfocusedBorderColor = Color(0xFF444444),
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedContainerColor = Color(0xFF222222),
                                        unfocusedContainerColor = Color(0xFF1E1E1E)
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconButton(
                                    onClick = {
                                        if (newCommentText.isNotBlank()) {
                                            onAddComment(commentReel.id, newCommentText)
                                            newCommentText = ""
                                        }
                                    },
                                    enabled = newCommentText.isNotBlank()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "إرسال",
                                        tint = if (newCommentText.isNotBlank()) YouTubeRed else Color.Gray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Persistent Top Quality & Filter Bar (Admin link import moved strictly to Settings)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xBB000000), Color(0x66000000), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(top = 8.dp, bottom = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0x88000000))
                        .border(0.8.dp, YouTubeGold.copy(alpha = 0.65f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ريلز رحمة رياض • ${reelQuality.maxHeightPx}p",
                        color = YouTubeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x88000000))
                        .border(1.dp, YouTubeGold.copy(alpha = 0.7f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✪Mister Ali",
                        color = YouTubeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shorts_filter_bar")
            ) {
                items(ReelFilter.entries) { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) YouTubeRed else Color(0x88141414))
                            .border(
                                width = 0.8.dp,
                                color = if (isSelected) YouTubeGold.copy(alpha = 0.8f) else Color(0x55FFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedFilter = filter }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .testTag("reel_filter_${filter.name.lowercase()}")
                    ) {
                        Text(
                            text = filter.displayName,
                            color = if (isSelected) Color.White else Color(0xFFE0E0E0),
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
