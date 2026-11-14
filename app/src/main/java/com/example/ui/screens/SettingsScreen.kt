package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.LocalFileEntity
import com.example.data.local.LocalMediaScanner
import com.example.model.AppAccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.DefaultDownloadFormat
import com.example.model.DownloadStorageLocation
import com.example.model.LinkSourceConfig
import com.example.model.LinkSourceType
import com.example.model.LocalMediaType
import com.example.model.PreferredQuality
import com.example.model.SongCategory
import com.example.model.VideoAspectMode
import com.example.ui.components.LinkSourcesSettingsSection
import com.example.ui.theme.LocalCustomTheme
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.viewmodel.LinkPreviewState
import java.util.UUID

private enum class SettingsAccordionSection {
    DISPLAY_CATEGORIES,
    LINK_SOURCES,
    ADMIN_IMPORT_LOCAL,
    PLAYBACK_QUALITY,
    DOWNLOADS_CACHE,
    NOTIFICATIONS_ABOUT
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    localFiles: List<LocalFileEntity>,
    downloadsStorageSizeFormatted: String = "0 MB",
    linkPreviewState: LinkPreviewState = LinkPreviewState.Idle,
    isImportingLink: Boolean = false,
    onSaveAllSettings: (AppSettings) -> Unit = {},
    onResetAllSettings: () -> Unit = {},
    onPreviewImportLink: (String) -> Unit = {},
    onClearLinkPreview: () -> Unit = {},
    onImportAdminLink: (url: String, customTitle: String, targetCategory: SongCategory, mediaType: LocalMediaType) -> Unit = { _, _, _, _ -> },
    onAddCustomLinkDomain: (String) -> Unit = {},
    onRemoveCustomLinkDomain: (String) -> Unit = {},
    onImportLocalFiles: (List<LocalFileEntity>) -> Unit = {},
    onDeleteLocalFile: (String) -> Unit = {},
    onClearAllLocalFiles: () -> Unit = {},
    onPlayLocalFile: (LocalFileEntity) -> Unit = {},
    onOpenLocalMediaHub: () -> Unit = {},
    onClearAllDownloads: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val customTheme = LocalCustomTheme.current
    val scrollState = rememberScrollState()

    var draftSettings by remember { mutableStateOf(settings) }
    var lastCommittedSettings by remember { mutableStateOf(settings) }
    LaunchedEffect(settings) {
        if (draftSettings == lastCommittedSettings) {
            draftSettings = settings
        }
        lastCommittedSettings = settings
    }

    val hasUnsavedChanges = draftSettings != settings
    var showUnsavedExitDialog by remember { mutableStateOf(false) }

    val handleAttemptExit: () -> Unit = {
        if (hasUnsavedChanges) {
            showUnsavedExitDialog = true
        } else {
            onBack()
        }
    }

    BackHandler(enabled = true) {
        handleAttemptExit()
    }

    var expandedSection by remember { mutableStateOf<SettingsAccordionSection?>(SettingsAccordionSection.DISPLAY_CATEGORIES) }
    val accentHex = Color(draftSettings.accentColor.hex)

    // Admin Link Import State
    var adminUrlInput by remember { mutableStateOf("") }
    var adminTitleInput by remember { mutableStateOf("") }
    var adminSelectedCategory by remember { mutableStateOf(SongCategory.IMPORTED) }
    var adminSelectedMediaType by remember { mutableStateOf(LocalMediaType.VIDEO) }

    LaunchedEffect(linkPreviewState) {
        if (linkPreviewState is LinkPreviewState.Ready) {
            if (adminTitleInput.isBlank()) {
                adminTitleInput = linkPreviewState.title
            }
            adminSelectedCategory = linkPreviewState.suggestedCategory
            adminSelectedMediaType = linkPreviewState.suggestedMediaType
        }
    }

