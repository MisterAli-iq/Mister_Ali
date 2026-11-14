package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.RahmaCatalog
import com.example.data.remote.GroundedSearchUiState
import com.example.model.RahmaReel
import com.example.model.Song
import com.example.model.SongCategory
import com.example.ui.components.CategoryChips
import com.example.ui.components.GridVideoCard
import com.example.ui.components.GroundedSearchCard
import com.example.ui.components.VideoCard
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed

@Composable
fun HomeScreen(
    allSongs: List<Song>,
    featuredSongs: List<Song> = emptyList(),
    songs: List<Song>,
    reels: List<RahmaReel>,
    isLoadingFeed: Boolean,
    selectedCategory: SongCategory,
    favoriteIds: Set<String>,
    downloadedIds: Set<String>,
    activeDownloadSongId: String?,
    activeDownloadTitle: String? = null,
    activeDownloadProgress: Int,
    searchQuery: String = "",
    isSearchingOnline: Boolean = false,
    isLoadingMoreChannel: Boolean = false,
    hasMoreChannelMedia: Boolean = true,
    isGridView: Boolean = false,
    hideCategoriesBar: Boolean = false,
    hiddenCategories: Set<SongCategory> = emptySet(),
    groundedSearchState: GroundedSearchUiState = GroundedSearchUiState.Idle,
    onRunGroundedSearch: (String) -> Unit = {},
    onDismissGroundedSearch: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onTriggerSmartSearch: (String) -> Unit = {},
    onCategorySelected: (SongCategory) -> Unit,
    onSongClick: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onToggleFeatured: (Song) -> Unit = {},
    onDownloadClick: (Song) -> Unit,
    onPlayExternalUrl: (String) -> Unit = {},
    onOpenExternalUrl: (String) -> Unit = {},
    onRefreshFeed: () -> Unit = {},
    onLoadMoreChannel: () -> Unit = {},
    onOpenShorts: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val customTheme = LocalCustomTheme.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(customTheme.background)
            .testTag("home_screen")
    ) {
        // Button ABOVE Search Bar: "بحث أكثر بالذكاء الاصطناعي"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    val q = searchQuery.trim().ifBlank { "أحدث أغاني وحفلات وأخبار رحمة رياض @RahmaRiad" }
                    onRunGroundedSearch(q)
                },
                colors = ButtonDefaults.buttonColors(containerColor = customTheme.surfaceVariant),
                border = BorderStroke(0.9.dp, YouTubeGold.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(18.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .testTag("ai_search_more_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "بحث أكثر بالذكاء الاصطناعي",
                    tint = YouTubeGold,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "بحث أكثر بالذكاء الاصطناعي",
                    color = customTheme.textPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (searchQuery.isNotBlank()) {
                Text(
                    text = "${songs.size} نتيجة",
                    color = YouTubeGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Google-badged Manual Search Bar (Search runs ONLY on Search button tap or IME Search)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .testTag("home_search_input"),
                placeholder = {
                    Text(
                        text = "ابحث عن أغنية أو كليب لرحمة رياض...",
                        color = customTheme.textSecondary,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            color = Color(0xFF4285F4),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "مسح البحث",
                                tint = customTheme.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        focusManager.clearFocus()
                        onTriggerSmartSearch(searchQuery)
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = YouTubeRed,
                    unfocusedBorderColor = customTheme.border,
                    focusedContainerColor = customTheme.surface,
                    unfocusedContainerColor = customTheme.surfaceVariant,
                    cursorColor = YouTubeRed,
                    focusedTextColor = customTheme.textPrimary,
                    unfocusedTextColor = customTheme.textPrimary
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    onTriggerSmartSearch(searchQuery)
                },
                enabled = !isSearchingOnline,
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                shape = RoundedCornerShape(22.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                modifier = Modifier
                    .height(50.dp)
                    .defaultMinSize(minWidth = 64.dp, minHeight = 48.dp)
                    .testTag("manual_search_button")
            ) {
                if (isSearchingOnline) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "بحث",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Quick Search Suggestion Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("smart_search_suggestions")
        ) {
            val quickQueries = listOf(
                "وعد مني",
                "الكوكب",
                "اصعد للكمر",
                "ماكو مني",
                "@RahmaRiad"
            )
            items(quickQueries) { q ->
                val isCurrent = searchQuery.trim().equals(q, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isCurrent) YouTubeRed else customTheme.surfaceVariant)
                        .border(0.6.dp, if (isCurrent) YouTubeGold else customTheme.border, RoundedCornerShape(14.dp))
                        .clickable {
                            focusManager.clearFocus()
                            onTriggerSmartSearch(q)
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = q,
                        color = if (isCurrent) Color.White else customTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Clean Category Chips (controlled from Settings: Hide entire bar or individual categories)
        CategoryChips(
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected,
            hideBar = hideCategoriesBar,
            hiddenCategories = hiddenCategories
        )

        // Live Google / AI Grounded Search Card
        GroundedSearchCard(
            state = groundedSearchState,
            onRetry = onRunGroundedSearch,
            onDismiss = onDismissGroundedSearch,
            onPlayMediaSource = onPlayExternalUrl
        )

        // Active Download Banner
        AnimatedVisibility(visible = activeDownloadSongId != null) {
            val downloadingSong = allSongs.find { it.id == activeDownloadSongId }
            val displayTitle = activeDownloadTitle ?: downloadingSong?.arabicTitle ?: ""
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(containerColor = customTheme.surface),
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
                            text = "جارٍ التحميل «$displayTitle»...",
                            color = customTheme.textPrimary,
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

        if (songs.isEmpty() && isLoadingFeed) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = YouTubeGold)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "جارٍ جلب أحدث أعمال رحمة رياض...",
                        color = customTheme.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (songs.isEmpty()) {
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
                        tint = customTheme.textSecondary,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isNotBlank()) "لا توجد نتائج مطابقة ضمن المصادر المفعّلة" else "لا توجد مقاطع في هذا القسم بعد",
                        color = customTheme.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "يمكنك تحديث القائمة، تغيير المصادر المفعّلة في الإعدادات، أو استخدام زر البحث بالذكاء الاصطناعي.",
                        color = customTheme.textSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onRefreshFeed,
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                        ) {
                            Text("تحديث القائمة", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                        if (searchQuery.isNotBlank()) {
                            Button(
                                onClick = { onSearchQueryChange("") },
                                colors = ButtonDefaults.buttonColors(containerColor = customTheme.surfaceVariant)
                            ) {
                                Text("عرض الكل", color = YouTubeGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (isGridView) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val columnCount = if (maxWidth >= 600.dp) 3 else 2
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columnCount),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("home_video_grid")
                ) {
                    if (featuredSongs.isNotEmpty() && searchQuery.isBlank() && selectedCategory != SongCategory.FEATURED) {
                        item(span = { GridItemSpan(columnCount) }) {
                            FeaturedShelfSection(
                                featuredSongs = featuredSongs,
                                onSongClick = onSongClick,
                                onToggleFeatured = onToggleFeatured,
                                onShowAllFeatured = { onCategorySelected(SongCategory.FEATURED) }
                            )
                        }
                    }

                    if (reels.isNotEmpty() && searchQuery.isBlank()) {
                        item(span = { GridItemSpan(columnCount) }) {
                            ReelsShelfSection(
                                reels = reels,
                                onOpenShorts = onOpenShorts
                            )
                        }
                    }

                    items(
                        items = songs,
                        key = { it.id }
                    ) { song ->
                        GridVideoCard(
                            song = song,
                            isFavorite = favoriteIds.contains(song.id),
                            isDownloaded = downloadedIds.contains(song.id),
                            onSongClick = onSongClick,
                            onFavoriteToggle = onFavoriteToggle,
                            showLinkBadge = searchQuery.isNotBlank(),
                            onOpenExternalUrl = onOpenExternalUrl
                        )
                    }

                    if (hasMoreChannelMedia && searchQuery.isBlank() && selectedCategory == SongCategory.ALL) {
                        item(span = { GridItemSpan(columnCount) }) {
                            LoadMoreFooter(
                                isLoadingMoreChannel = isLoadingMoreChannel,
                                onLoadMoreChannel = onLoadMoreChannel
                            )
                        }
                    }

                    item(span = { GridItemSpan(columnCount) }) {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("home_video_feed")
            ) {
                if (featuredSongs.isNotEmpty() && searchQuery.isBlank() && selectedCategory != SongCategory.FEATURED) {
                    item {
                        FeaturedShelfSection(
                            featuredSongs = featuredSongs,
                            onSongClick = onSongClick,
                            onToggleFeatured = onToggleFeatured,
                            onShowAllFeatured = { onCategorySelected(SongCategory.FEATURED) }
                        )
                    }
                }

                if (reels.isNotEmpty() && searchQuery.isBlank()) {
                    item {
                        ReelsShelfSection(
                            reels = reels,
                            onOpenShorts = onOpenShorts
                        )
                    }
                }

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
                        onDownloadClick = onDownloadClick,
                        onToggleFeatured = onToggleFeatured,
                        showLinkBadge = searchQuery.isNotBlank(),
                        onOpenExternalUrl = onOpenExternalUrl
                    )
                }

                if (hasMoreChannelMedia && searchQuery.isBlank() && selectedCategory == SongCategory.ALL) {
                    item {
                        LoadMoreFooter(
                            isLoadingMoreChannel = isLoadingMoreChannel,
                            onLoadMoreChannel = onLoadMoreChannel
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
private fun FeaturedShelfSection(
    featuredSongs: List<Song>,
    onSongClick: (Song) -> Unit,
    onToggleFeatured: (Song) -> Unit,
    onShowAllFeatured: () -> Unit
) {
    val customTheme = LocalCustomTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
            .testTag("featured_songs_shelf")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E2000))
                        .border(0.8.dp, YouTubeGold, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = YouTubeGold,
                        modifier = Modifier.size(15.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "أغاني مميزة (${featuredSongs.size})",
                    color = customTheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "عرض الكل ←",
                color = YouTubeGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onShowAllFeatured() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .testTag("featured_show_all_button")
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(featuredSongs, key = { "feat_${it.id}" }) { featSong ->
                Card(
                    modifier = Modifier
                        .width(204.dp)
                        .height(142.dp)
                        .clickable { onSongClick(featSong) }
                        .testTag("featured_card_${featSong.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1412)),
                    border = BorderStroke(1.dp, YouTubeGold.copy(alpha = 0.5f))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        val thumb = featSong.thumbnailUrl
                            ?: if (featSong.youtubeVideoId.isNotBlank()) "https://i.ytimg.com/vi/${featSong.youtubeVideoId}/hqdefault.jpg" else null
                        if (!thumb.isNullOrBlank()) {
                            AsyncImage(
                                model = thumb,
                                contentDescription = featSong.arabicTitle,
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

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x44000000),
                                            Color(0x55000000),
                                            Color(0xEE0B090A)
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xCC2E2000))
                                .clickable { onToggleFeatured(featSong) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "إزالة من المميزة",
                                    tint = YouTubeGold,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "مميزة",
                                    color = YouTubeGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(YouTubeRed.copy(alpha = 0.9f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "تشغيل",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = featSong.arabicTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = RahmaCatalog.displayChannelName(featSong),
                                color = YouTubeGold,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelsShelfSection(
    reels: List<RahmaReel>,
    onOpenShorts: () -> Unit
) {
    val customTheme = LocalCustomTheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                Text(
                    text = "مقاطع قصيرة • ريلز (${reels.size})",
                    color = customTheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "فتح المشغل ←",
                color = YouTubeGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenShorts() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reels, key = { it.id }) { reel ->
                Card(
                    modifier = Modifier
                        .width(135.dp)
                        .height(210.dp)
                        .clickable { onOpenShorts() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    border = BorderStroke(0.5.dp, Color(0xFF333333))
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        if (!reel.thumbnailUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = reel.thumbnailUrl,
                                contentDescription = reel.title,
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
                                alpha = 0.65f
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0x33000000),
                                            Color.Transparent,
                                            Color(0xEE0F0F0F)
                                        )
                                    )
                                )
                        )

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(YouTubeRed)
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = reel.platformLabel,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = reel.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadMoreFooter(
    isLoadingMoreChannel: Boolean,
    onLoadMoreChannel: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onLoadMoreChannel,
            enabled = !isLoadingMoreChannel,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
            border = BorderStroke(1.dp, YouTubeGold.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("load_more_channel_button")
        ) {
            if (isLoadingMoreChannel) {
                CircularProgressIndicator(
                    color = YouTubeGold,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("جارٍ جلب المزيد من @RahmaRiad...", color = YouTubeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("تحميل المزيد من @RahmaRiad", color = YouTubeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
