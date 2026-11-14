package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.GroundedSearchUiState
import com.example.data.remote.GroundedWebSource
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GroundedSearchCard(
    state: GroundedSearchUiState,
    onRetry: (String) -> Unit,
    onDismiss: () -> Unit,
    onPlayMediaSource: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

    AnimatedVisibility(
        visible = state !is GroundedSearchUiState.Idle,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = YouTubeSurface),
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(
                width = 0.8.dp,
                color = if (state is GroundedSearchUiState.Error) YouTubeRed.copy(alpha = 0.7f) else YouTubeGold.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("grounded_search_card")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = YouTubeGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "بحث Google المباشر • معلومات محدثة",
                            color = YouTubeGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .testTag("dismiss_grounded_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق ملخص بحث Google",
                            tint = YouTubeTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                when (state) {
                    is GroundedSearchUiState.Loading -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .testTag("grounded_search_loading")
                        ) {
                            CircularProgressIndicator(
                                color = YouTubeGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "جارٍ البحث في Google حول «${state.query}»...",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }

                    is GroundedSearchUiState.Streaming -> {
                        GroundedBodyContent(
                            text = state.text,
                            webQueries = state.webQueries,
                            sources = state.sources,
                            isStreaming = true,
                            onSourceClick = { source ->
                                if (isPlayableVideoHost(source.uri)) {
                                    onPlayMediaSource(source.uri)
                                } else {
                                    runCatching { uriHandler.openUri(source.uri) }
                                }
                            }
                        )
                    }

                    is GroundedSearchUiState.Success -> {
                        GroundedBodyContent(
                            text = state.text,
                            webQueries = state.webQueries,
                            sources = state.sources,
                            isStreaming = false,
                            onSourceClick = { source ->
                                if (isPlayableVideoHost(source.uri)) {
                                    onPlayMediaSource(source.uri)
                                } else {
                                    runCatching { uriHandler.openUri(source.uri) }
                                }
                            }
                        )
                    }

                    is GroundedSearchUiState.Error -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2D1215))
                                .border(0.7.dp, YouTubeRed, RoundedCornerShape(10.dp))
                                .padding(10.dp)
                                .testTag("grounded_search_error_box")
                        ) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = YouTubeRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = state.message,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onRetry(state.query) },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = YouTubeGold),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("retry_grounded_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    tint = YouTubeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إعادة المحاولة",
                                    color = YouTubeGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    GroundedSearchUiState.Idle -> Unit
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroundedBodyContent(
    text: String,
    webQueries: List<String>,
    sources: List<GroundedWebSource>,
    isStreaming: Boolean,
    onSourceClick: (GroundedWebSource) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (text.isNotBlank()) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 12.5.sp,
                lineHeight = 19.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grounded_search_answer_text")
            )
        }

        if (isStreaming) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = YouTubeGold,
                    strokeWidth = 1.5.dp,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "جارٍ استكمال التحديث المباشر...",
                    color = YouTubeTextSecondary,
                    fontSize = 10.sp
                )
            }
        }

        if (webQueries.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                webQueries.take(3).forEach { q ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E24))
                            .border(0.5.dp, Color(0xFF3A3A44), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = YouTubeTextSecondary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = q,
                            color = YouTubeTextSecondary,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        if (sources.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "المصادر الموثقة من بحث Google:",
                color = YouTubeGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                contentPadding = PaddingValues(end = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("grounded_sources_row")
            ) {
                items(sources, key = { it.uri }) { source ->
                    val isVideoSource = isPlayableVideoHost(source.uri)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A2228))
                            .border(0.7.dp, Color(0xFF3F5869), RoundedCornerShape(12.dp))
                            .clickable { onSourceClick(source) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("grounded_source_chip")
                    ) {
                        Icon(
                            imageVector = if (isVideoSource) Icons.Default.PlayCircleOutline else Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = if (isVideoSource) "تشغيل من المصدر" else "فتح المصدر",
                            tint = if (isVideoSource) YouTubeRed else YouTubeGold,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = source.title,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (source.domain.isNotBlank()) {
                                Text(
                                    text = source.domain,
                                    color = YouTubeTextSecondary,
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
    }
}

private fun isPlayableVideoHost(uri: String): Boolean {
    val lower = uri.lowercase()
    return lower.contains("youtube.com/watch") ||
        lower.contains("youtu.be/") ||
        lower.contains("instagram.com/reel") ||
        lower.contains("tiktok.com/") ||
        lower.contains("fb.watch") ||
        lower.endsWith(".mp4") ||
        lower.endsWith(".m3u8")
}
