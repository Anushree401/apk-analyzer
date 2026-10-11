package com.example.android_manager.ui.report

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.data.report.SecurityReportRepository
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.SecurityReport
import com.example.android_manager.navigation.Screen
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskLow
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ReportSectionTab(val title: String) {
    FINDINGS("FINDINGS"),
    PERMISSIONS("PERMISSIONS"),
    ATTACK_SURFACE("ATTACK SURFACE"),
    REMEDIATION("REMEDIATION")
}

/**
 * Main Security Report and Explanation screen presenting executive telemetry,
 * filterable vulnerability deep-dives, attack surface exposure, and developer mitigations.
 */
@Composable
fun SecurityReportScreen(
    targetPackageName: String? = null,
    historyId: Long? = null,
    onBack: () -> Unit = {},
    onNavigate: ((Screen) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { SecurityReportRepository(context) }
    val scope = rememberCoroutineScope()

    var report by remember { mutableStateOf<SecurityReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedTab by remember { mutableStateOf(ReportSectionTab.FINDINGS) }
    var selectedSeverityFilter by remember { mutableStateOf<RiskLevel?>(null) }

    // Load active report from cache or synthesize from parameters
    LaunchedEffect(targetPackageName, historyId) {
        isLoading = true
        errorMessage = null

        withContext(Dispatchers.IO) {
            try {
                val cached = SecurityReportRepository.getActiveReport()
                if (cached != null) {
                    report = cached
                } else if (historyId != null && historyId > 0L) {
                    report = repository.buildFromHistoryId(historyId)
                } else if (!targetPackageName.isNullOrBlank()) {
                    report = repository.buildFromInstalledApp(targetPackageName)
                } else {
                    // Fallback to APVM self package or first available app
                    val selfPkg = context.packageName
                    report = repository.buildFromInstalledApp(selfPkg)
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to assemble security report."
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        if (isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = CyberCyan)
            }
        } else if (report != null) {
            val currentReport = report!!

            // Filter findings by selected severity chip
            val filteredFindings = remember(currentReport, selectedSeverityFilter) {
                if (selectedSeverityFilter == null) {
                    currentReport.vulnerabilityFindings
                } else {
                    currentReport.vulnerabilityFindings.filter { it.severity == selectedSeverityFilter }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Target Metadata Header
                item {
                    ReportTargetHeader(
                        report = currentReport,
                        onShareClick = {
                            val shareText = repository.formatReportForExport(currentReport)
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "APVM Security Audit Report - ${currentReport.metadata.appName}")
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Security Audit Report"))
                        }
                    )
                }

                // Executive Posture & Score Gauge Card
                item {
                    ReportSummaryCard(
                        summary = currentReport.summary,
                        selectedSeverity = selectedSeverityFilter,
                        onSeveritySelect = { selectedSeverityFilter = it }
                    )
                }

                // Navigation Tabs
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReportSectionTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            val countBadge = when (tab) {
                                ReportSectionTab.FINDINGS -> currentReport.vulnerabilityFindings.size
                                ReportSectionTab.PERMISSIONS -> currentReport.dangerousPermissions.size
                                ReportSectionTab.ATTACK_SURFACE -> currentReport.componentBreakdown.totalExported
                                ReportSectionTab.REMEDIATION -> currentReport.recommendations.size
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else Surface)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) CyberCyan else Border,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedTab = tab }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = tab.title,
                                        color = if (isSelected) CyberCyan else SecondaryText,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (isSelected) CyberCyan.copy(alpha = 0.25f) else BorderSubtle,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = "$countBadge",
                                            color = if (isSelected) CyberCyan else SecondaryText,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Tab Content Switcher
                when (selectedTab) {
                    ReportSectionTab.FINDINGS -> {
                        if (filteredFindings.isEmpty()) {
                            item {
                                CleanFindingsState()
                            }
                        } else {
                            items(filteredFindings, key = { "${it.name}_${it.affectedComponent}" }) { finding ->
                                ReportFindingCard(finding = finding)
                            }
                        }
                    }

                    ReportSectionTab.PERMISSIONS -> {
                        item {
                            PermissionsAuditSection(report = currentReport)
                        }
                    }

                    ReportSectionTab.ATTACK_SURFACE -> {
                        item {
                            AttackSurfaceSection(report = currentReport)
                        }
                    }

                    ReportSectionTab.REMEDIATION -> {
                        item {
                            RemediationRoadmapSection(report = currentReport)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = errorMessage ?: "No security report data available.",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            }
        }

        APVMBottomBar(
            currentScreen = Screen.SecurityReport,
            onNavigate = { screen -> onNavigate?.invoke(screen) }
        )
    }
}

@Composable
private fun ReportTargetHeader(
    report: SecurityReport,
    onShareClick: () -> Unit
) {
    val meta = report.metadata
    val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(meta.scanTimestamp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meta.appName,
                    color = PrimaryText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = meta.packageName,
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(CyberCyan.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = meta.scanType.displayName.uppercase(),
                        color = CyberCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Export Report",
                        tint = CyberCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Version: ${meta.versionName ?: "N/A"}${meta.targetSdk?.let { " (SDK $it)" } ?: ""}",
                color = SecondaryText,
                fontSize = 11.sp
            )
            Text(
                text = formattedDate,
                color = SecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun CleanFindingsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "NO MATCHING FINDINGS",
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "No vulnerabilities detected for the active filter selection.",
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PermissionsAuditSection(report: SecurityReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (report.detectedCombinations.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "DANGEROUS PERMISSION COMBINATIONS",
                    color = report.detectedCombinations.first().severity.color(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                report.detectedCombinations.forEach { combo ->
                    Text(
                        text = "• ${combo.name}",
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = combo.dangerSummary,
                        color = SecondaryText,
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 6.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Border, RoundedCornerShape(12.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "DANGEROUS RUNTIME PERMISSIONS (${report.dangerousPermissions.size})",
                color = PrimaryText,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (report.dangerousPermissions.isEmpty()) {
                Text(
                    text = "No dangerous runtime permissions requested.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            } else {
                report.dangerousPermissions.forEach { perm ->
                    Text(
                        text = "• ${perm.name}",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttackSurfaceSection(report: SecurityReport) {
    val breakdown = report.componentBreakdown
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "COMPONENT EXPOSURE BREAKDOWN",
            color = SecondaryText,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ExposureItem("Activities", breakdown.totalExportedActivities, Modifier.weight(1f))
            ExposureItem("Services", breakdown.totalExportedServices, Modifier.weight(1f))
            ExposureItem("Receivers", breakdown.totalExportedReceivers, Modifier.weight(1f))
            ExposureItem("Providers", breakdown.totalExportedProviders, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ExposureItem(label: String, count: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(BorderSubtle, RoundedCornerShape(8.dp))
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            color = if (count > 0) PrimaryText else SecondaryText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.uppercase(),
            color = SecondaryText,
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun RemediationRoadmapSection(report: SecurityReport) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (report.recommendations.isEmpty()) {
            CleanFindingsState()
        } else {
            report.recommendations.forEachIndexed { index, rec ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}. ${rec.title}",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .background(rec.priority.containerColor(), RoundedCornerShape(4.dp))
                                .border(1.dp, rec.priority.color().copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = rec.priority.name,
                                color = rec.priority.color(),
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = rec.action,
                        color = SecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}