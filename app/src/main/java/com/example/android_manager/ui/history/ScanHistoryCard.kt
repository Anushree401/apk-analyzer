package com.example.android_manager.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskCritical
import com.example.android_manager.ui.theme.RiskHigh
import com.example.android_manager.ui.theme.RiskLow
import com.example.android_manager.ui.theme.RiskMedium
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Card displaying an individual scan history record with risk indicators and findings tally.
 */
@Composable
fun ScanHistoryCard(
    entry: ScanHistoryEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val riskColor = entry.riskLevel.color()
    val riskContainerColor = entry.riskLevel.containerColor()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Target Name, Risk Badge, and Delete Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(riskContainerColor, RoundedCornerShape(8.dp))
                            .border(1.dp, riskColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = entry.targetName,
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = entry.packageName,
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Risk Badge
                Box(
                    modifier = Modifier
                        .background(riskContainerColor, RoundedCornerShape(6.dp))
                        .border(1.dp, riskColor, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = entry.riskLevel.name,
                        color = riskColor,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "Delete record",
                        tint = MutedText,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Severity Breakdown & Score
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.criticalCount > 0) {
                        SeverityTag(count = entry.criticalCount, label = "CRIT", color = RiskCritical)
                    }
                    if (entry.highCount > 0) {
                        SeverityTag(count = entry.highCount, label = "HIGH", color = RiskHigh)
                    }
                    if (entry.mediumCount > 0) {
                        SeverityTag(count = entry.mediumCount, label = "MED", color = RiskMedium)
                    }
                    if (entry.lowCount > 0) {
                        SeverityTag(count = entry.lowCount, label = "LOW", color = RiskLow)
                    }
                    if (entry.findingsCount == 0) {
                        SeverityTag(count = 0, label = "CLEAN", color = RiskLow)
                    }
                }

                Text(
                    text = "Score: ${entry.riskScore}/100",
                    color = riskColor,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Footer Row: Scan Type & Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.scanType.displayName.uppercase(),
                    color = MutedText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                Text(
                    text = formatRelativeTime(entry.timestamp),
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun SeverityTag(
    count: Int,
    label: String,
    color: Color
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = if (count > 0) "$count $label" else label,
            color = color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
