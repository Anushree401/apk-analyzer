package com.example.android_manager.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.ui.scanner.ApkFindingCard
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskLow
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet displaying in-depth security analysis findings and metadata for a scan entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryDetailSheet(
    entry: ScanHistoryEntry,
    onDismiss: () -> Unit,
    onViewFullReport: ((Long) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val riskColor = entry.riskLevel.color()
    val riskContainerColor = entry.riskLevel.containerColor()
    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header with Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AUDIT INSPECTION",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.targetName,
                        color = PrimaryText,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close sheet",
                        tint = SecondaryText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                text = entry.packageName,
                color = SecondaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Risk Assessment Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(riskContainerColor, RoundedCornerShape(10.dp))
                    .border(1.dp, riskColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Shield,
                            contentDescription = null,
                            tint = riskColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "RISK: ${entry.riskLevel.name}",
                                color = riskColor,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Status: ${entry.status.displayName}",
                                color = PrimaryText,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${entry.riskScore}",
                            color = riskColor,
                            fontSize = 24.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "/ 100",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackground, RoundedCornerShape(10.dp))
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MetadataRow(label = "Version", value = if (entry.versionName.isNotBlank()) "${entry.versionName} (${entry.versionCode})" else "N/A")
                MetadataRow(label = "SDK Support", value = "Min SDK ${entry.minSdk} • Target SDK ${entry.targetSdk}")
                MetadataRow(label = "Audit Date", value = formattedDate)
                MetadataRow(label = "Scan Category", value = entry.scanType.displayName)
                if (entry.scanDurationMs > 0) {
                    MetadataRow(label = "Scan Duration", value = "${entry.scanDurationMs} ms")
                }
            }

            if (onViewFullReport != null) {
                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onViewFullReport(entry.id)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("VIEW COMPLETE SECURITY REPORT")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Findings Section
            Text(
                text = "FINDINGS (${entry.findingsCount})",
                color = PrimaryText,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (entry.findings.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardBackground, RoundedCornerShape(10.dp))
                        .border(1.dp, Border, RoundedCornerShape(10.dp))
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = RiskLow,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "No vulnerability findings detected.",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(entry.findings) { finding ->
                        ApkFindingCard(finding = finding)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            color = PrimaryText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
