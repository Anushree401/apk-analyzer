package com.example.android_manager.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.android_manager.ui.theme.SecondaryText
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ScanCompareDialog(
    baselineScan: ScanHistoryEntry,
    currentScan: ScanHistoryEntry,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    val baselineDate = remember(baselineScan.timestamp) { dateFormat.format(Date(baselineScan.timestamp)) }
    val currentDate = remember(currentScan.timestamp) { dateFormat.format(Date(currentScan.timestamp)) }

    val scoreDelta = currentScan.riskScore - baselineScan.riskScore

    // Finding diffs
    val baselineNames = remember(baselineScan.findings) { baselineScan.findings.map { it.name }.toSet() }
    val currentNames = remember(currentScan.findings) { currentScan.findings.map { it.name }.toSet() }

    val newFindings = remember(currentScan.findings, baselineNames) {
        currentScan.findings.filter { it.name !in baselineNames }
    }
    val resolvedFindings = remember(baselineScan.findings, currentNames) {
        baselineScan.findings.filter { it.name !in currentNames }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxSize(0.88f)
                .clip(RoundedCornerShape(20.dp))
                .border(1.dp, Border, RoundedCornerShape(20.dp)),
            color = CardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "SECURITY SCAN COMPARISON",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = currentScan.targetName,
                            color = PrimaryText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentScan.packageName,
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close comparison",
                            tint = SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScrollbar(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Score Comparison Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(com.example.android_manager.ui.theme.Surface)
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                "OVERALL RISK SCORE DELTA",
                                color = SecondaryText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "BASELINE (${baselineScan.versionName})",
                                        color = SecondaryText,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        "${baselineScan.riskScore}/100",
                                        color = PrimaryText,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(baselineDate, color = SecondaryText, fontSize = 10.sp)
                                }

                                // Delta indicator badge
                                val deltaText = when {
                                    scoreDelta > 0 -> "+$scoreDelta INCREASED"
                                    scoreDelta < 0 -> "$scoreDelta IMPROVED"
                                    else -> "UNCHANGED"
                                }
                                val deltaColor = when {
                                    scoreDelta > 0 -> Color(0xFFFF5252)
                                    scoreDelta < 0 -> CyberCyan
                                    else -> SecondaryText
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(deltaColor.copy(alpha = 0.15f))
                                        .border(1.dp, deltaColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = deltaText,
                                        color = deltaColor,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "CURRENT (${currentScan.versionName})",
                                        color = SecondaryText,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        "${currentScan.riskScore}/100",
                                        color = PrimaryText,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(currentDate, color = SecondaryText, fontSize = 10.sp)
                                }
                            }
                        }
                    }

                    // Severity breakdown counts
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(com.example.android_manager.ui.theme.Surface)
                                .border(1.dp, Border, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                "SEVERITY BREAKDOWN TRANSITIONS",
                                color = SecondaryText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            SeverityDiffRow("Critical", baselineScan.criticalCount, currentScan.criticalCount, Color(0xFFFF5252))
                            Spacer(Modifier.height(6.dp))
                            SeverityDiffRow("High", baselineScan.highCount, currentScan.highCount, Color(0xFFFF7043))
                            Spacer(Modifier.height(6.dp))
                            SeverityDiffRow("Medium", baselineScan.mediumCount, currentScan.mediumCount, Color(0xFFFFB300))
                            Spacer(Modifier.height(6.dp))
                            SeverityDiffRow("Low", baselineScan.lowCount, currentScan.lowCount, CyberCyan)
                        }
                    }

                    // New Vulnerabilities Introduced
                    if (newFindings.isNotEmpty()) {
                        item {
                            Text(
                                text = "NEW VULNERABILITIES DETECTED (${newFindings.size})",
                                color = Color(0xFFFF5252),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        items(newFindings.size) { index ->
                            val finding = newFindings[index]
                            FindingDiffTile(
                                name = finding.name,
                                component = finding.affectedComponent ?: "",
                                status = "NEW ISSUE",
                                statusColor = Color(0xFFFF5252)
                            )
                        }
                    }

                    // Resolved Vulnerabilities
                    if (resolvedFindings.isNotEmpty()) {
                        item {
                            Text(
                                text = "RESOLVED ISSUES (${resolvedFindings.size})",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        items(resolvedFindings.size) { index ->
                            val finding = resolvedFindings[index]
                            FindingDiffTile(
                                name = finding.name,
                                component = finding.affectedComponent ?: "",
                                status = "RESOLVED",
                                statusColor = CyberCyan
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityDiffRow(
    label: String,
    baselineCount: Int,
    currentCount: Int,
    accentColor: Color
) {
    val diff = currentCount - baselineCount
    val diffText = if (diff > 0) "+$diff" else if (diff < 0) "$diff" else "±0"
    val diffColor = if (diff > 0) Color(0xFFFF5252) else if (diff < 0) CyberCyan else SecondaryText

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(modifier = Modifier.size(8.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
            Text(label, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("$baselineCount → $currentCount", color = SecondaryText, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            Text(
                text = "($diffText)",
                color = diffColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun FindingDiffTile(
    name: String,
    component: String,
    status: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(com.example.android_manager.ui.theme.Surface)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(name, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            if (component.isNotBlank()) {
                Text(component, color = SecondaryText, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(statusColor.copy(alpha = 0.15f))
                .border(1.dp, statusColor.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) {
            Text(status, color = statusColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}
