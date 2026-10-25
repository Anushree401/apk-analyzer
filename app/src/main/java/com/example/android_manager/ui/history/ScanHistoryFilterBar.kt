package com.example.android_manager.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanType
import com.example.android_manager.ui.components.cardInteraction
import com.example.android_manager.ui.components.keyboardFocusable
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor

enum class HistorySortOption(val displayName: String) {
    DATE_DESC("Newest"),
    DATE_ASC("Oldest"),
    SCORE_DESC("Highest Risk"),
    SCORE_ASC("Lowest Risk")
}

/**
 * Interactive filter bar providing search querying, risk severity chips,
 * scan-type filters, and sort controls.
 */
@Composable
fun ScanHistoryFilterBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedRiskFilter: RiskLevel?,
    onRiskFilterSelect: (RiskLevel?) -> Unit,
    selectedScanType: ScanType? = null,
    onScanTypeSelect: (ScanType?) -> Unit = {},
    selectedSort: HistorySortOption = HistorySortOption.DATE_DESC,
    onSortSelect: (HistorySortOption) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // Search Input Box
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(8.dp))
                .border(1.dp, Border, RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Search",
                tint = SecondaryText,
                modifier = Modifier.size(18.dp)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Search by app or package name...",
                        color = MutedText,
                        fontSize = 13.sp
                    )
                }

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = PrimaryText,
                        fontSize = 13.sp
                    ),
                    cursorBrush = SolidColor(CyberCyan),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Clear search",
                    tint = SecondaryText,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSearchQueryChange("")
                            focusManager.clearFocus()
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Horizontal filter chips: Risk Severity
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChipItem(
                label = "ALL",
                isSelected = selectedRiskFilter == null,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onRiskFilterSelect(null)
                }
            )

            RiskLevel.entries.forEach { level ->
                FilterChipItem(
                    label = level.name,
                    isSelected = selectedRiskFilter == level,
                    riskLevel = level,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRiskFilterSelect(if (selectedRiskFilter == level) null else level)
                    }
                )
            }

            // Divider chip
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "•", color = MutedText, fontSize = 12.sp)
            }

            // Scan Type filter chips (Phase 4.4)
            FilterChipItem(
                label = "APK FILES",
                isSelected = selectedScanType == ScanType.APK_FILE,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onScanTypeSelect(if (selectedScanType == ScanType.APK_FILE) null else ScanType.APK_FILE)
                }
            )

            FilterChipItem(
                label = "INSTALLED",
                isSelected = selectedScanType == ScanType.INSTALLED_APP,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onScanTypeSelect(if (selectedScanType == ScanType.INSTALLED_APP) null else ScanType.INSTALLED_APP)
                }
            )

            // Divider chip
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "•", color = MutedText, fontSize = 12.sp)
            }

            // Sort Toggle Chip (Phase 2.1)
            FilterChipItem(
                label = "SORT: ${selectedSort.displayName.uppercase()}",
                isSelected = true,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Sort,
                        contentDescription = "Sort order",
                        tint = CyberCyan,
                        modifier = Modifier.size(13.dp)
                    )
                },
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val next = when (selectedSort) {
                        HistorySortOption.DATE_DESC -> HistorySortOption.SCORE_DESC
                        HistorySortOption.SCORE_DESC -> HistorySortOption.SCORE_ASC
                        HistorySortOption.SCORE_ASC -> HistorySortOption.DATE_ASC
                        HistorySortOption.DATE_ASC -> HistorySortOption.DATE_DESC
                    }
                    onSortSelect(next)
                }
            )
        }
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    isSelected: Boolean,
    riskLevel: RiskLevel? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val activeColor = riskLevel?.color() ?: CyberCyan
    val containerBg = if (isSelected) {
        riskLevel?.containerColor() ?: CardBackground
    } else {
        Surface
    }
    val borderColor = if (isSelected) activeColor else Border
    val textColor = if (isSelected) activeColor else SecondaryText

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 44.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .keyboardFocusable(shape = RoundedCornerShape(8.dp))
            .cardInteraction(shape = RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text = label,
                color = textColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                letterSpacing = 0.5.sp
            )
        }
    }
}

