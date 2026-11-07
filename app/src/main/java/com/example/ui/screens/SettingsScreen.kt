package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleanHands
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.LocalFileEntity
import com.example.model.AppAccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.PreferredQuality
import com.example.model.Song
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import java.util.UUID

@Composable
fun SettingsScreen(
    settings: AppSettings,
    localFiles: List<LocalFileEntity>,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onAccentColorChange: (AppAccentColor) -> Unit,
    onToggleShowLocalFiles: (Boolean) -> Unit,
    onImportLocalFiles: (List<LocalFileEntity>) -> Unit,
    onDeleteLocalFile: (String) -> Unit,
    onClearAllLocalFiles: () -> Unit,
    onPlayLocalFile: (LocalFileEntity) -> Unit,
    onAutoPlayNextChange: (Boolean) -> Unit,
    onRememberPositionChange: (Boolean) -> Unit,
    onQualityChange: (PreferredQuality) -> Unit,
    onAutoRepeatChange: (Boolean) -> Unit,
    onLyricsSyncChange: (Boolean) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onNotificationsChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onSleepTimerChange: (Int) -> Unit,
    onClearCache: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val customTheme = LocalCustomTheme.current
    val scrollState = rememberScrollState()

    // SAF Document Picker for local audio/video files
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val parsedFiles = uris.mapNotNull { uri ->
                queryLocalFileInfo(context, uri)
            }
            if (parsedFiles.isNotEmpty()) {
                onImportLocalFiles(parsedFiles)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(customTheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("settings_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("settings_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "رجوع",
                    tint = customTheme.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "الإعدادات والمظهر",
                color = customTheme.textPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 1. Appearance & Theme Section
        SettingsSectionHeader(
            icon = Icons.Default.Brightness4,
            title = "المظهر ونمط الألوان (Theme)",
            accentColor = Color(settings.accentColor.hex)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "اختر مظهر التطبيق المفضل لديك:",
                    color = customTheme.textSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                AppThemeMode.entries.forEach { mode ->
                    val isSelected = settings.themeMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(settings.accentColor.hex).copy(alpha = 0.15f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) Color(settings.accentColor.hex) else customTheme.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onThemeModeChange(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.titleArabic,
                                color = if (isSelected) Color(settings.accentColor.hex) else customTheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = mode.subtitleArabic,
                                color = customTheme.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(settings.accentColor.hex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(14.dp))

                // Accent Color Selector
                Text(
                    text = "لون التمييز والأزرار (Accent Color):",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppAccentColor.entries.forEach { accent ->
                        val isSelected = settings.accentColor == accent
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { onAccentColorChange(accent) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(accent.hex))
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (accent == AppAccentColor.GOLD) Color.Black else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = accent.titleArabic,
                                color = if (isSelected) Color(accent.hex) else customTheme.textSecondary,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Local Device Files Section (Requested specifically)
        SettingsSectionHeader(
            icon = Icons.Default.FolderOpen,
            title = "ملفات الهاتف المحلية (Device Files)",
            accentColor = Color(settings.accentColor.hex)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "إظهار الملفات المحلية للجهاز",
                            color = customTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "إتاحة تصفح وتشغيل مقاطع الصوت المخزنة في ذاكرة الهاتف",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.showLocalDeviceFiles,
                        onCheckedChange = onToggleShowLocalFiles,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(settings.accentColor.hex)
                        ),
                        modifier = Modifier.testTag("toggle_show_local_files_switch")
                    )
                }

                if (settings.showLocalDeviceFiles) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Import Button
                    Button(
                        onClick = {
                            filePickerLauncher.launch(arrayOf("audio/*", "video/*"))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(settings.accentColor.hex),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_device_audio_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "استيراد ملفات صوتية من الهاتف",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الملفات المستوردة (${localFiles.size})",
                            color = customTheme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (localFiles.isNotEmpty()) {
                            Text(
                                text = "مسح الكل",
                                color = YouTubeRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onClearAllLocalFiles() }
                                    .padding(4.dp)
                            )
                        }
                    }

                    if (localFiles.isEmpty()) {
                        Text(
                            text = "لم تقم باستيراد أي ملفات بعد. اضغط على الزر أعلاه لاختيار أغاني من هاتفك.",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            localFiles.take(5).forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(customTheme.surfaceVariant)
                                        .clickable { onPlayLocalFile(file) }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color(settings.accentColor.hex),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = file.displayName,
                                            color = customTheme.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = file.sizeFormatted,
                                            color = customTheme.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    IconButton(
                                        onClick = { onPlayLocalFile(file) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = "تشغيل",
                                            tint = Color(0xFF4CAF50),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteLocalFile(file.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "حذف",
                                            tint = customTheme.textSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            if (localFiles.size > 5) {
                                Text(
                                    text = "+ ${localFiles.size - 5} ملفات أخرى متاحة في شاشة التنزيلات",
                                    color = customTheme.textSecondary,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Playback Preferences Section
        SettingsSectionHeader(
            icon = Icons.Default.Headphones,
            title = "إعدادات المشغل والصوت",
            accentColor = Color(settings.accentColor.hex)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingsToggleRow(
                    title = "التشغيل التلقائي للأغنية التالية",
                    subtitle = "الانتقال للأغنية اللاحقة تلقائياً عند انتهاء الأغنية الحالية",
                    checked = settings.autoPlayNext,
                    onCheckedChange = onAutoPlayNextChange,
                    accentColor = Color(settings.accentColor.hex),
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    title = "تكرار التشغيل المستمر (Loop)",
                    subtitle = "إعادة تشغيل الأغنية الحالية بشكل لا نهائي",
                    checked = settings.autoRepeat,
                    onCheckedChange = onAutoRepeatChange,
                    accentColor = Color(settings.accentColor.hex),
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    title = "المزامنة الحية لكلمات الأغاني (Lyrics)",
                    subtitle = "إظهار الكلمات المتزامنة مع المقاطع الطربية تلقائياً",
                    checked = settings.lyricsSyncEnabled,
                    onCheckedChange = onLyricsSyncChange,
                    accentColor = Color(settings.accentColor.hex),
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Quality Selector
                Text(
                    text = "جودة البث والصوت المفضلة:",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                PreferredQuality.entries.forEach { quality ->
                    val isSelected = settings.preferredQuality == quality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color(settings.accentColor.hex).copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { onQualityChange(quality) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .border(
                                    width = 1.5.dp,
                                    color = if (isSelected) Color(settings.accentColor.hex) else customTheme.textSecondary,
                                    shape = CircleShape
                                )
                                .background(if (isSelected) Color(settings.accentColor.hex) else Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = quality.titleArabic,
                            color = if (isSelected) Color(settings.accentColor.hex) else customTheme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Sleep Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "مؤقت النوم (Sleep Timer)",
                            color = customTheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (settings.sleepTimerMinutes == 0) "معطّل" else "إيقاف التشغيل بعد ${settings.sleepTimerMinutes} دقيقة",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Row {
                        listOf(0, 15, 30, 60).forEach { mins ->
                            val isSelected = settings.sleepTimerMinutes == mins
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) Color(settings.accentColor.hex) else customTheme.surfaceVariant)
                                    .clickable { onSleepTimerChange(mins) }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (mins == 0) "إيقاف" else "${mins}د",
                                    color = if (isSelected) Color.White else customTheme.textSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Downloads & Storage Section
        SettingsSectionHeader(
            icon = Icons.Default.FileDownload,
            title = "التنزيلات والذاكرة (Storage)",
            accentColor = Color(settings.accentColor.hex)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingsToggleRow(
                    title = "التنزيل عبر شبكة Wi-Fi فقط",
                    subtitle = "تجنب استهلاك باقة بيانات الهاتف عند تحميل الأغاني",
                    checked = settings.downloadOverWifiOnly,
                    onCheckedChange = onWifiOnlyChange,
                    accentColor = Color(settings.accentColor.hex),
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تنظيف الذاكرة المؤقتة (Cache)",
                            color = customTheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "مسح الصور المصغرة والملفات المؤقتة لتوفير مساحة الهاتف",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    OutlinedButton(
                        onClick = onClearCache,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(settings.accentColor.hex)),
                        modifier = Modifier.testTag("clear_cache_button")
                    ) {
                        Text("مسح الآن", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Notifications Section
        SettingsSectionHeader(
            icon = Icons.Default.Notifications,
            title = "الإشعارات والتنبيهات",
            accentColor = Color(settings.accentColor.hex)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingsToggleRow(
                    title = "تنبيهات الأعمال والجديد",
                    subtitle = "إشعار فوري عند صدور كليب أو أغنية أو جلسة جديدة لرحمة رياض",
                    checked = settings.newReleasesNotification,
                    onCheckedChange = onNotificationsChange,
                    accentColor = Color(settings.accentColor.hex),
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                SettingsToggleRow(
                    title = "الاهتزاز مع التنبيه",
                    subtitle = "تفعيل الاهتزاز عند استلام إشعارات الأغاني",
                    checked = settings.vibrateOnNotification,
                    onCheckedChange = onVibrateChange,
                    accentColor = Color(settings.accentColor.hex),
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Developer Rights Box: ✪Mister Ali
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(customTheme.surface)
                .border(1.dp, YouTubeGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = YouTubeGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RahmaRiad v1.2 Pro • برمجة وتطوير ✪Mister Ali",
                    color = YouTubeGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(90.dp))
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    accentColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color,
    textColor: Color,
    subtextColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = textColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = subtextColor,
                fontSize = 11.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor
            )
        )
    }
}

private fun queryLocalFileInfo(context: Context, uri: Uri): LocalFileEntity? {
    return try {
        var displayName = "ملف صوتي محلي"
        var sizeBytes: Long = 0L
        val mimeType = context.contentResolver.getType(uri) ?: "audio/mpeg"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    displayName = cursor.getString(nameIndex) ?: displayName
                }
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    sizeBytes = cursor.getLong(sizeIndex)
                }
            }
        }

        val sizeFormatted = when {
            sizeBytes > 1024 * 1024 -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
            sizeBytes > 1024 -> String.format("%.1f KB", sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }

        LocalFileEntity(
            id = UUID.randomUUID().toString(),
            uriString = uri.toString(),
            displayName = displayName,
            sizeFormatted = sizeFormatted,
            mimeType = mimeType,
            durationFormatted = "ملف محلي",
            addedAt = System.currentTimeMillis()
        )
    } catch (e: Exception) {
        null
    }
}
