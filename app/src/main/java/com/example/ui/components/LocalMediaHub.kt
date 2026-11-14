package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.example.data.local.LocalFileEntity
import com.example.data.local.LocalMediaScanner
import com.example.model.LocalMediaSortOption
import com.example.model.LocalMediaType
import com.example.ui.theme.CustomThemePalette
import com.example.ui.theme.DarkChipBg
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalMediaHub(
    localFiles: List<LocalFileEntity>,
    grantedFolderUris: Set<String>,
    customTheme: CustomThemePalette,
    onGrantFolderUri: (Uri) -> Unit,
    onRemoveFolderUri: (String) -> Unit,
    onImportPickedFiles: (List<LocalFileEntity>) -> Unit,
    onPlayMediaFile: (LocalFileEntity) -> Unit,
    onDeleteLocalFile: (String) -> Unit,
    onRefreshScan: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(LocalMediaType.ALL) }
    var selectedSort by remember { mutableStateOf(LocalMediaSortOption.DATE_DESC) }
    var previewImageItem by remember { mutableStateOf<LocalFileEntity?>(null) }

    // Auto-refresh whenever user returns to app after adding/editing files on device
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        onRefreshScan(true)
    }

    // SAF Persistent Folder Grant (Play Policy Compliant)
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            onGrantFolderUri(uri)
        }
    }

    // Android Photo & Video Picker (Zero Permissions)
    val visualPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val entities = uris.mapNotNull { queryPickedLocalEntity(context, it, persistPermission = false) }
            if (entities.isNotEmpty()) onImportPickedFiles(entities)
        }
    }

    // SAF Audio & Documents Picker
    val audioDocsPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val entities = uris.mapNotNull { queryPickedLocalEntity(context, it, persistPermission = true) }
            if (entities.isNotEmpty()) onImportPickedFiles(entities)
        }
    }

    val filteredSortedFiles = remember(localFiles, searchQuery, selectedType, selectedSort) {
        LocalMediaScanner.filterAndSortLocalMedia(
            items = localFiles,
            query = searchQuery,
            typeFilter = selectedType,
            sortOption = selectedSort
        )
    }

    val counts = remember(localFiles) {
        val v = localFiles.count { LocalMediaScanner.resolveEntityMediaType(it) == "VIDEO" }
        val a = localFiles.count { LocalMediaScanner.resolveEntityMediaType(it) == "AUDIO" }
        val i = localFiles.count { LocalMediaScanner.resolveEntityMediaType(it) == "IMAGE" }
        Triple(v, a, i)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("local_media_hub")
    ) {
        // First-time onboarding / SAF folder permission card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Default.PermMedia,
                            contentDescription = null,
                            tint = YouTubeGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "الوسائط المحلية (بدون إنترنت)",
                                color = customTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "فيديو (${counts.first}) • صوت (${counts.second}) • صور (${counts.third})",
                                color = customTheme.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { onRefreshScan(false) },
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("refresh_local_media_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "تحديث الوسائط المحلية",
                            tint = YouTubeGold
                        )
                    }
                }

                if (grantedFolderUris.isEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "لجلب جميع فيديوهاتك وصوتياتك وصورك تلقائياً وتحديثها عند إضافة أي ملف جديد، امنح صلاحية الوصول لمجلد الوسائط أو اختر من مختار النظام الآمن:",
                        color = customTheme.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("grant_local_folder_button")
                    ) {
                        Icon(Icons.Default.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (grantedFolderUris.isEmpty()) "منح صلاحية مجلد" else "+ مجلد إضافي",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            visualPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pick_visual_media_button")
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صور وفيديو", color = customTheme.textPrimary, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            audioDocsPickerLauncher.launch(arrayOf("audio/*", "video/*", "image/*"))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("pick_audio_docs_button")
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("صوتيات / ملفات", color = customTheme.textPrimary, fontSize = 11.sp)
                    }
                }

                if (grantedFolderUris.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        grantedFolderUris.forEach { uriStr ->
                            val folderLabel = runCatching {
                                val docId = DocumentsContract.getTreeDocumentId(Uri.parse(uriStr))
                                docId.substringAfterLast(':').substringAfterLast('/').ifBlank { "مجلد وسائط" }
                            }.getOrDefault("مجلد وسائط")

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(customTheme.surfaceVariant)
                                    .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(folderLabel, color = customTheme.textPrimary, fontSize = 10.sp)
                                IconButton(
                                    onClick = { onRemoveFolderUri(uriStr) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "إزالة المجلد", tint = customTheme.textSecondary, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Local Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text("ابحث بالاسم أو المجلد أو الصيغة...", color = customTheme.textSecondary, fontSize = 12.sp)
            },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = customTheme.textSecondary)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "مسح البحث", tint = customTheme.textSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = YouTubeRed,
                unfocusedBorderColor = customTheme.border,
                focusedTextColor = customTheme.textPrimary,
                unfocusedTextColor = customTheme.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("local_media_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter by Type (All, Video, Audio, Image)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LocalMediaType.entries.forEach { typeOpt ->
                val isSelected = selectedType == typeOpt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) YouTubeRed else customTheme.surface)
                        .clickable { selectedType = typeOpt }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("local_type_filter_${typeOpt.name.lowercase()}")
                ) {
                    Text(
                        text = typeOpt.titleArabic,
                        color = if (isSelected) Color.White else customTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sort Row (Date, Folder, Type)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Sort, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(16.dp))
            Text("ترتيب:", color = customTheme.textSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            LocalMediaSortOption.entries.forEach { sortOpt ->
                val isSelected = selectedSort == sortOpt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) YouTubeGold.copy(alpha = 0.2f) else Color.Transparent)
                        .border(
                            0.7.dp,
                            if (isSelected) YouTubeGold else customTheme.border,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { selectedSort = sortOpt }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .testTag("local_sort_${sortOpt.name.lowercase()}")
                ) {
                    Text(
                        text = sortOpt.titleArabic,
                        color = if (isSelected) YouTubeGold else customTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredSortedFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.PermMedia,
                        contentDescription = null,
                        tint = customTheme.textSecondary,
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (localFiles.isEmpty()) "لا توجد وسائط محلية مضافة بعد" else "لا توجد نتائج مطابقة للفلتر أو البحث",
                        color = customTheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "اضغط على «منح صلاحية مجلد» أو اختر صوراً وفيديوهات وصوتيات لتصفحها وتشغيلها بدون إنترنت.",
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
                var lastFolderHeader: String? = null
                items(filteredSortedFiles, key = { it.id }) { file ->
                    val mediaType = LocalMediaScanner.resolveEntityMediaType(file)

                    if (selectedSort == LocalMediaSortOption.FOLDER && file.folderName != lastFolderHeader) {
                        lastFolderHeader = file.folderName
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, tint = YouTubeGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.folderName,
                                color = YouTubeGold,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (mediaType == "IMAGE") previewImageItem = file else onPlayMediaFile(file)
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
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkChipBg),
                                contentAlignment = Alignment.Center
                            ) {
                                if (mediaType == "IMAGE" || mediaType == "VIDEO") {
                                    AsyncImage(
                                        model = file.uriString,
                                        contentDescription = file.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                if (mediaType != "IMAGE") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(if (mediaType == "VIDEO") Color(0x55000000) else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (mediaType == "VIDEO") Icons.Default.Videocam else Icons.Default.MusicNote,
                                            contentDescription = null,
                                            tint = if (mediaType == "VIDEO") Color.White else YouTubeGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.displayName,
                                    color = customTheme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                val dateFormatted = remember(file.dateModified, file.addedAt) {
                                    val ts = maxOf(file.dateModified, file.addedAt)
                                    SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(ts))
                                }
                                val typeLabel = when (mediaType) {
                                    "VIDEO" -> "فيديو"
                                    "IMAGE" -> "صورة"
                                    else -> "صوت"
                                }
                                Text(
                                    text = "$typeLabel • ${file.folderName} • ${file.sizeFormatted} • $dateFormatted",
                                    color = customTheme.textSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    if (mediaType == "IMAGE") previewImageItem = file else onPlayMediaFile(file)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = if (mediaType == "IMAGE") Icons.Default.Image else Icons.Default.PlayArrow,
                                    contentDescription = if (mediaType == "IMAGE") "عرض الصورة" else "تشغيل",
                                    tint = if (mediaType == "IMAGE") YouTubeGold else Color(0xFF4CAF50),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            IconButton(
                                onClick = { onDeleteLocalFile(file.id) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "حذف من القائمة",
                                    tint = customTheme.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(90.dp)) }
            }
        }
    }

    // In-App Offline Image Viewer Dialog
    previewImageItem?.let { img ->
        Dialog(
            onDismissRequest = { previewImageItem = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = img.uriString,
                        contentDescription = img.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 56.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(Color(0xAA000000))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = img.displayName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${img.folderName} • ${img.sizeFormatted}",
                                color = YouTubeGold,
                                fontSize = 11.sp
                            )
                        }
                        Row {
                            IconButton(onClick = {
                                runCatching {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = img.mimeType.ifBlank { "image/*" }
                                        putExtra(Intent.EXTRA_STREAM, Uri.parse(img.uriString))
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "مشاركة الصورة"))
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "مشاركة", tint = Color.White)
                            }
                            IconButton(onClick = { previewImageItem = null }) {
                                Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun queryPickedLocalEntity(
    context: Context,
    uri: Uri,
    persistPermission: Boolean
): LocalFileEntity? {
    if (persistPermission) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }
    return try {
        var displayName = "ملف محلي"
        var sizeBytes = 0L
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) displayName = cursor.getString(nameIndex) ?: displayName
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) sizeBytes = cursor.getLong(sizeIndex)
            }
        }
        val mediaType = LocalMediaScanner.classifyMediaType(mimeType, displayName) ?: "AUDIO"
        LocalFileEntity(
            id = "picked_${UUID.randomUUID().toString().take(8)}",
            uriString = uri.toString(),
            displayName = displayName,
            sizeFormatted = LocalMediaScanner.formatFileSize(sizeBytes),
            mimeType = mimeType,
            durationFormatted = when (mediaType) {
                "IMAGE" -> "صورة"
                "VIDEO" -> "فيديو"
                else -> "صوت"
            },
            mediaType = mediaType,
            folderName = "ملفات مختارة",
            sizeBytes = sizeBytes,
            dateModified = System.currentTimeMillis(),
            addedAt = System.currentTimeMillis()
        )
    } catch (_: Exception) {
        null
    }
}
