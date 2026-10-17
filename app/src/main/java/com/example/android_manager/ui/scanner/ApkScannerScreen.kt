package com.example.android_manager.ui.scanner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.report.SecurityReportRepository
import com.example.android_manager.data.risk.RiskAssessmentRepository
import com.example.android_manager.data.scanner.ApkAnalysisResult
import com.example.android_manager.data.scanner.ApkScannerRepository
import com.example.android_manager.model.AppInfo
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ApkScannerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onNavigateToRiskAssessment: (() -> Unit)? = null,
    onNavigateToReport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var apkResult by remember { mutableStateOf<ApkAnalysisResult?>(null) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var installedAssessment by remember { mutableStateOf<RiskAssessment?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var appsLoaded by remember { mutableStateOf(false) }
    var appsSectionExpanded by rememberSaveable { mutableStateOf(false) }
    var isDiscovering by remember { mutableStateOf(false) }
    var isScanningApk by remember { mutableStateOf(false) }
    var isAssessingApp by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var appError by remember { mutableStateOf<String?>(null) }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        selectedUri = uri
        selectedName = uri?.lastPathSegment
        apkResult = null
        errorMessage = null
    }

    fun discoverApps() {
        if (isDiscovering) return
        scope.launch {
            isDiscovering = true
            appError = null
            try {
                installedApps = withContext(Dispatchers.IO) {
                    AppRepository(context).getInstalledApps()
                }
                appsLoaded = true
            } catch (e: Exception) {
                appError = e.message ?: "Could not discover installed apps."
            } finally {
                isDiscovering = false
            }
        }
    }

    fun assessApp(app: AppInfo) {
        if (isAssessingApp) return
        selectedApp = app
        installedAssessment = null
        appError = null
        scope.launch {
            isAssessingApp = true
            try {
                val assessment = withContext(Dispatchers.IO) {
                    RiskAssessmentRepository(context).assessInstalledApp(app.packageName)
                }
                installedAssessment = assessment
                // Cache a matching report for the existing report screen.
                withContext(Dispatchers.IO) {
                    val report = SecurityReportRepository(context)
                        .buildFromInstalledApp(app.packageName)
                    SecurityReportRepository.setActiveReport(report)
                }
            } catch (e: Exception) {
                appError = e.message ?: "Installed-app assessment failed."
            } finally {
                isAssessingApp = false
            }
        }
    }

    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect
        isScanningApk = true
        errorMessage = null
        val startTime = System.currentTimeMillis()
        try {
            val result = withContext(Dispatchers.IO) {
                ApkScannerRepository(context).scanApk(uri)
            }
            apkResult = result
            withContext(Dispatchers.IO) {
                try {
                    val durationMs = System.currentTimeMillis() - startTime
                    val risk = RiskAssessmentRepository(context).assessApkResult(result)
                    val report = SecurityReportRepository(context).buildFromApkAnalysis(result, risk)
                    SecurityReportRepository.setActiveReport(report)
                    com.example.android_manager.data.history.ScanHistoryRepository(context)
                        .recordApkScan(result = result, risk = risk, durationMs = durationMs)
                } catch (_: Exception) {
                    // A history/report persistence failure must not hide a successful APK analysis.
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "APK analysis failed."
        } finally {
            isScanningApk = false
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        installedApps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier.fillMaxSize().background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("APK SCANNER", color = PrimaryText, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(5.dp))
                Text(
                    "Discover installed applications or scan an APK file.",
                    color = SecondaryText,
                    fontSize = 13.sp
                )
            }

            item {
                SectionPanel(title = "") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { appsSectionExpanded = !appsSectionExpanded },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "INSTALLED APPLICATIONS",
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.7.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when {
                                    isDiscovering -> "Discovering applications..."
                                    appsLoaded -> "${installedApps.size} applications discovered"
                                    else -> "Discover and assess apps on this device"
                                },
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            if (appsSectionExpanded) "⌃" else "⌄",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (appsSectionExpanded) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Review requested permissions, component exposure, and configuration.",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { discoverApps() },
                            enabled = !isDiscovering,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isDiscovering) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.size(8.dp))
                                Text("DISCOVERING APPS...")
                            } else {
                                Text(if (appsLoaded) "REFRESH INSTALLED APPS" else "DISCOVER INSTALLED APPS")
                            }
                        }
                        if (isDiscovering) {
                            Spacer(Modifier.height(8.dp))
                            Text("Reading app metadata from Android PackageManager.", color = SecondaryText, fontSize = 12.sp)
                        }
                        appError?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = PrimaryText, fontSize = 12.sp)
                        }
                        if (appsLoaded) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("Search by app or package name") }
                            )
                        }
                    }
                }
            }

            if (appsLoaded && appsSectionExpanded) {
                if (filteredApps.isEmpty()) {
                    item {
                        Text("No applications match this search.", color = SecondaryText, fontSize = 13.sp)
                    }
                } else {
                    items(filteredApps, key = { it.packageName }) { app ->
                        InstalledAppRow(
                            app = app,
                            selected = selectedApp?.packageName == app.packageName,
                            onClick = { assessApp(app) }
                        )
                    }
                }
            }

            if (selectedApp != null) {
                item {
                    SectionPanel(title = "INSTALLED-APP ASSESSMENT") {
                        Text(selectedApp!!.name, color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Text(selectedApp!!.packageName, color = SecondaryText, fontSize = 11.sp)
                        Spacer(Modifier.height(8.dp))
                        if (isAssessingApp) {
                            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.height(6.dp))
                            Text("Assessing app metadata and security configuration...", color = SecondaryText, fontSize = 12.sp)
                        } else {
                            installedAssessment?.let { risk ->
                                Text("${risk.overallScore}/100  ·  ${risk.overallLevel}", color = PrimaryText, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text("Permission risk: ${risk.permissionRisk}/100", color = SecondaryText, fontSize = 12.sp)
                                Text("Component exposure: ${risk.componentExposureRisk}/100", color = SecondaryText, fontSize = 12.sp)
                                Text("Configuration risk: ${risk.configurationRisk}/100", color = SecondaryText, fontSize = 12.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "This is an installed-app metadata assessment. It does not include static DEX vulnerability analysis of the installed APK.",
                                    color = SecondaryText,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                                if (onNavigateToReport != null) {
                                    Spacer(Modifier.height(10.dp))
                                    Button(onClick = onNavigateToReport, modifier = Modifier.fillMaxWidth()) {
                                        Text("VIEW SECURITY REPORT")
                                    }
                                }
                                if (onNavigateToRiskAssessment != null) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedButton(onClick = onNavigateToRiskAssessment, modifier = Modifier.fillMaxWidth()) {
                                        Text("VIEW RISK ASSESSMENT")
                                    }
                                }
                                if (risk.contributingFactors.isNotEmpty()) {
                                    Spacer(Modifier.height(12.dp))
                                    Text("ASSESSMENT FACTORS", color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    risk.contributingFactors.take(8).forEach { factor ->
                                        Spacer(Modifier.height(7.dp))
                                        Text(factor.title, color = PrimaryText, fontSize = 12.sp)
                                        factor.evidence?.let { evidence ->
                                            Text(evidence, color = SecondaryText, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionPanel(title = "SCAN AN APK FILE") {
                    Text(
                        "Use this option for static analysis of an APK file you can access.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { apkPicker.launch("application/vnd.android.package-archive") },
                        enabled = !isScanningApk,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isScanningApk) "ANALYZING APK..." else "SELECT APK FILE")
                    }
                    selectedName?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("Selected: $it", color = SecondaryText, fontSize = 12.sp)
                    }
                    if (isScanningApk) {
                        Spacer(Modifier.height(8.dp))
                        CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                        Text("Reading metadata, manifest, components and DEX files.", color = SecondaryText, fontSize = 12.sp)
                    }
                    errorMessage?.let {
                        Spacer(Modifier.height(8.dp))
                        Text("SCAN FAILED", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Text(it, color = SecondaryText, fontSize = 12.sp)
                    }
                    apkResult?.let { result ->
                        Spacer(Modifier.height(12.dp))
                        Text("APK INFORMATION", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                        Text("App: ${result.apkInfo.appName ?: "Unknown"}", color = SecondaryText, fontSize = 12.sp)
                        Text("Package: ${result.apkInfo.packageName}", color = SecondaryText, fontSize = 12.sp)
                        Text("Version: ${result.apkInfo.versionName ?: "Unknown"}", color = SecondaryText, fontSize = 12.sp)
                        Text("Target SDK: ${result.apkInfo.targetSdk ?: "Unknown"}", color = SecondaryText, fontSize = 12.sp)
                        Text("Findings: ${result.findings.size}", color = SecondaryText, fontSize = 12.sp)
                        if (onNavigateToReport != null) {
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = onNavigateToReport, modifier = Modifier.fillMaxWidth()) {
                                Text("VIEW COMPLETE SECURITY REPORT")
                            }
                        }
                        if (onNavigateToRiskAssessment != null) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onNavigateToRiskAssessment, modifier = Modifier.fillMaxWidth()) {
                                Text("VIEW RISK ASSESSMENT")
                            }
                        }
                        if (result.findings.isEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text("No configured static security indicators were detected.", color = SecondaryText, fontSize = 12.sp)
                        } else {
                            Spacer(Modifier.height(10.dp))
                            Text("SECURITY FINDINGS", color = PrimaryText, fontWeight = FontWeight.SemiBold)
                            result.findings.forEach { finding ->
                                Spacer(Modifier.height(8.dp))
                                ApkFindingCard(finding = finding)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionPanel(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .background(Graphite.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        if (title.isNotBlank()) {
            Text(title, color = PrimaryText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.7.sp)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

@Composable
private fun InstalledAppRow(
    app: AppInfo,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (selected) androidx.compose.material3.MaterialTheme.colorScheme.primary else Border,
                shape = RoundedCornerShape(12.dp)
            )
            .background(Graphite.copy(alpha = if (selected) 0.35f else 0.18f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(app.name, color = PrimaryText, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(3.dp))
                Text(app.packageName, color = SecondaryText, fontSize = 10.sp)
                Text(
                    "Version ${app.versionName ?: "unknown"} · ${if (app.isSystemApp) "System app" else "User app"}",
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
            Text(if (selected) "SELECTED" else "ASSESS", color = androidx.compose.material3.MaterialTheme.colorScheme.primary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
