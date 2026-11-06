package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RahmaCatalog
import com.example.model.Song
import com.example.model.SongCategory
import com.example.ui.components.CategoryChips
import com.example.ui.components.VideoCard
import com.example.ui.theme.YouTubeBlack
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary

@Composable
fun HomeScreen(
    songs: List<Song>,
    selectedCategory: SongCategory,
    favoriteIds: Set<String>,
    downloadedIds: Set<String>,
    activeDownloadSongId: String?,
    activeDownloadProgress: Int,
    onCategorySelected: (SongCategory) -> Unit,
    onSongClick: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onDownloadClick: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(YouTubeBlack)
    ) {
        // Horizontal Category Chips
        CategoryChips(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        // Active Download Banner (if downloading)
        AnimatedVisibility(visible = activeDownloadSongId != null) {
            val downloadingSong = RahmaCatalog.getSongById(activeDownloadSongId ?: "")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = YouTubeSurface),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = YouTubeRed,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "جارٍ تحميل «${downloadingSong?.arabicTitle ?: ""}»...",
                            color = YouTubeTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$activeDownloadProgress%",
                            color = YouTubeRed,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { activeDownloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = YouTubeRed,
                        trackColor = Color(0xFF333333)
                    )
                }
            }
        }

        // Empty Search Results State
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.SearchOff,
                        contentDescription = null,
                        tint = YouTubeTextSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "لا توجد نتائج مطابقة لبحثك",
                        color = YouTubeTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "جرب البحث بكلمات أخرى أو تصفح الأقسام الرئيسية",
                        color = YouTubeTextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Video Feed List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_video_feed")
            ) {
                items(
                    items = songs,
                    key = { it.id }
                ) { song ->
                    VideoCard(
                        song = song,
                        isFavorite = favoriteIds.contains(song.id),
                        isDownloaded = downloadedIds.contains(song.id),
                        onSongClick = onSongClick,
                        onFavoriteToggle = onFavoriteToggle,
                        onDownloadClick = onDownloadClick
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
