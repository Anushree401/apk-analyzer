package com.example.android_manager.ui.report

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.android_manager.data.report.SecurityReportRepository
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.SecurityReport
import com.example.android_manager.navigation.Screen
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.verticalScrollbar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.MutedText
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
    val snackbarHostState = remember { SnackbarHostState() }

    var report by remember { mutableStateOf<SecurityReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedTabName by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf(ReportSectionTab.FINDINGS.name)
    }
    val selectedTab = ReportSectionTab.valueOf(selectedTabName)

    var selectedSeverityFilterName by androidx.compose.runtime.saveable.rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val selectedSeverityFilter = selectedSeverityFilterName?.let { RiskLevel.valueOf(it) }

    var showCalculationExplainer by remember { mutableStateOf(false) }
    var exportContentToSave by remember { mutableStateOf<String?>(null) }
    var exportMimeType by remember { mutableStateOf("text/markdown") }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(exportMimeType)
    ) { uri ->
        if (uri != null && exportContentToSave != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        exportContentToSave?.let { content ->
                            stream.write(content.toByteArray())
                        }
                    }
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Report exported successfully.")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        snackbarHostState.showSnackbar("Export failed: ${e.message}")
                    }
                }
            }
        }
    }

    BackHandler(enabled = selectedSeverityFilterName != null || selectedTab != ReportSectionTab.FINDINGS) {
        if (selectedSeverityFilterName != null) {
            selectedSeverityFilterName = null
        } else if (selectedTab != ReportSectionTab.FINDINGS) {
            selectedTabName = ReportSectionTab.FINDINGS.name
        }
    }


    LaunchedEffect(targetPackageName, historyId) {
        isLoading = true
        errorMessage = null

        try {
            val loadedReport = withContext(Dispatchers.IO) {
                when {
                    historyId != null && historyId > 0L -> {
                        repository.buildFromHistoryId(historyId)
                    }

                    !targetPackageName.isNullOrBlank() -> {
                        repository.buildFromInstalledApp(targetPackageName)
                    }

                    else -> {
                        SecurityReportRepository.getActiveReport()
                    }
                }
            }

            report = loadedReport

            if (loadedReport == null) {
                errorMessage =
                    "No report available. Run an assessment or select a saved scan from History."
            }
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Failed to assemble security report."
        } finally {
            isLoading = false
        }
    }



    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { APVMTopBar(onBack = onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
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
                                try {
                                    val shareText = repository.formatReportForExport(currentReport)
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "APVM Security Audit Report - ${currentReport.metadata.appName}")
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    val chooser = Intent.createChooser(sendIntent, "Export Security Audit Report").apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(chooser)
                                } catch (_: Exception) {}
                            },
                            onExportMarkdown = {
                                exportContentToSave = repository.formatReportForExport(currentReport)
                                exportMimeType = "text/markdown"
                                createDocumentLauncher.launch("apvm_report_${currentReport.metadata.packageName}.md")
                            },
                            onExportJson = {
                                exportContentToSave = repository.formatReportAsJson(currentReport)
                                exportMimeType = "application/json"
                                createDocumentLauncher.launch("apvm_report_${currentReport.metadata.packageName}.json")
                            }
                        )
                    }

                    // Executive Posture & Score Gauge Card
                    item {
                        ReportSummaryCard(
                            summary = currentReport.summary,
                            selectedSeverity = selectedSeverityFilter,
                            onSeveritySelect = { selectedSeverityFilterName = it?.name }
                        )
                    }

                    // How Scores Are Calculated Action
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBackground)
                                .border(1.dp, Border, RoundedCornerShape(10.dp))
                                .clickable { showCalculationExplainer = true }
                                .padding(vertical = 12.dp, horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "HOW THREAT SCORES ARE CALCULATED ℹ",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
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
                                    .clickable { selectedTabName = tab.name }
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
                            itemsIndexed(
                                items = filteredFindings,
                                key = { index, finding -> "${finding.name}_${finding.affectedComponent}_${finding.severity.name}_$index" }
                            ) { _, finding ->
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBackground)
                            .border(1.dp, Border, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = SecondaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "HEURISTIC STATIC ANALYSIS DISCLAIMER",
                                color = SecondaryText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "APVM static analysis heuristics provide defensive triage indicators and automated security risk estimates based on local manifest declarations and DEX bytecodes. They do not constitute formal commercial certification or dynamic execution analysis.",
                            color = MutedText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(30.dp))
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
    }
}

    if (showCalculationExplainer) {
        CalculationExplainerDialog(onDismiss = { showCalculationExplainer = false })
    }
}

@Composable
private fun ReportTargetHeader(
    report: SecurityReport,
    onShareClick: () -> Unit,
    onExportMarkdown: () -> Unit,
    onExportJson: () -> Unit
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
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.12f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onExportMarkdown)
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = ".MD",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(CyberCyan.copy(alpha = 0.12f))
                        .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .clickable(onClick = onExportJson)
                        .padding(horizontal = 7.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = ".JSON",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Share,
                        contentDescription = "Share Report",
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

@Composable
private fun CalculationExplainerDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxSize(0.85f)
                .clip(RoundedCornerShape(18.dp))
                .background(CardBackground)
                .border(1.dp, Border, RoundedCornerShape(18.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "RISK SCORING METHODOLOGY",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "How Scores Are Calculated",
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
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close dialog",
                            tint = SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScrollbar(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "APVM computes an overall threat score from 0 (Clean) to 100 (Critical) using a deterministic multi-dimensional heuristic evaluation formula:",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }

                    item {
                        ScoreDimensionCard(
                            dimension = "1. Dangerous Permission Vector",
                            weight = "35% of total score",
                            description = "Assesses granted sensitive permissions including SMS, Contacts, Location, Camera, Audio Recording, Storage, and special access rights (Accessibility, Overlay, Package Installation)."
                        )
                    }

                    item {
                        ScoreDimensionCard(
                            dimension = "2. Component Attack Surface",
                            weight = "25% of total score",
                            description = "Evaluates exported Activities, Broadcast Receivers, Services, and Content Providers accessible to other applications without requiring system signature permissions."
                        )
                    }

                    item {
                        ScoreDimensionCard(
                            dimension = "3. Hardcoded Secrets & Transport",
                            weight = "20% of total score",
                            description = "Scans DEX bytecode and network security configuration for plaintext HTTP traffic allowances, debuggable build flags, and leaked API credentials."
                        )
                    }

                    item {
                        ScoreDimensionCard(
                            dimension = "4. Risky Capability Combos",
                            weight = "15% of total score",
                            description = "Detects synergistic threat patterns such as Camera + Audio (surveillance exfiltration), Audio + Location, or Package Installer + External Storage (Trojan dropper vector)."
                        )
                    }

                    item {
                        ScoreDimensionCard(
                            dimension = "5. Target SDK Platform Recency",
                            weight = "5% of total score",
                            description = "Penalizes apps targeting legacy Android API levels that bypass modern runtime permission constraints and scoped storage protections."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreDimensionCard(
    dimension: String,
    weight: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dimension,
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = weight,
                    color = CyberCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = description,
            color = SecondaryText,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}