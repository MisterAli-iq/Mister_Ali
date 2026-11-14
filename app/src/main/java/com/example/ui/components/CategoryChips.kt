package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SongCategory
import com.example.ui.theme.LocalCustomTheme

@Composable
fun CategoryChips(
    selectedCategory: SongCategory,
    onCategorySelected: (SongCategory) -> Unit,
    hideBar: Boolean = false,
    hiddenCategories: Set<SongCategory> = emptySet(),
    modifier: Modifier = Modifier
) {
    if (hideBar) return

    val customTheme = LocalCustomTheme.current
    val scrollState = rememberScrollState()
    val visibleCategories = SongCategory.entries.filter { cat ->
        cat == SongCategory.ALL || !hiddenCategories.contains(cat)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(customTheme.background)
            .padding(vertical = 8.dp)
            .horizontalScroll(scrollState)
            .testTag("category_chips_bar"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.padding(start = 8.dp))

        visibleCategories.forEach { category ->
            val isSelected = category == selectedCategory
            val bgColor = if (isSelected) {
                if (customTheme.isDark) Color.White else Color(0xFF141414)
            } else {
                customTheme.surfaceVariant
            }
            val textColor = if (isSelected) {
                if (customTheme.isDark) Color.Black else Color.White
            } else {
                customTheme.textPrimary
            }

            Box(
                modifier = Modifier
                    .defaultMinSize(minHeight = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(
                        width = if (isSelected) 0.dp else 0.5.dp,
                        color = if (isSelected) Color.Transparent else customTheme.border,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
                    .testTag("category_chip_${category.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.displayName,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }

        Box(modifier = Modifier.padding(end = 8.dp))
    }
}