    // Local Media Filter in Settings
    var localMediaFilter by remember { mutableStateOf(LocalMediaType.ALL) }
    val filteredLocalFiles = remember(localFiles, localMediaFilter) {
        if (localMediaFilter == LocalMediaType.ALL) {
            localFiles
        } else {
            localFiles.filter { LocalMediaScanner.resolveEntityMediaType(it) == localMediaFilter.name }
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val parsedFiles = uris.mapNotNull { uri -> queryLocalFileInfo(context, uri) }
            if (parsedFiles.isNotEmpty()) {
                onImportLocalFiles(parsedFiles)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(customTheme.background)
            .testTag("settings_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = handleAttemptExit,
                    modifier = Modifier.testTag("settings_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "رجوع",
                        tint = customTheme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "الإعدادات الشاملة",
                        color = customTheme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasUnsavedChanges) "يوجد تعديلات غير محفوظة" else "جميع الإعدادات محفوظة ومفعّلة",
                        color = if (hasUnsavedChanges) YouTubeGold else customTheme.textSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Scrollable Accordion Cards
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Display & Categories Accordion
            AccordionSettingsCard(
                title = "العرض والمظهر والفئات",
                subtitle = "${draftSettings.themeMode.titleArabic} • ${if (draftSettings.isGridView) "عرض شبكي" else "عرض قائمة"}",
                icon = Icons.Default.Brightness4,
                accentHex = accentHex,
                customTheme = customTheme,
                isExpanded = expandedSection == SettingsAccordionSection.DISPLAY_CATEGORIES,
                onToggle = {
                    expandedSection = if (expandedSection == SettingsAccordionSection.DISPLAY_CATEGORIES) null else SettingsAccordionSection.DISPLAY_CATEGORIES
                },
                tag = "accordion_display_categories"
            ) {
                SettingsToggleRow(
                    title = "تفعيل العرض الشبكي في الرئيسية",
                    subtitle = "تبديل مباشر بين عرض القائمة وعرض الشبكة (2-3 أعمدة)",
                    checked = draftSettings.isGridView,
                    onCheckedChange = { draftSettings = draftSettings.copy(isGridView = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                SettingsToggleRow(
                    title = "إخفاء شريط الفئات بالكامل",
                    subtitle = "عرض قائمة الأغاني والفيديوهات فقط بواجهة نظيفة تماماً",
                    checked = draftSettings.hideCategoriesBar,
                    onCheckedChange = { draftSettings = draftSettings.copy(hideCategoriesBar = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary,
                    switchTag = "toggle_hide_categories_bar"
                )

                if (!draftSettings.hideCategoriesBar) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "إظهار وإخفاء كل فئة على حدة:",
                        color = customTheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    SongCategory.entries.filter { it != SongCategory.ALL }.forEach { cat ->
                        val isVisible = !draftSettings.hiddenCategories.contains(cat)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cat.displayName,
                                color = customTheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Switch(
                                checked = isVisible,
                                onCheckedChange = { showCat ->
                                    val nextHidden = draftSettings.hiddenCategories.toMutableSet().apply {
                                        if (showCat) remove(cat) else add(cat)
                                    }
                                    draftSettings = draftSettings.copy(hiddenCategories = nextHidden)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentHex
                                ),
                                modifier = Modifier.testTag("category_visibility_switch_${cat.name.lowercase()}")
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "نمط المظهر (Theme):",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                AppThemeMode.entries.forEach { mode ->
                    val isSelected = draftSettings.themeMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) accentHex.copy(alpha = 0.15f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.5.dp else 0.5.dp,
                                color = if (isSelected) accentHex else customTheme.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { draftSettings = draftSettings.copy(themeMode = mode) }
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.titleArabic,
                                color = if (isSelected) accentHex else customTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(
                                text = mode.subtitleArabic,
                                color = customTheme.textSecondary,
                                fontSize = 10.sp
                            )
                        }
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = accentHex)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "لون التمييز والأزرار:",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AppAccentColor.entries.forEach { accent ->
                        val isSelected = draftSettings.accentColor == accent
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { draftSettings = draftSettings.copy(accentColor = accent) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
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
                                        modifier = Modifier.size(18.dp)
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

            // 2. Link Sources (12 Sources + Custom Domains) Accordion
            val enabledSourcesCount = LinkSourceType.twelvePlatforms.count { draftSettings.configFor(it).enabled }
            AccordionSettingsCard(
                title = "مصدر الروابط والبحث (12 منصة)",
                subtitle = "$enabledSourcesCount من 12 منصة مفعّلة",
                icon = Icons.Default.Link,
                accentHex = accentHex,
                customTheme = customTheme,
                isExpanded = expandedSection == SettingsAccordionSection.LINK_SOURCES,
                onToggle = {
                    expandedSection = if (expandedSection == SettingsAccordionSection.LINK_SOURCES) null else SettingsAccordionSection.LINK_SOURCES
                },
                tag = "accordion_link_sources"
            ) {
                LinkSourcesSettingsSection(
                    settings = draftSettings,
                    accentHex = accentHex,
                    customTheme = customTheme,
                    onSetAllEnabled = { enabled ->
                        val updatedMap = LinkSourceType.entries.associateWith { src ->
                            draftSettings.configFor(src).copy(enabled = enabled)
                        }
                        draftSettings = draftSettings.copy(linkSources = updatedMap)
                    },
                    onSetSourceEnabled = { src, enabled ->
                        val currentCfg = draftSettings.configFor(src)
                        val updatedMap = draftSettings.linkSources.toMutableMap().apply {
                            put(src, currentCfg.copy(enabled = enabled))
                        }
                        draftSettings = draftSettings.copy(linkSources = updatedMap)
                    },
                    onUpdateSourceConfig = { src, cfg ->
                        val updatedMap = draftSettings.linkSources.toMutableMap().apply {
                            put(src, cfg)
                        }
                        draftSettings = draftSettings.copy(linkSources = updatedMap)
                    },
                    onUpdateCustomSourceConfig = { cfg ->
                        val updatedMap = draftSettings.linkSources.toMutableMap().apply {
                            put(LinkSourceType.OTHER, cfg)
                        }
                        draftSettings = draftSettings.copy(linkSources = updatedMap)
                    },
                    onAddCustomDomain = { dom ->
                        val clean = dom.trim().lowercase()
                        if (clean.contains(".")) {
                            val nextDomains = (draftSettings.customLinkDomains + clean).distinct()
                            draftSettings = draftSettings.copy(customLinkDomains = nextDomains)
                        }
                        onAddCustomLinkDomain(dom)
                    },
                    onRemoveCustomDomain = { dom ->
                        draftSettings = draftSettings.copy(
                            customLinkDomains = draftSettings.customLinkDomains.filterNot { it == dom }
                        )
                        onRemoveCustomLinkDomain(dom)
                    }
                )
            }

            // 3. Admin Link Import + Live Preview + Phone Files Accordion
            AccordionSettingsCard(
                title = "استيراد الروابط (للإدارة) وملفات الهاتف",
                subtitle = "إضافة ومعاينة روابط جديدة + الوسائط المحلية (${localFiles.size})",
                icon = Icons.Default.AddLink,
                accentHex = accentHex,
                customTheme = customTheme,
                isExpanded = expandedSection == SettingsAccordionSection.ADMIN_IMPORT_LOCAL,
                onToggle = {
                    expandedSection = if (expandedSection == SettingsAccordionSection.ADMIN_IMPORT_LOCAL) null else SettingsAccordionSection.ADMIN_IMPORT_LOCAL
                },
                tag = "accordion_admin_import"
            ) {
                Text(
                    text = "إضافة رابط جديد (خاص بالإدارة):",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "ألصق الرابط واضغط «فحص ومعاينة» لمشاهدة النتيجة وتحديد الفئة قبل الحفظ (لا يتكرر الرابط بعد إضافته).",
                    color = customTheme.textSecondary,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = adminUrlInput,
                    onValueChange = {
                        adminUrlInput = it
                        if (it.isBlank()) onClearLinkPreview()
                    },
                    placeholder = { Text("https://...", color = customTheme.textSecondary, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentHex,
                        unfocusedBorderColor = customTheme.border,
                        focusedTextColor = customTheme.textPrimary,
                        unfocusedTextColor = customTheme.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_link_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = { onPreviewImportLink(adminUrlInput) },
                    enabled = adminUrlInput.isNotBlank() && linkPreviewState !is LinkPreviewState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = customTheme.surfaceVariant),
                    border = BorderStroke(0.8.dp, YouTubeGold.copy(alpha = 0.65f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_preview_link_button")
                ) {
                    if (linkPreviewState is LinkPreviewState.Loading) {
                        CircularProgressIndicator(color = YouTubeGold, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("جارٍ الفحص وجلب المعاينة...", color = YouTubeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.Preview, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فحص ومعاينة الرابط", color = YouTubeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Live Preview Result Card
                when (val preview = linkPreviewState) {
                    is LinkPreviewState.Error -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2D1215))
                                .border(0.7.dp, YouTubeRed, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(text = preview.message, color = Color.White, fontSize = 12.sp)
                        }
                    }

                    is LinkPreviewState.Ready -> {
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = customTheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, if (preview.isDuplicate) YouTubeRed else YouTubeGold.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!preview.thumbnailUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = preview.thumbnailUrl,
                                            contentDescription = preview.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(72.dp, 50.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preview.title,
                                            color = customTheme.textPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${preview.platformLabel} • ${preview.channelName}",
                                            color = YouTubeGold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                if (preview.isDuplicate) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "تنبيه: هذا الرابط مضاف مسبقاً ولن يتم تكراره.",
                                        color = YouTubeRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = adminTitleInput,
                            onValueChange = { adminTitleInput = it },
                            label = { Text("عنوان العمل", fontSize = 11.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentHex,
                                unfocusedBorderColor = customTheme.border,
                                focusedTextColor = customTheme.textPrimary,
                                unfocusedTextColor = customTheme.textPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "اختر الفئة المستهدفة:",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SongCategory.entries.filter { it != SongCategory.ALL }.forEach { cat ->
                                val selected = adminSelectedCategory == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) accentHex else customTheme.background)
                                        .clickable { adminSelectedCategory = cat }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        color = if (selected) Color.White else customTheme.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "نوع المحتوى:",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            LocalMediaType.entries.forEach { mt ->
                                val selected = adminSelectedMediaType == mt
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) accentHex else customTheme.background)
                                        .clickable { adminSelectedMediaType = mt }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = mt.titleArabic,
                                        color = if (selected) Color.White else customTheme.textSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                onImportAdminLink(
                                    preview.correctedUrl,
                                    adminTitleInput,
                                    adminSelectedCategory,
                                    adminSelectedMediaType
                                )
                                adminUrlInput = ""
                                adminTitleInput = ""
                            },
                            enabled = !isImportingLink && !preview.isDuplicate,
                            colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_save_link_button")
                        ) {
                            Text("حفظ ونشر الرابط في التطبيق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    else -> Unit
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Phone Local Files Hub & Type Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ملفات الهاتف المحلية (${localFiles.size})",
                            color = customTheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Switch(
                        checked = draftSettings.showLocalDeviceFiles,
                        onCheckedChange = { draftSettings = draftSettings.copy(showLocalDeviceFiles = it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentHex),
                        modifier = Modifier.testTag("toggle_show_local_files_switch")
                    )
                }

                if (draftSettings.showLocalDeviceFiles) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { filePickerLauncher.launch(arrayOf("audio/*", "video/*", "image/*")) },
                            colors = ButtonDefaults.buttonColors(containerColor = accentHex, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_device_audio_button")
                        ) {
                            Icon(imageVector = Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استيراد من الهاتف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenLocalMediaHub,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(0.8.dp, YouTubeGold),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_local_media_hub_button")
                        ) {
                            Icon(imageVector = Icons.Default.PermMedia, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("عرض الكل", color = YouTubeGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // MediaType Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        LocalMediaType.entries.forEach { mt ->
                            val selected = localMediaFilter == mt
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) accentHex else customTheme.surfaceVariant)
                                    .clickable { localMediaFilter = mt }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = mt.titleArabic,
                                    color = if (selected) Color.White else customTheme.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    if (filteredLocalFiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        filteredLocalFiles.take(4).forEach { file ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(customTheme.surfaceVariant)
                                    .clickable { onPlayLocalFile(file) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(imageVector = Icons.Default.MusicNote, contentDescription = null, tint = accentHex, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = file.displayName, color = customTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(text = file.sizeFormatted, color = customTheme.textSecondary, fontSize = 10.sp)
                                }
                                IconButton(onClick = { onPlayLocalFile(file) }, modifier = Modifier.size(30.dp)) {
                                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "تشغيل", tint = Color(0xFF4CAF50))
                                }
                                IconButton(onClick = { onDeleteLocalFile(file.id) }, modifier = Modifier.size(30.dp)) {
                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "حذف", tint = customTheme.textSecondary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (localFiles.isNotEmpty()) {
                            Text(
                                text = "مسح جميع الملفات المحلية",
                                color = YouTubeRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onClearAllLocalFiles() }
                                    .padding(top = 6.dp)
                            )
                        }
                    }
                }
            }

            // 4. Playback & 6 Quality Tiers Accordion
            AccordionSettingsCard(
                title = "التشغيل وجودة الريلز والفيديو",
                subtitle = draftSettings.preferredQuality.titleArabic,
                icon = Icons.Default.Videocam,
                accentHex = accentHex,
                customTheme = customTheme,
                isExpanded = expandedSection == SettingsAccordionSection.PLAYBACK_QUALITY,
                onToggle = {
                    expandedSection = if (expandedSection == SettingsAccordionSection.PLAYBACK_QUALITY) null else SettingsAccordionSection.PLAYBACK_QUALITY
                },
                tag = "accordion_playback_quality"
            ) {
                // Default Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "وضع التشغيل الافتراضي", color = customTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (draftSettings.defaultVideoMode) "فيديو وصوت" else "صوت فقط",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(true to "فيديو", false to "صوت").forEach { (isVid, label) ->
                            val selected = draftSettings.defaultVideoMode == isVid
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) accentHex else customTheme.surfaceVariant)
                                    .clickable { draftSettings = draftSettings.copy(defaultVideoMode = isVid) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = label, color = if (selected) Color.White else customTheme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "مقاس عرض الفيديو:",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VideoAspectMode.entries.forEach { aspect ->
                        val isSelected = draftSettings.videoAspectMode == aspect
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) accentHex.copy(alpha = 0.16f) else customTheme.surfaceVariant)
                                .border(if (isSelected) 1.2.dp else 0.5.dp, if (isSelected) accentHex else customTheme.border, RoundedCornerShape(8.dp))
                                .clickable { draftSettings = draftSettings.copy(videoAspectMode = aspect) }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = aspect.titleArabic, color = if (isSelected) accentHex else customTheme.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "الجودة الافتراضية لجميع الفيديوهات والريلز (6 خيارات):",
                    color = customTheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                PreferredQuality.entries.forEach { quality ->
                    SettingsRadioOptionRow(
                        title = quality.titleArabic,
                        isSelected = draftSettings.preferredQuality == quality,
                        accentColor = accentHex,
                        textColor = customTheme.textPrimary,
                        onClick = { draftSettings = draftSettings.copy(preferredQuality = quality) },
                        testTag = "quality_option_${quality.name.lowercase()}"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Speed
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "سرعة التشغيل (${draftSettings.defaultPlaybackSpeed}x)", color = customTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f).forEach { spd ->
                            val isSelected = draftSettings.defaultPlaybackSpeed == spd
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) accentHex else customTheme.surfaceVariant)
                                    .clickable { draftSettings = draftSettings.copy(defaultPlaybackSpeed = spd) }
                                    .padding(horizontal = 7.dp, vertical = 5.dp)
                            ) {
                                Text(text = "${spd}x", color = if (isSelected) Color.White else customTheme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                SettingsToggleRow(
                    title = "تذكّر موضع التوقف",
                    subtitle = "استكمال الفيديو من آخر ثانية تلقائياً",
                    checked = draftSettings.rememberPlaybackPosition,
                    onCheckedChange = { draftSettings = draftSettings.copy(rememberPlaybackPosition = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "التشغيل التلقائي للأغنية التالية",
                    subtitle = "الانتقال التلقائي عند انتهاء المقطع",
                    checked = draftSettings.autoPlayNext,
                    onCheckedChange = { draftSettings = draftSettings.copy(autoPlayNext = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "تكرار التشغيل (Loop)",
                    subtitle = "إعادة تشغيل المقطع الحالي",
                    checked = draftSettings.autoRepeat,
                    onCheckedChange = { draftSettings = draftSettings.copy(autoRepeat = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "البديل الصامت لاستخراج البث",
                    subtitle = "استخراج رابط الفيديو والفيديو مع الصوت تلقائياً عند تعذر المصدر المباشر",
                    checked = draftSettings.silentWebFallbackEnabled,
                    onCheckedChange = { draftSettings = draftSettings.copy(silentWebFallbackEnabled = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "مزامنة الكلمات (Lyrics)",
                    subtitle = "تمييز الكلمات المتزامنة تلقائياً",
                    checked = draftSettings.lyricsSyncEnabled,
                    onCheckedChange = { draftSettings = draftSettings.copy(lyricsSyncEnabled = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "مؤقت النوم", color = customTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(0, 15, 30, 60).forEach { mins ->
                            val isSelected = draftSettings.sleepTimerMinutes == mins
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) accentHex else customTheme.surfaceVariant)
                                    .clickable { draftSettings = draftSettings.copy(sleepTimerMinutes = mins) }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(text = if (mins == 0) "إيقاف" else "${mins}د", color = if (isSelected) Color.White else customTheme.textSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 5. Downloads & Storage Accordion
            AccordionSettingsCard(
                title = "التنزيلات والذاكرة",
                subtitle = "المساحة: $downloadsStorageSizeFormatted",
                icon = Icons.Default.FileDownload,
                accentHex = accentHex,
                customTheme = customTheme,
                isExpanded = expandedSection == SettingsAccordionSection.DOWNLOADS_CACHE,
                onToggle = {
                    expandedSection = if (expandedSection == SettingsAccordionSection.DOWNLOADS_CACHE) null else SettingsAccordionSection.DOWNLOADS_CACHE
                },
                tag = "accordion_downloads_cache"
            ) {
                Text(text = "صيغة التنزيل الافتراضية:", color = customTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                DefaultDownloadFormat.entries.forEach { format ->
                    SettingsRadioOptionRow(
                        title = format.titleArabic,
                        isSelected = draftSettings.defaultDownloadType == format.key,
                        accentColor = accentHex,
                        textColor = customTheme.textPrimary,
                        onClick = { draftSettings = draftSettings.copy(defaultDownloadType = format.key) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "جودة التنزيل:", color = customTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                PreferredQuality.entries.forEach { dlQuality ->
                    SettingsRadioOptionRow(
                        title = dlQuality.titleArabic,
                        isSelected = draftSettings.downloadQuality == dlQuality,
                        accentColor = accentHex,
                        textColor = customTheme.textPrimary,
                        onClick = { draftSettings = draftSettings.copy(downloadQuality = dlQuality) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                Text(text = "مكان الحفظ:", color = customTheme.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                DownloadStorageLocation.entries.forEach { loc ->
                    val isSelected = draftSettings.downloadStorageLocation == loc
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) accentHex.copy(alpha = 0.12f) else Color.Transparent)
                            .clickable { draftSettings = draftSettings.copy(downloadStorageLocation = loc) }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(text = loc.titleArabic, color = if (isSelected) accentHex else customTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = loc.subtitleArabic, color = customTheme.textSecondary, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "التنزيل عبر Wi-Fi فقط",
                    subtitle = "حماية باقة بيانات الهاتف",
                    checked = draftSettings.downloadOverWifiOnly,
                    onCheckedChange = { draftSettings = draftSettings.copy(downloadOverWifiOnly = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "إعادة المحاولة التلقائية",
                    subtitle = "استكمال التحميل عند ضعف الإنترنت",
                    checked = draftSettings.autoRetryDownload,
                    onCheckedChange = { draftSettings = draftSettings.copy(autoRetryDownload = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onClearAllDownloads,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = YouTubeRed),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("حذف التنزيلات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onClearCache,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentHex),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("مسح الكاش", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 6. Notifications & About Accordion
            AccordionSettingsCard(
                title = "الإشعارات وحول التطبيق",
                subtitle = if (draftSettings.newReleasesNotification) "التنبيهات مفعّلة" else "التنبيهات متوقفة",
                icon = Icons.Default.Notifications,
                accentHex = accentHex,
                customTheme = customTheme,
                isExpanded = expandedSection == SettingsAccordionSection.NOTIFICATIONS_ABOUT,
                onToggle = {
                    expandedSection = if (expandedSection == SettingsAccordionSection.NOTIFICATIONS_ABOUT) null else SettingsAccordionSection.NOTIFICATIONS_ABOUT
                },
                tag = "accordion_notifications_about"
            ) {
                SettingsToggleRow(
                    title = "تنبيهات الإصدارات الجديدة",
                    subtitle = "إشعار فوري مع مهلة 6 ثوان قبل التشغيل أو الفتح",
                    checked = draftSettings.newReleasesNotification,
                    onCheckedChange = { draftSettings = draftSettings.copy(newReleasesNotification = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                SettingsToggleRow(
                    title = "الاهتزاز مع التنبيه",
                    subtitle = "تفعيل الاهتزاز عند استلام إشعارات رحمة رياض",
                    checked = draftSettings.vibrateOnNotification,
                    onCheckedChange = { draftSettings = draftSettings.copy(vibrateOnNotification = it) },
                    accentColor = accentHex,
                    textColor = customTheme.textPrimary,
                    subtextColor = customTheme.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenAbout,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(0.8.dp, YouTubeGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_about_from_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حول التطبيق والمطور ✪Mister Ali", color = YouTubeGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Developer Brand Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(customTheme.surface)
                    .border(1.dp, YouTubeGold.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RahmaRiad Pro • برمجة وتطوير ✪Mister Ali",
                        color = YouTubeGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Fixed Bottom Save & Reset Bar
        Surface(
            color = customTheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(0.6.dp, customTheme.border),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { onSaveAllSettings(draftSettings) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasUnsavedChanges) YouTubeRed else accentHex),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1.4f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("save_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasUnsavedChanges) "حفظ الإعدادات *" else "حفظ الإعدادات",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = {
                        onResetAllSettings()
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, customTheme.border),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("reset_settings_button")
                ) {
                    Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, tint = customTheme.textPrimary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إعادة تعيين", color = customTheme.textPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    // Unsaved Changes Exit Prompt
    if (showUnsavedExitDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedExitDialog = false },
            containerColor = customTheme.surface,
            title = {
                Text(
                    text = "حفظ التغييرات قبل الخروج؟",
                    color = customTheme.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "لديك تعديلات غير محفوظة في الإعدادات. هل ترغب في حفظها وتطبيقها على التطبيق قبل المغادرة؟",
                    color = customTheme.textSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAllSettings(draftSettings)
                        showUnsavedExitDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed)
                ) {
                    Text("حفظ والخروج", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = {
                        showUnsavedExitDialog = false
                        draftSettings = settings
                        onBack()
                    }) {
                        Text("خروج بدون حفظ", color = customTheme.textSecondary)
                    }
                    TextButton(onClick = { showUnsavedExitDialog = false }) {
                        Text("إلغاء", color = YouTubeGold)
                    }
                }
            }
        )
    }
}

@Composable
private fun AccordionSettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentHex: Color,
    customTheme: com.example.ui.theme.CustomThemePalette,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    tag: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = customTheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isExpanded) 1.dp else 0.5.dp,
            color = if (isExpanded) accentHex.copy(alpha = 0.6f) else customTheme.border
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentHex.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentHex,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = customTheme.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = customTheme.textSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "طي القسم" else "توسيع القسم",
                    tint = customTheme.textSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                ) {
                    HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun SettingsRadioOptionRow(
    title: String,
    isSelected: Boolean,
    accentColor: Color,
    textColor: Color,
    onClick: () -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .let { if (testTag != null) it.testTag(testTag) else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .border(
                    width = 1.5.dp,
                    color = if (isSelected) accentColor else Color.Gray,
                    shape = CircleShape
                )
                .background(if (isSelected) accentColor else Color.Transparent)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = if (isSelected) accentColor else textColor,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
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
    subtextColor: Color,
    switchTag: String? = null
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
            ),
            modifier = if (switchTag != null) Modifier.testTag(switchTag) else Modifier
        )
    }
}

private fun queryLocalFileInfo(context: Context, uri: Uri): LocalFileEntity? {
    return try {
        var displayName = "ملف محلي"
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
    } catch (_: Exception) {
        null
    }
}
