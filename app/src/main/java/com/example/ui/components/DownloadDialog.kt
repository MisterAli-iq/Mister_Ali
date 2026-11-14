package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.DownloadType
import com.example.model.Song
import com.example.ui.theme.YouTubeBorder
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeSurfaceVariant
import com.example.ui.theme.YouTubeTextPrimary
import com.example.ui.theme.YouTubeTextSecondary

@Composable
fun DownloadDialog(
    song: Song,
    settings: AppSettings = AppSettings(),
    onDismiss: () -> Unit,
    onConfirmDownload: (Song, DownloadType) -> Unit
) {
    val initialType = if (settings.defaultDownloadType == "AUDIO_ONLY") {
        DownloadType.AUDIO_ONLY
    } else {
        DownloadType.FULL_VIDEO
    }
    var selectedType by remember(song.id) { mutableStateOf(initialType) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YouTubeSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = null,
                    tint = YouTubeRed,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "تنزيل حقيقي بدون إنترنت",
                    color = YouTubeTextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "اختر صيغة التحميل المفضلة لـ «${song.arabicTitle}»:",
                    color = YouTubeTextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Option 1: Full Video
                val isVideo = selectedType == DownloadType.FULL_VIDEO
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isVideo) YouTubeSurfaceVariant else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isVideo) YouTubeRed else YouTubeBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedType = DownloadType.FULL_VIDEO }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isVideo,
                            onClick = { selectedType = DownloadType.FULL_VIDEO },
                            colors = RadioButtonDefaults.colors(selectedColor = YouTubeRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = if (isVideo) YouTubeRed else YouTubeTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "فيديو كامل مباشر (MP4)",
                                color = YouTubeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "جودة التنزيل: ${settings.downloadQuality.titleArabic}",
                                color = YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Option 2: Audio Only
                val isAudio = selectedType == DownloadType.AUDIO_ONLY
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isAudio) YouTubeSurfaceVariant else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isAudio) YouTubeRed else YouTubeBorder,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { selectedType = DownloadType.AUDIO_ONLY }
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isAudio,
                            onClick = { selectedType = DownloadType.AUDIO_ONLY },
                            colors = RadioButtonDefaults.colors(selectedColor = YouTubeRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = if (isAudio) YouTubeGold else YouTubeTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "مسار صوتي نقي (M4A / MP3)",
                                color = YouTubeTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "استخراج مباشر للمسار الصوتي فقط بحجم خفيف",
                                color = YouTubeTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF181818))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = YouTubeGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "مكان الحفظ: ${settings.downloadStorageLocation.titleArabic}",
                        color = YouTubeGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirmDownload(song, selectedType) },
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                modifier = Modifier.testTag("confirm_download_button")
            ) {
                Text("بدء التحميل الحقيقي", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = YouTubeTextSecondary)
            ) {
                Text("إلغاء")
            }
        }
    )
}
