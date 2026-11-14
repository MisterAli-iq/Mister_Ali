package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSettings
import com.example.model.LinkSourceConfig
import com.example.model.LinkSourceType
import com.example.model.LocalMediaType
import com.example.model.PreferredQuality
import com.example.model.SongCategory
import com.example.ui.theme.CustomThemePalette
import com.example.ui.theme.YouTubeGold
import com.example.ui.theme.YouTubeRed

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LinkSourcesSettingsSection(
    settings: AppSettings,
    accentHex: Color,
    customTheme: CustomThemePalette,
    onSetAllEnabled: (Boolean) -> Unit,
    onSetSourceEnabled: (LinkSourceType, Boolean) -> Unit,
    onUpdateSourceConfig: (LinkSourceType, LinkSourceConfig) -> Unit,
    onUpdateCustomSourceConfig: (LinkSourceConfig) -> Unit,
    onAddCustomDomain: (String) -> Unit,
    onRemoveCustomDomain: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedSourceKey by remember { mutableStateOf<String?>(null) }
    var customDomainInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("link_sources_settings_section")
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = customTheme.surface),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Master "All Sources" Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الكل (تفعيل جميع المصادر الـ 12)",
                            color = customTheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "تحديد المواقع التي يشملها البحث وجلب الروابط داخل التطبيق",
                            color = customTheme.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = settings.allLinkSourcesEnabled,
                        onCheckedChange = onSetAllEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentHex
                        ),
                        modifier = Modifier.testTag("link_sources_all_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // All 12 Independent Source Cards + OTHER
                LinkSourceType.entries.forEachIndexed { idx, source ->
                    val cfg = settings.configFor(source)
                    val isExpanded = expandedSourceKey == source.key

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(customTheme.surfaceVariant)
                            .border(
                                width = 0.6.dp,
                                color = if (cfg.enabled) accentHex.copy(alpha = 0.35f) else customTheme.border,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (source == LinkSourceType.OTHER) Icons.Default.Public else Icons.Default.PlayCircle,
                                contentDescription = null,
                                tint = if (cfg.enabled) YouTubeGold else customTheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        expandedSourceKey = if (isExpanded) null else source.key
                                    }
                            ) {
                                Text(
                                    text = source.titleArabic,
                                    color = customTheme.textPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${source.subtitleArabic} • ${shortQualityLabel(cfg.quality)} • ${cfg.targetCategory.displayName}",
                                    color = customTheme.textSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = {
                                    expandedSourceKey = if (isExpanded) null else source.key
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("link_source_expand_${source.key.lowercase()}")
                            ) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "إعدادات ${source.titleArabic}",
                                    tint = customTheme.textSecondary
                                )
                            }

                            Switch(
                                checked = cfg.enabled,
                                onCheckedChange = { onSetSourceEnabled(source, it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentHex
                                ),
                                modifier = Modifier.testTag("link_source_switch_${source.key.lowercase()}")
                            )
                        }

                        AnimatedVisibility(visible = isExpanded) {
                            PerSourceOptionsPanel(
                                config = cfg,
                                accentHex = accentHex,
                                customTheme = customTheme,
                                onUpdateConfig = { onUpdateSourceConfig(source, it) }
                            )
                        }
                    }

                    if (idx < LinkSourceType.entries.lastIndex) {
                        Spacer(modifier = Modifier.height(2.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Custom Website / Domain Section
                val customExpanded = expandedSourceKey == "CUSTOM"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                expandedSourceKey = if (customExpanded) null else "CUSTOM"
                            }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = YouTubeGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "رابط مخصص (مواقع إضافية)",
                                color = customTheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أضف أي نطاق موقع مثل vimeo.com أو dailymotion.com",
                                color = customTheme.textSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = { expandedSourceKey = if (customExpanded) null else "CUSTOM" },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "إعدادات الروابط المخصصة",
                            tint = customTheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Switch(
                        checked = settings.customSourceConfig.enabled,
                        onCheckedChange = {
                            onUpdateCustomSourceConfig(settings.customSourceConfig.copy(enabled = it))
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = accentHex
                        ),
                        modifier = Modifier.testTag("link_source_switch_custom")
                    )
                }

                AnimatedVisibility(visible = customExpanded) {
                    PerSourceOptionsPanel(
                        config = settings.customSourceConfig,
                        accentHex = accentHex,
                        customTheme = customTheme,
                        onUpdateConfig = onUpdateCustomSourceConfig
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customDomainInput,
                        onValueChange = { customDomainInput = it },
                        placeholder = {
                            Text(
                                text = "مثال: vimeo.com",
                                color = customTheme.textSecondary,
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentHex,
                            unfocusedBorderColor = customTheme.border,
                            focusedTextColor = customTheme.textPrimary,
                            unfocusedTextColor = customTheme.textPrimary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("custom_link_domain_input")
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (customDomainInput.isNotBlank()) {
                                onAddCustomDomain(customDomainInput)
                                customDomainInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentHex),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("add_custom_domain_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "إضافة موقع مخصص",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إضافة", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (settings.customLinkDomains.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        settings.customLinkDomains.forEach { domain ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(customTheme.surfaceVariant)
                                    .border(0.7.dp, accentHex.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                    .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = domain,
                                    color = customTheme.textPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = { onRemoveCustomDomain(domain) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "حذف $domain",
                                        tint = YouTubeRed,
                                        modifier = Modifier.size(14.dp)
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

@Composable
private fun PerSourceOptionsPanel(
    config: LinkSourceConfig,
    accentHex: Color,
    customTheme: CustomThemePalette,
    onUpdateConfig: (LinkSourceConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        HorizontalDivider(color = customTheme.border, thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "الجودة الافتراضية لهذا المصدر",
            color = customTheme.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            PreferredQuality.entries.forEach { q ->
                val selected = config.quality == q
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) accentHex else customTheme.background)
                        .clickable { onUpdateConfig(config.copy(quality = q)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = shortQualityLabel(q),
                        color = if (selected) Color.White else customTheme.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "فئة الحفظ التلقائية",
            color = customTheme.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            SongCategory.entries.filter { it != SongCategory.ALL }.forEach { cat ->
                val selected = config.targetCategory == cat
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) accentHex else customTheme.background)
                        .clickable { onUpdateConfig(config.copy(targetCategory = cat)) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = cat.displayName,
                        color = if (selected) Color.White else customTheme.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "نوع الوسائط",
            color = customTheme.textSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            LocalMediaType.entries.forEach { mt ->
                val selected = config.mediaType == mt
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) accentHex else customTheme.background)
                        .clickable { onUpdateConfig(config.copy(mediaType = mt)) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = mt.titleArabic,
                        color = if (selected) Color.White else customTheme.textSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "التشغيل التلقائي فور الاختيار",
                color = customTheme.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = config.autoPlay,
                onCheckedChange = { onUpdateConfig(config.copy(autoPlay = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentHex)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "السماح بالتحميل من هذا المصدر",
                color = customTheme.textPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Switch(
                checked = config.allowDownload,
                onCheckedChange = { onUpdateConfig(config.copy(allowDownload = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentHex)
            )
        }
    }
}

fun shortQualityLabel(q: PreferredQuality): String = when (q) {
    PreferredQuality.AUTO -> "تلقائي"
    PreferredQuality.HIGH -> "1080p"
    PreferredQuality.MEDIUM -> "720p"
    PreferredQuality.STANDARD_480 -> "480p"
    PreferredQuality.LOW_360 -> "360p"
    PreferredQuality.DATA_SAVER -> "توفير البيانات"
}
