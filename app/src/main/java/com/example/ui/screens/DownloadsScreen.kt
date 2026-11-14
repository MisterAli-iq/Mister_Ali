package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.LocalFileEntity
import com.example.data.local.PlaylistEntity
import com.example.data.local.PlaylistItemEntity
import com.example.model.DownloadType
import com.example.model.DownloadedMedia
import com.example.model.Song
import com.example.model.SongCategory
import com.example.ui.theme.DarkChipBg
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary

import android.net.Uri
import com.example.ui.components.LocalMediaHub

@Composable
fun DownloadsScreen(
    downloads: List<DownloadedMedia>,
    allSongs: List<Song> = emptyList(),
    localFiles: List<LocalFileEntity> = emptyList(),
    grantedFolderUris: Set<String> = emptySet(),
    selectedTopTab: Int = 0,
    onSelectTopTab: (Int) -> Unit = {},
    playlists: List<PlaylistEntity> = emptyList(),
    playlistItems: List<PlaylistItemEntity> = emptyList(),
    recentPlayed: List<Song> = emptyList(),
    showLocalFiles: Boolean = true,
    downloadsStorageSizeFormatted: String = "0 MB",
    downloadStorageLocationTitle: String = "داخل التطبيق + مجلدات الهاتف",
    onOpenDownloadSettings: () -> Unit = {},
    onPlayOffline: (Song) -> Unit,
    onPlaySong: (Song) -> Unit = {},
    onPlayLocalFile: (LocalFileEntity) -> Unit = {},
    onDeleteDownload: (DownloadedMedia) -> Unit,
    onDeleteLocalFile: (String) -> Unit = {},
    onGrantFolderUri: (Uri) -> Unit = {},
    onRemoveFolderUri: (String) -> Unit = {},
    onImportPickedFiles: (List<LocalFileEntity>) -> Unit = {},
    onCreatePlaylist: (String, String) -> Unit = { _, _ -> },
    onDeletePlaylist: (String) -> Unit = {},
    onRemoveSongFromPlaylist: (String, String) -> Unit = { _, _ -> },
    onPlayPlaylist: (String) -> Unit = {},
    onClearRecentHistory: () -> Unit = {},
    onImportLocalFiles: () -> Unit = {},
    onScanDeviceMedia: (Boolean) -> Unit = {},
    onGoToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customTheme = LocalCustomTheme.current
    var downloadFilterIndex by remember { mutableIntStateOf(0) } // 0: All, 1: Videos, 2: Audios

    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var newPlaylistDesc by remember { mutableStateOf("") }
    var expandedPlaylistId by remember { mutableStateOf<String?>(null) }

    val topTabs = remember(downloads.size, playlists.size, recentPlayed.size, localFiles.size, showLocalFiles) {
        buildList {
            add("التنزيلات (${downloads.size})")
            add("قوائم التشغيل (${playlists.size})")
            add("سجل الاستماع (${recentPlayed.size})")
            if (showLocalFiles) {
                add("الوسائط المحلية (${localFiles.size})")
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(customTheme.background)
            .padding(horizontal = 14.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                    contentDescription = null,
                    tint = YouTubeGold,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "المكتبة والمحتوى الخاص",
                    color = customTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B5E20))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "✪Mister Ali",
                    color = YouTubeGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Top Navigation Tabs: Downloads | Playlists | Recent 20 | Local
        TabRow(
            selectedTabIndex = selectedTopTab.coerceAtMost(topTabs.size - 1),
            containerColor = customTheme.background,
            contentColor = Color.White,
            indicator = { tabPositions ->
                val safeIndex = selectedTopTab.coerceAtMost(tabPositions.size - 1)
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[safeIndex]),
                    color = YouTubeRed
                )
            },
            divider = {}
        ) {
            topTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTopTab == index,
                    onClick = { onSelectTopTab(index) },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedTopTab == index) customTheme.textPrimary else customTheme.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (selectedTopTab == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // TAB CONTENT
        when (selectedTopTab) {
            // TAB 0: DOWNLOADS
            0 -> {
                // Storage & Download Settings Summary Strip
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = customTheme.surface),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حجم التنزيلات: $downloadsStorageSizeFormatted",
                                color = YouTubeGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "الحفظ: $downloadStorageLocationTitle",
                                color = customTheme.textSecondary,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(YouTubeRed)
                                .clickable { onOpenDownloadSettings() }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                                .testTag("downloads_settings_shortcut")
                        ) {
                            Text(
                                text = "إعدادات التنزيل",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sub-filter chips: All, Videos, Audios
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val subFilters = listOf("الكل", "فيديوهات (MP4)", "صوتيات (MP3/M4A)")
                    subFilters.forEachIndexed { idx, filterName ->
                        val isSelected = downloadFilterIndex == idx
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) YouTubeRed else DarkChipBg)
                                .clickable { downloadFilterIndex = idx }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = filterName,
                                color = if (isSelected) Color.White else customTheme.textSecondary,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                val filteredDownloads = remember(downloads, downloadFilterIndex) {
                    when (downloadFilterIndex) {
                        1 -> downloads.filter { it.downloadType == DownloadType.FULL_VIDEO }
                        2 -> downloads.filter { it.downloadType == DownloadType.AUDIO_ONLY }
                        else -> downloads
                    }
                }

                if (filteredDownloads.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = customTheme.textSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد ملفات منزّلة حالياً",
                                color = customTheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يمكنك تنزيل الأغاني والفيديوهات للاستماع بدون إنترنت عبر زر التحميل في المشغل",
                                color = customTheme.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onGoToHome,
                                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("تصفح الأغاني للتنزيل", color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredDownloads, key = { it.id }) { item ->
                            val song = allSongs.find { it.id == item.songId } ?: Song(
                                id = item.songId,
                                title = item.title,
                                arabicTitle = item.title,
                                category = SongCategory.ALL,
                                durationFormatted = item.durationFormatted,
                                viewsCount = "ملف منزّل",
                                sourcePlatform = "أوفلاين",
                                externalUrl = "file://${item.localFilePath}",
                                resolvedStreamUrl = "file://${item.localFilePath}"
                            )
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlayOffline(song) },
                                colors = CardDefaults.cardColors(containerColor = customTheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(70.dp, 48.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.rahma_hero_banner),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color(0x33000000)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (item.downloadType == DownloadType.FULL_VIDEO) Icons.Default.Videocam else Icons.Default.MusicNote,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title,
                                            color = customTheme.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${item.downloadType.titleArabic} • ${item.fileSizeFormatted}",
                                            color = YouTubeGold,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(onClick = { if (song != null) onPlayOffline(song) }) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل أوفلاين",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    IconButton(onClick = { onDeleteDownload(item) }) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = customTheme.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            }

            // TAB 1: CUSTOM PLAYLISTS (قوائم التشغيل)
            1 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "قوائم التشغيل المخصصة (${playlists.size})",
                        color = customTheme.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { showCreatePlaylistDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("قائمة جديدة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (playlists.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = null,
                                tint = YouTubeGold,
                                modifier = Modifier.size(60.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "لا توجد قوائم تشغيل مخصصة",
                                color = customTheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "أنشئ قوائمك الخاصة لترتيب وتنظيم أغانيك وحفلاتك المفضلة لرحمة رياض لتشغيلها بكل سهولة!",
                                color = customTheme.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { showCreatePlaylistDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("+ إنشاء أول قائمة تشغيل", color = Color.White)
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(playlists, key = { it.id }) { playlist ->
                            val currentItems = playlistItems.filter { it.playlistId == playlist.id }
                            val isExpanded = expandedPlaylistId == playlist.id

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = customTheme.surface),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
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
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(playlist.coverColorHex)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = playlist.name,
                                                    color = customTheme.textPrimary,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (playlist.description.isNotBlank()) {
                                                    Text(
                                                        text = playlist.description,
                                                        color = customTheme.textSecondary,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                Text(
                                                    text = "${currentItems.size} أغنية",
                                                    color = YouTubeGold,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            // Play All button
                                            IconButton(
                                                onClick = { onPlayPlaylist(playlist.id) },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(YouTubeRed)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow,
                                                    contentDescription = "تشغيل الكل",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Spacer(modifier = Modifier.width(4.dp))

                                            // Toggle Expand button
                                            IconButton(
                                                onClick = {
                                                    expandedPlaylistId = if (isExpanded) null else playlist.id
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = "تفاصيل القائمة",
                                                    tint = customTheme.textSecondary
                                                )
                                            }

                                            // Delete Playlist button
                                            IconButton(onClick = { onDeletePlaylist(playlist.id) }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف القائمة",
                                                    tint = customTheme.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Expanded items list inside playlist
                                    AnimatedVisibility(visible = isExpanded) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp)
                                        ) {
                                            HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(8.dp))

                                            if (currentItems.isEmpty()) {
                                                Text(
                                                    text = "هذه القائمة فارغة حالياً. اضغط على 'قائمة' أثناء تشغيل أي أغنية لإضافتها هنا.",
                                                    color = customTheme.textSecondary,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.padding(vertical = 8.dp)
                                                )
                                            } else {
                                                currentItems.forEach { item ->
                                                    val song = allSongs.find { it.id == item.songId }
                                                    if (song != null) {
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                modifier = Modifier
                                                                    .weight(1f)
                                                                    .clickable { onPlaySong(song) }
                                                            ) {
                                                                Text(
                                                                    text = "▶",
                                                                    color = YouTubeRed,
                                                                    fontSize = 10.sp
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = song.arabicTitle,
                                                                    color = Color.White,
                                                                    fontSize = 13.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Text(
                                                                    text = song.durationFormatted,
                                                                    color = customTheme.textSecondary,
                                                                    fontSize = 10.sp
                                                                )
                                                            }

                                                            IconButton(
                                                                onClick = { onRemoveSongFromPlaylist(playlist.id, song.id) },
                                                                modifier = Modifier.size(24.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Close,
                                                                    contentDescription = "إزالة من القائمة",
                                                                    tint = customTheme.textSecondary,
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            }

            // TAB 2: RECENT PLAYED (سجل الاستماع - آخر 20 أغنية)
            2 -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.History, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "آخر ${recentPlayed.size} أعمال تم تشغيلها",
                            color = customTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (recentPlayed.isNotEmpty()) {
                        IconButton(onClick = onClearRecentHistory) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "مسح السجل",
                                tint = customTheme.textSecondary
                            )
                        }
                    }
                }

                if (recentPlayed.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = customTheme.textSecondary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "سجل الاستماع فارغ",
                                color = customTheme.textPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "سيتم حفظ وعرض آخر 20 أغنية أو فيديو تستمع إليها أو تشاهدها هنا تلقائياً لتصل إليها بنقرة واحدة!",
                                color = customTheme.textSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(recentPlayed, key = { index, song -> "${song.id}_$index" }) { index, song ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPlaySong(song) },
                                colors = CardDefaults.cardColors(containerColor = customTheme.surface),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Rank index badge
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (index < 3) YouTubeRed else DarkChipBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${index + 1}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    // Thumbnail
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp, 44.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color.Black)
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.rahma_hero_banner),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.arabicTitle,
                                            color = customTheme.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${song.category.displayName} • ${song.durationFormatted} • ${song.viewsCount}",
                                            color = customTheme.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }

                                    IconButton(
                                        onClick = { onPlaySong(song) },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل",
                                            tint = YouTubeRed,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                        item { Spacer(modifier = Modifier.height(90.dp)) }
                    }
                }
            }

            // TAB 3: LOCAL DEVICE MEDIA ("الوسائط المحلية")
            3 -> {
                LocalMediaHub(
                    localFiles = localFiles,
                    grantedFolderUris = grantedFolderUris,
                    customTheme = customTheme,
                    onGrantFolderUri = onGrantFolderUri,
                    onRemoveFolderUri = onRemoveFolderUri,
                    onImportPickedFiles = onImportPickedFiles,
                    onPlayMediaFile = onPlayLocalFile,
                    onDeleteLocalFile = onDeleteLocalFile,
                    onRefreshScan = onScanDeviceMedia
                )
            }
        }

        // Create Playlist Dialog
        if (showCreatePlaylistDialog) {
            AlertDialog(
                onDismissRequest = {
                    showCreatePlaylistDialog = false
                    newPlaylistName = ""
                    newPlaylistDesc = ""
                },
                containerColor = YouTubeSurface,
                title = {
                    Text("إنشاء قائمة تشغيل جديدة", color = Color.White, fontWeight = FontWeight.Bold)
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("اسم القائمة (مثال: أغاني السفر)", color = YouTubeTextSecondary) },
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
                        OutlinedTextField(
                            value = newPlaylistDesc,
                            onValueChange = { newPlaylistDesc = it },
                            label = { Text("وصف القائمة (اختياري)", color = YouTubeTextSecondary) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = YouTubeRed,
                                unfocusedBorderColor = Color(0xFF444444),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreatePlaylist(newPlaylistName.trim(), newPlaylistDesc.trim())
                                showCreatePlaylistDialog = false
                                newPlaylistName = ""
                                newPlaylistDesc = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                    ) {
                        Text("إنشاء", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showCreatePlaylistDialog = false
                        newPlaylistName = ""
                        newPlaylistDesc = ""
                    }) {
                        Text("إلغاء", color = customTheme.textSecondary)
                    }
                }
            )
        }
    }
}
