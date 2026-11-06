package com.example.ui.components

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.TextDecrease
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
import com.example.R
import com.example.data.RahmaCatalog
import com.example.model.Song
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
    isPlaying: Boolean,
    isVideoMode: Boolean,
    currentPositionSeconds: Int,
    durationSeconds: Int,
    playbackSpeed: Float,
    isLooping: Boolean,
    isLyricsExpanded: Boolean,
    isFavorite: Boolean,
    onClose: () -> Unit,
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
    onDownloadClick: () -> Unit,
    onSelectUpNextSong: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isSubscribed by remember { mutableStateOf(true) }
    var selectedPlayerTab by remember { mutableIntStateOf(0) } // 0: Up Next, 1: Lyrics, 2: Info
    var lyricsFontSizeSp by remember { mutableStateOf(16) }
    var lyricsCopied by remember { mutableStateOf(false) }

    // System Back Handler collapses the player back to mini-player
    BackHandler {
        onClose()
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
            .statusBarsPadding(),
        color = YouTubeBlack
    ) {
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
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Videocam,
                                contentDescription = null,
                                tint = if (isVideoMode) Color.White else YouTubeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "فيديو",
                                color = if (isVideoMode) Color.White else YouTubeTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (!isVideoMode) YouTubeRed else Color.Transparent)
                            .clickable { if (isVideoMode) onToggleVideoAudio() }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = if (!isVideoMode) Color.White else YouTubeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "صوت",
                                color = if (!isVideoMode) Color.White else YouTubeTextSecondary,
                                fontSize = 12.sp,
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

            // Video Screen View (16:9 Aspect Ratio)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black)
            ) {
                if (isVideoMode) {
                    // Video Mode Canvas: Cinematic background with concert stage & pulsing animation
                    Image(
                        painter = painterResource(id = R.drawable.rahma_hero_banner),
                        contentDescription = "مشغل الفيديو",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        alpha = if (isPlaying) 0.85f else 0.45f
                    )

                    // Stage lighting gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0x33000000),
                                        Color.Transparent,
                                        Color(0xAA0F0F0F)
                                    )
                                )
                            )
                    )

                    // Real-time Lyrics Subtitle at bottom of video
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x99000000))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        val currentLine = song.lyrics.lines().getOrNull(
                            ((currentPositionSeconds / 8) % song.lyrics.lines().size.coerceAtLeast(1))
                        ) ?: song.arabicTitle
                        Text(
                            text = currentLine,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
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

                // Center playback control overlays
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onSkipBackward,
                        modifier = Modifier
                            .size(42.dp)
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
                            .size(42.dp)
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

                    Text(
                        text = currentFormatted,
                        color = YouTubeTextSecondary,
                        fontSize = 11.sp
                    )

                    Text(
                        text = song.durationFormatted,
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
                // Song Title & Views
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "رحمة رياض - ${song.arabicTitle} (${song.title})",
                        color = YouTubeTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${song.viewsCount} • ${song.releaseYear} • ألحان: ${song.composer} • كلمات: ${song.lyricist}",
                        color = YouTubeTextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Action Buttons Row (Like, Download, Share, Speed, Loop)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Favorite / Like
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { onFavoriteToggle() }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) YouTubeRed else YouTubeTextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isFavorite) "أعجبني" else "إعجاب",
                                color = if (isFavorite) YouTubeRed else YouTubeTextSecondary,
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
                                    text = "معلومات العمل",
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

                    val upNextSongs = RahmaCatalog.songs.filter { it.id != song.id }
                    items(upNextSongs) { nextSong ->
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
                                Image(
                                    painter = painterResource(id = R.drawable.rahma_hero_banner),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = 0.6f
                                )
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
                                Text(
                                    text = "رحمة رياض • ${nextSong.viewsCount}",
                                    color = YouTubeTextSecondary,
                                    fontSize = 11.sp
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

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Rahma Riad | رحمة رياض",
                                        color = YouTubeTextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "قناة موثقة",
                                        tint = Color(0xFF64B5F6),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "3.25M مشترك • القناة الرسمية",
                                    color = YouTubeTextSecondary,
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

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
