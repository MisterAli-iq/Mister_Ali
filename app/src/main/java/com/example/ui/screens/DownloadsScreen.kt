package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.RahmaCatalog
import com.example.data.local.LocalFileEntity
import com.example.model.DownloadType
import com.example.model.DownloadedMedia
import com.example.model.Song
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary

@Composable
fun DownloadsScreen(
    downloads: List<DownloadedMedia>,
    localFiles: List<LocalFileEntity> = emptyList(),
    showLocalFiles: Boolean = true,
    onPlayOffline: (Song) -> Unit,
    onPlayLocalFile: (LocalFileEntity) -> Unit = {},
    onDeleteDownload: (DownloadedMedia) -> Unit,
    onDeleteLocalFile: (String) -> Unit = {},
    onImportLocalFiles: () -> Unit = {},
    onScanDeviceMedia: () -> Unit = {},
    onGoToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customTheme = LocalCustomTheme.current
    var selectedFilterIndex by remember { mutableIntStateOf(0) }

    val filterTabs = remember(showLocalFiles, localFiles.size) {
        if (showLocalFiles) {
            listOf("الكل", "فيديوهات (MP4)", "صوتيات (MP3)", "ملفات الهاتف (${localFiles.size})")
        } else {
            listOf("الكل", "فيديوهات (MP4)", "صوتيات (MP3)")
        }
    }

    val filteredList = remember(downloads, selectedFilterIndex) {
        when (selectedFilterIndex) {
            1 -> downloads.filter { it.downloadType == DownloadType.FULL_VIDEO }
            2 -> downloads.filter { it.downloadType == DownloadType.AUDIO_ONLY }
            else -> downloads
        }
    }

    val isLocalFilesTab = showLocalFiles && selectedFilterIndex == 3

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(customTheme.background)
            .padding(horizontal = 14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.OfflinePin,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "التنزيلات والمحتوى المحلي",
                    color = customTheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            val totalCount = downloads.size + (if (showLocalFiles) localFiles.size else 0)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1B5E20))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "$totalCount ملف أوفلاين",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Offline Banner Tip
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDownload,
                    contentDescription = null,
                    tint = YouTubeGold,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isLocalFilesTab)
                        "ملفاتك الصوتية المستوردة من ذاكرة الهاتف جاهزة للتشغيل المباشر داخل التطبيق."
                    else
                        "جميع المحتويات هنا مخزنة على جهازك وتعمل في وضع الطيران وبدون إنترنت.",
                    color = customTheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Tabs Row: All, Videos, Audios, Local Phone Files
        TabRow(
            selectedTabIndex = selectedFilterIndex.coerceAtMost(filterTabs.size - 1),
            containerColor = customTheme.background,
            contentColor = Color.White,
            indicator = { tabPositions ->
                val safeIndex = selectedFilterIndex.coerceAtMost(tabPositions.size - 1)
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[safeIndex]),
                    color = YouTubeRed
                )
            },
            divider = {}
        ) {
            filterTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedFilterIndex == index,
                    onClick = { selectedFilterIndex = index },
                    text = {
                        Text(
                            text = title,
                            color = if (selectedFilterIndex == index) customTheme.textPrimary else customTheme.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (selectedFilterIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Content based on selected tab
        if (isLocalFilesTab) {
            // Local Device Files Tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ملفات وسائط الهاتف (${localFiles.size})",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onScanDeviceMedia,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF242424))
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(15.dp), tint = YouTubeGold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("فحص تلقائي", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    Button(
                        onClick = onImportLocalFiles,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("استيراد", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (localFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = customTheme.textSecondary,
                            modifier = Modifier.size(68.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "لا توجد ملفات محلية مضافة بعد",
                            color = customTheme.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يمكنك استيراد وتشغيل أي مقاطع صوتية أو فيديو من ذاكرة هاتفك عبر زر الاستيراد.",
                            color = customTheme.textSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onImportLocalFiles,
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("استيراد من الهاتف الآن", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("local_files_list")
                ) {
                    items(
                        items = localFiles,
                        key = { it.id }
                    ) { file ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable { onPlayLocalFile(file) },
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
                                        .size(54.dp, 44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF2E2500)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = YouTubeGold,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = file.displayName,
                                        color = customTheme.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${file.mimeType.substringAfter('/')} • ${file.sizeFormatted}",
                                        color = YouTubeGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                IconButton(onClick = { onPlayLocalFile(file) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل محلي",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                IconButton(onClick = { onDeleteLocalFile(file.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف من القائمة",
                                        tint = customTheme.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        } else {
            // App Downloads Tabs (All, MP4, MP3)
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = customTheme.textSecondary,
                            modifier = Modifier.size(68.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "لا توجد ملفات منزلة حالياً",
                            color = customTheme.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يمكنك تنزيل فيديوهات أو مسارات MP3 لأي أغنية بالضغط على زر التنزيل في المشغل.",
                            color = customTheme.textSecondary,
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onGoToHome,
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text("تصفح وتحميل الأغاني", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("downloads_list")
                ) {
                    items(
                        items = filteredList,
                        key = { it.id }
                    ) { item ->
                        val song = RahmaCatalog.getSongById(item.songId)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp)
                                .clickable {
                                    if (song != null) onPlayOffline(song)
                                },
                            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Video or Audio Thumbnail with YouTube Thumbnail & Instagram Fallback
                                Box(
                                    modifier = Modifier
                                        .size(62.dp, 46.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF222222))
                                ) {
                                    if (song != null) {
                                        AsyncImage(
                                            model = "https://img.youtube.com/vi/${song.youtubeVideoId}/hqdefault.jpg",
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                            error = painterResource(id = R.drawable.img_rahma_instagram_concert)
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.img_rahma_instagram_concert),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Icon(
                                        imageVector = if (item.downloadType == DownloadType.FULL_VIDEO) Icons.Default.Videocam else Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = if (item.downloadType == DownloadType.FULL_VIDEO) YouTubeRed else YouTubeGold,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .align(Alignment.Center)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        color = customTheme.textPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.downloadType.titleArabic,
                                            color = customTheme.textSecondary,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = " • ${item.fileSizeFormatted}",
                                            color = YouTubeGold,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                // Play offline icon
                                IconButton(onClick = { if (song != null) onPlayOffline(song) }) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "تشغيل أوفلاين",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                // Delete button
                                IconButton(onClick = { onDeleteDownload(item) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "حذف الملف",
                                        tint = customTheme.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(90.dp))
                    }
                }
            }
        }
    }
}
