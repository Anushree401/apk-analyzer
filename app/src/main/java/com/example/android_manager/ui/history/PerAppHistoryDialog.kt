package com.example.android_manager.ui.history

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.example.android_manager.model.VulnerabilityFinding
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.ui.components.verticalScrollbar
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskCritical
import com.example.android_manager.ui.theme.RiskHigh
import com.example.android_manager.ui.theme.RiskLow
import com.example.android_manager.ui.theme.RiskMedium
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PerAppHistoryDialog(
    packageName: String,
    allScans: List<ScanHistoryEntry>,
    onDismiss: () -> Unit,
    onCompare: (ScanHistoryEntry, ScanHistoryEntry) -> Unit
) {
    val scans = remember(packageName, allScans) {
        allScans.filter { it.packageName == packageName }.sortedByDescending { it.timestamp }
    }

    val appName = scans.firstOrNull()?.targetName ?: packageName
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxSize(0.80f)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, Border, RoundedCornerShape(18.dp)),
            color = CardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timeline,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "PACKAGE SCAN TIMELINE",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = appName,
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = packageName,
                    color = SecondaryText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${scans.size} audit records logged on this device",
                    color = CyberCyan,
                    fontSize = 11.sp
                )

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(Border)
                )

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScrollbar(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(
                        items = scans,
                        key = { index, item -> "${item.id}_${item.timestamp}_$index" }
                    ) { index, scan ->
                        val olderScan = if (index < scans.size - 1) scans[index + 1] else null

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(com.example.android_manager.ui.theme.Surface)
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(scan.riskLevel.color())
                                    )
                                    Text(
                                        text = "v${scan.versionName.ifBlank { "N/A" }}",
                                        color = PrimaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = dateFormatter.format(Date(scan.timestamp)),
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Risk Score: ${scan.riskScore}/100 [${scan.riskLevel.name}]",
                                    color = scan.riskLevel.color(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                Text(
                                    text = "${scan.findingsCount} findings",
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }

                            // If there is an older scan, show score trend and Compare button
                            if (olderScan != null) {
                                val scoreDiff = scan.riskScore - olderScan.riskScore
                                Spacer(Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val trendText = when {
                                        scoreDiff > 0 -> "Risk increased by +$scoreDiff vs previous scan"
                                        scoreDiff < 0 -> "Risk decreased by $scoreDiff (improved)"
                                        else -> "Risk score unchanged vs previous scan"
                                    }
                                    val trendColor = when {
                                        scoreDiff > 0 -> RiskHigh
                                        scoreDiff < 0 -> RiskLow
                                        else -> SecondaryText
                                    }

                                    Text(
                                        text = trendText,
                                        color = trendColor,
                                        fontSize = 10.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(CardBackground)
                                            .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                onCompare(olderScan, scan)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                            contentDescription = "Compare scans",
                                            tint = CyberCyan,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "COMPARE",
                                            color = CyberCyan,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
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
}

data class PerAppScanDiff(
    val scoreDelta: Int,
    val newFindings: List<VulnerabilityFinding>,
    val resolvedFindings: List<VulnerabilityFinding>
)

fun calculateScanDiff(
    newerScore: Int,
    olderScore: Int,
    newerFindings: List<VulnerabilityFinding>,
    olderFindings: List<VulnerabilityFinding>
): PerAppScanDiff {
    val olderNames = olderFindings.map { it.name }.toSet()
    val newerNames = newerFindings.map { it.name }.toSet()

    val newFindings = newerFindings.filter { it.name !in olderNames }
    val resolvedFindings = olderFindings.filter { it.name !in newerNames }

    return PerAppScanDiff(
        scoreDelta = newerScore - olderScore,
        newFindings = newFindings,
        resolvedFindings = resolvedFindings
    )
}
