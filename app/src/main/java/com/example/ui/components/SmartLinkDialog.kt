package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.LinkCheckStatus
import com.example.data.remote.LinkDiagnosticResult
import com.example.data.remote.LinkSourceDetection
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed
import com.example.ui.theme.YouTubeSurface
import com.example.ui.theme.YouTubeTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartLinkDialog(
    title: String = "تشغيل واستيراد رابط ذكي",
    description: String = "الصق رابطاً من يوتيوب، انستغرام، تيك توك، فيسبوك، أو أي موقع مخصص ليتم فحص دعمه وتصحيح أخطاء الصيغة فوراً قبل التشغيل:",
    confirmButtonText: String = "جلب وتشغيل",
    linkCheckState: LinkDiagnosticResult,
    isBusy: Boolean = false,
    onInspectLink: (String) -> Unit,
    onCheckLink: (String) -> Unit,
    onEnableSourceNow: (LinkSourceDetection) -> Unit,
    onAddCustomDomain: ((String) -> Unit)? = null,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var inputUrl by remember { mutableStateOf(linkCheckState.rawInput) }

    val canConfirm = inputUrl.isNotBlank() &&
        !isBusy &&
        linkCheckState.status != LinkCheckStatus.CHECKING &&
        linkCheckState.status != LinkCheckStatus.SOURCE_DISABLED &&
        linkCheckState.status != LinkCheckStatus.INVALID &&
        linkCheckState.status != LinkCheckStatus.UNSUPPORTED &&
        linkCheckState.isSupported &&
        linkCheckState.isSourceEnabled

    val showFixCard = inputUrl.isNotBlank() &&
        linkCheckState.hasMinorSyntaxErrors &&
        linkCheckState.correctedUrl.isNotBlank() &&
        linkCheckState.correctedUrl != inputUrl

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = YouTubeSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = YouTubeGold,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = description,
                    color = YouTubeTextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputUrl,
                    onValueChange = {
                        inputUrl = it
                        onInspectLink(it)
                    },
                    placeholder = {
                        Text(
                            text = "https://youtube.com/... أو t.co / fb.watch / vm.tiktok.com",
                            color = YouTubeTextSecondary,
                            fontSize = 11.sp
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                val pasted = clipboardManager.getText()?.text.orEmpty()
                                if (pasted.isNotBlank()) {
                                    inputUrl = pasted
                                    onInspectLink(pasted)
                                } else {
                                    Toast.makeText(context, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                .testTag("smart_link_paste_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "لصق من الحافظة",
                                tint = YouTubeGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = YouTubeRed,
                        unfocusedBorderColor = Color(0xFF444444),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("smart_link_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Support Status Pill + Pre-Flight Check Button
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (inputUrl.isNotBlank() && linkCheckState.status != LinkCheckStatus.IDLE) {
                        val badgeBg = when {
                            !linkCheckState.isSupported || linkCheckState.status == LinkCheckStatus.INVALID -> Color(0xFF3A1D1D)
                            !linkCheckState.isSourceEnabled -> Color(0xFF332211)
                            else -> Color(0xFF1E2B22)
                        }
                        val badgeBorder = when {
                            !linkCheckState.isSupported || linkCheckState.status == LinkCheckStatus.INVALID -> YouTubeRed
                            !linkCheckState.isSourceEnabled -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                        val badgeLabel = when {
                            linkCheckState.status == LinkCheckStatus.INVALID -> "صيغة غير صالحة"
                            !linkCheckState.isSupported -> "غير مدعوم: ${linkCheckState.detection.sourceLabelArabic}"
                            !linkCheckState.isSourceEnabled -> "معطّل: ${linkCheckState.detection.sourceLabelArabic}"
                            else -> "مدعوم: ${linkCheckState.detection.sourceLabelArabic}"
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(badgeBg)
                                .border(0.8.dp, badgeBorder, RoundedCornerShape(16.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                                .testTag("link_support_badge"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (linkCheckState.isSupported && linkCheckState.isSourceEnabled && linkCheckState.status != LinkCheckStatus.INVALID) {
                                    Icons.Default.Link
                                } else {
                                    Icons.Default.Block
                                },
                                contentDescription = null,
                                tint = badgeBorder,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = badgeLabel,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { onCheckLink(inputUrl) },
                        enabled = inputUrl.isNotBlank() && linkCheckState.status != LinkCheckStatus.CHECKING,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("check_link_button")
                    ) {
                        if (linkCheckState.status == LinkCheckStatus.CHECKING) {
                            CircularProgressIndicator(
                                color = YouTubeGold,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("جارٍ الفحص...", color = YouTubeGold, fontSize = 11.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = YouTubeGold,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text("فحص الرابط", color = YouTubeGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Instant Minor Syntax Fix Card with Dedicated 'Fix' Button
                AnimatedVisibility(
                    visible = showFixCard,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF231E12))
                            .border(0.9.dp, YouTubeGold.copy(alpha = 0.75f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                            .testTag("minor_syntax_fix_card")
                    ) {
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
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = null,
                                    tint = YouTubeGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "أخطاء صيغة بسيطة بالرابط",
                                    color = YouTubeGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    val fixed = linkCheckState.correctedUrl
                                    inputUrl = fixed
                                    onInspectLink(fixed)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = YouTubeGold),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("fix_link_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoFixHigh,
                                    contentDescription = "إصلاح الرابط",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "إصلاح (Fix)",
                                    color = Color.Black,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (linkCheckState.syntaxIssues.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            linkCheckState.syntaxIssues.forEach { issue ->
                                Text(
                                    text = "• $issue",
                                    color = Color(0xFFFFECB3),
                                    fontSize = 11.sp,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = linkCheckState.correctedUrl,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("corrected_url_preview_text")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            OutlinedButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(linkCheckState.correctedUrl))
                                    Toast.makeText(context, "تم نسخ الرابط المصحح", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .testTag("copy_corrected_link_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "نسخ الرابط المصحح",
                                    tint = YouTubeGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ", color = YouTubeGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Instant Diagnostic Status Banner
                val shouldShowStatusBanner = inputUrl.isNotBlank() &&
                    linkCheckState.status != LinkCheckStatus.IDLE &&
                    !(showFixCard && linkCheckState.status == LinkCheckStatus.FIXED_AND_VALID)

                AnimatedVisibility(
                    visible = shouldShowStatusBanner,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        when (linkCheckState.status) {
                            LinkCheckStatus.SOURCE_DISABLED -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF331B0E))
                                        .border(0.8.dp, Color(0xFFFF9800), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                        .testTag("disabled_source_warning_box")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.WarningAmber,
                                            contentDescription = null,
                                            tint = Color(0xFFFFB74D),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = linkCheckState.exactReason ?: "هذا المصدر معطّل حالياً من الإعدادات.",
                                            color = Color(0xFFFFE0B2),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = { onEnableSourceNow(linkCheckState.detection) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF6C00)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("enable_disabled_source_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PowerSettingsNew,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تفعيل المصدر الآن",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            LinkCheckStatus.UNSUPPORTED -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF301616))
                                        .border(0.8.dp, Color(0xFFFF7043), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                        .testTag("unsupported_link_box")
                                ) {
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = Color(0xFFFF8A65),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = linkCheckState.exactReason ?: "هذا الرابط غير مدعوم للتشغيل داخل التطبيق.",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    if (linkCheckState.canOfferCustomDomain && onAddCustomDomain != null && linkCheckState.detection.matchedDomain.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = {
                                                onAddCustomDomain(linkCheckState.detection.matchedDomain)
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .defaultMinSize(minHeight = 48.dp)
                                                .testTag("add_custom_domain_button")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AddCircleOutline,
                                                contentDescription = null,
                                                tint = YouTubeGold,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = "إضافة (${linkCheckState.detection.matchedDomain}) كموقع مخصص",
                                                color = YouTubeGold,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            LinkCheckStatus.INVALID -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF2D1215))
                                        .border(0.8.dp, YouTubeRed, RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                        .testTag("invalid_link_error_box")
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
                                            text = linkCheckState.exactReason ?: "الرابط غير صالح أو تعذر الوصول إليه.",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 17.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedButton(
                                        onClick = { onCheckLink(inputUrl) },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .defaultMinSize(minHeight = 48.dp)
                                            .testTag("retry_check_link_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            tint = YouTubeGold,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("إعادة المحاولة", color = YouTubeGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            LinkCheckStatus.VALID, LinkCheckStatus.FIXED_AND_VALID -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF132619))
                                        .border(0.8.dp, Color(0xFF4CAF50), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                        .testTag("valid_link_status_box"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF66BB6A),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val okMsg = buildString {
                                        append("الرابط مدعوم وصالح وجاهز للتشغيل ✓")
                                        linkCheckState.httpCode?.let { append(" (HTTP $it)") }
                                    }
                                    Text(
                                        text = okMsg,
                                        color = Color(0xFFC8E6C9),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

                            LinkCheckStatus.CHECKING -> {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF1E1E24))
                                        .border(0.6.dp, YouTubeGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        color = YouTubeGold,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "جارٍ التحقق الفعلي من حالة الرابط وتوفر البث...",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            LinkCheckStatus.IDLE -> Unit
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = linkCheckState.correctedUrl.ifBlank { inputUrl.trim() }
                    if (target.isNotBlank()) {
                        onConfirm(target)
                    }
                },
                enabled = canConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = YouTubeRed),
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("smart_link_confirm_button")
            ) {
                Text(
                    text = confirmButtonText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("smart_link_dismiss_button")
            ) {
                Text("إلغاء", color = YouTubeTextSecondary)
            }
        }
    )
}
