package com.example.ui.components

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.RahmaCatalog
import com.example.model.Song
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.YouTubeBlack
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurfaceVariant
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary

@Composable
fun VideoCard(
    song: Song,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onSongClick: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    onToggleFeatured: (Song) -> Unit = {},
    showLinkBadge: Boolean = false,
    onOpenExternalUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customTheme = LocalCustomTheme.current
    var showMenu by remember { mutableStateOf(false) }

    val resolvedThumb = song.thumbnailUrl
        ?: if (song.youtubeVideoId.isNotBlank()) "https://i.ytimg.com/vi/${song.youtubeVideoId}/hqdefault.jpg" else null
    val resolvedLink = song.externalUrl?.takeIf { it.isNotBlank() }
        ?: if (song.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${song.youtubeVideoId}" else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongClick(song) }
            .padding(bottom = 16.dp)
            .testTag("video_card_${song.id}")
    ) {
        // Thumbnail Box (16:9 ratio)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(Color(0xFF1E1E1E))
        ) {
            val startColor = Color(song.gradientStartHex)
            val endColor = Color(song.gradientEndHex)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(startColor, endColor, YouTubeBlack)
                        )
                    )
            ) {
                if (resolvedThumb != null) {
                    AsyncImage(
                        model = resolvedThumb,
                        contentDescription = song.arabicTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_rahma_instagram_concert)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_rahma_instagram_concert),
                        contentDescription = song.arabicTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC0F0F0F))
                            )
                        )
                )

                // Center play icon indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(30.dp)
                    )
                }

                // Badges: New release / Featured / Downloaded
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (song.isNewRelease) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(YouTubeRed)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "جديد",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (song.isFeatured) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xDD2E2000))
                                .border(0.8.dp, YouTubeGold, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "مميزة ⭐",
                                color = YouTubeGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (isDownloaded) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "محمّل ✓",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Duration Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xDD000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = song.durationFormatted,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Details Row (YouTube style: Publisher Photo + Title + Publisher Name + Clickable Link)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(id = R.drawable.rahma_avatar),
                contentDescription = RahmaCatalog.displayChannelName(song),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .border(1.dp, YouTubeRed.copy(alpha = 0.5f), CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.arabicTitle,
                    color = customTheme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                val isOfficial = RahmaCatalog.isOfficialRahmaChannel(song)
                val channelDisplay = RahmaCatalog.displayChannelName(song)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    if (isOfficial) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "القناة الرسمية",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = buildString {
                            append(channelDisplay)
                            if (song.channelHandle.isNotBlank() && !channelDisplay.contains(song.channelHandle, ignoreCase = true)) {
                                append(" (")
                                append(song.channelHandle)
                                append(")")
                            }
                            if (song.viewsCount.isNotBlank() && song.viewsCount != song.sourcePlatform && song.viewsCount != channelDisplay) {
                                append(" • ")
                                append(song.viewsCount)
                            }
                            if (song.publishDate.isNotBlank()) {
                                append(" • ")
                                append(song.publishDate)
                            }
                        },
                        color = if (isOfficial) YouTubeGold else customTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isOfficial) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }

                // Search Result / Direct Link Badge opening in Chrome / Default Browser
                if (showLinkBadge && !resolvedLink.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(customTheme.surfaceVariant)
                            .border(0.6.dp, YouTubeGold.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                            .clickable { onOpenExternalUrl(resolvedLink) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("video_card_link_badge_${song.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "فتح الرابط في المتصفح",
                            tint = YouTubeGold,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = resolvedLink,
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
                }
            }

            // More Options (3 Dots)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(48.dp).testTag("video_options_${song.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات إضافية",
                        tint = customTheme.textSecondary
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(YouTubeSurfaceVariant)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (song.isFeatured) "إزالة من الأغاني المميزة" else "تثبيت في الأغاني المميزة ⭐",
                                color = YouTubeTextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (song.isFeatured) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = YouTubeGold
                            )
                        },
                        onClick = {
                            showMenu = false
                            onToggleFeatured(song)
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Text(
                                if (isFavorite) "إزالة من المفضلة" else "إضافة للمفضلة",
                                color = YouTubeTextPrimary
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = null,
                                tint = if (isFavorite) YouTubeRed else YouTubeTextSecondary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onFavoriteToggle(song)
                        }
                    )

                    DropdownMenuItem(
                        text = { Text("تحميل بدون إنترنت", color = YouTubeTextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = null,
                                tint = YouTubeTextSecondary
                            )
                        },
                        onClick = {
                            showMenu = false
                            onDownloadClick(song)
                        }
                    )

                    if (!resolvedLink.isNullOrBlank()) {
                        DropdownMenuItem(
                            text = { Text("فتح الرابط في Chrome", color = Color(0xFF64B5F6)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                    contentDescription = null,
                                    tint = Color(0xFF64B5F6)
                                )
                            },
                            onClick = {
                                showMenu = false
                                onOpenExternalUrl(resolvedLink)
                            }
                        )
                    }

                    DropdownMenuItem(
                        text = { Text("مشاركة عبر وسائل التواصل", color = YouTubeTextPrimary) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = YouTubeTextSecondary
                            )
                        },
                        onClick = {
                            showMenu = false
                            ShareHelper.shareSong(context, song)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GridVideoCard(
    song: Song,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    onSongClick: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    showLinkBadge: Boolean = false,
    onOpenExternalUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val customTheme = LocalCustomTheme.current
    val resolvedThumb = song.thumbnailUrl
        ?: if (song.youtubeVideoId.isNotBlank()) "https://i.ytimg.com/vi/${song.youtubeVideoId}/hqdefault.jpg" else null
    val resolvedLink = song.externalUrl?.takeIf { it.isNotBlank() }
        ?: if (song.youtubeVideoId.isNotBlank()) "https://www.youtube.com/watch?v=${song.youtubeVideoId}" else null
    val channelDisplay = RahmaCatalog.displayChannelName(song)
    val isOfficial = RahmaCatalog.isOfficialRahmaChannel(song)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSongClick(song) }
            .testTag("grid_video_card_${song.id}"),
        colors = CardDefaults.cardColors(containerColor = customTheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(0.6.dp, customTheme.border)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color(0xFF1A1A1A))
            ) {
                if (resolvedThumb != null) {
                    AsyncImage(
                        model = resolvedThumb,
                        contentDescription = song.arabicTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = painterResource(id = R.drawable.img_rahma_instagram_concert)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.img_rahma_instagram_concert),
                        contentDescription = song.arabicTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0x88000000))
                            )
                        )
                )

                // Play indicator
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "تشغيل",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Duration badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xDD000000))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = song.durationFormatted,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isDownloaded) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF2E7D32))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "محمّل",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = song.arabicTitle,
                    color = customTheme.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.rahma_avatar),
                            contentDescription = channelDisplay,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        if (isOfficial) {
                            Icon(
                                imageVector = Icons.Default.Verified,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }
                        Text(
                            text = channelDisplay,
                            color = if (isOfficial) YouTubeGold else customTheme.textSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onFavoriteToggle(song) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "المفضلة",
                            tint = if (isFavorite) YouTubeRed else customTheme.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (showLinkBadge && !resolvedLink.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(customTheme.surfaceVariant)
                            .clickable { onOpenExternalUrl(resolvedLink) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "فتح الرابط",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(11.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = resolvedLink,
                            color = Color(0xFF64B5F6),
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
