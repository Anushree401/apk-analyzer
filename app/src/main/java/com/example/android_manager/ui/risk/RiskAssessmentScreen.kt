package com.example.android_manager.ui.risk

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.model.ScanStatus
import com.example.android_manager.model.ScanType
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.example.android_manager.data.risk.RiskAssessmentRepository
import com.example.android_manager.data.scanner.ApkScannerRepository
import com.example.android_manager.model.AppInfo
import com.example.android_manager.model.DeviceRiskPosture
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskCritical
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RiskScreenTab(val title: String) {
    TARGET_AUDIT("TARGET AUDIT"),
    DEVICE_POSTURE("DEVICE POSTURE"),
    RISK_MATRIX("RISK MATRIX")
}

/**
 * Main Risk Assessment Screen displaying holistic cybersecurity telemetry,
 * multi-vector scoring, component exposure, and mitigation recommendations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskAssessmentScreen(
    initialPackageName: String? = null,
    onBack: () -> Unit = {},
    onNavigateToReport: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val riskRepo = remember { RiskAssessmentRepository(context) }
    val appRepo = remember { AppRepository(context) }
    val apkScannerRepo = remember { ApkScannerRepository(context) }
    val historyRepo = remember { ScanHistoryRepository(context) }

    var selectedTab by remember { mutableStateOf(RiskScreenTab.TARGET_AUDIT) }
    var installedApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
    var currentAssessment by remember { mutableStateOf<RiskAssessment?>(null) }
    var devicePosture by remember { mutableStateOf<DeviceRiskPosture?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAppPicker by remember { mutableStateOf(false) }

    suspend fun saveAssessmentToHistory(
        assessment: RiskAssessment,
        scanType: ScanType,
        durationMs: Long = 0L
    ) {
        val findings = assessment.contributingFactors.map { factor ->
            com.example.android_manager.model.VulnerabilityFinding(
                name = factor.title,
                severity = factor.severity,
                affectedComponent = factor.category.displayName,
                evidence = factor.evidence ?: factor.description,
                impact = factor.description,
                recommendation = assessment.recommendations
                    .find {
                        it.title.contains(factor.title, ignoreCase = true)
                    }
                    ?.action
                    ?: "Review application privileges and minimize attack surface."
            )
        }

        val entry = ScanHistoryEntry(
            scanType = scanType,
            targetName = assessment.targetName,
            packageName = assessment.packageName,
            versionName = assessment.versionName ?: "",
            versionCode = 0L,
            minSdk = 0,
            targetSdk = 0,
            riskScore = assessment.overallScore,
            riskLevel = assessment.overallLevel,
            status = when {
                assessment.overallLevel == com.example.android_manager.model.RiskLevel.CRITICAL ||
                        assessment.overallLevel == com.example.android_manager.model.RiskLevel.HIGH ->
                    ScanStatus.THREAT_DETECTED

                assessment.overallLevel == com.example.android_manager.model.RiskLevel.MEDIUM ->
                    ScanStatus.SUSPICIOUS

                else -> ScanStatus.CLEAN
            },
            findingsCount = findings.size,
            criticalCount = findings.count {
                it.severity == com.example.android_manager.model.RiskLevel.CRITICAL
            },
            highCount = findings.count {
                it.severity == com.example.android_manager.model.RiskLevel.HIGH
            },
            mediumCount = findings.count {
                it.severity == com.example.android_manager.model.RiskLevel.MEDIUM
            },
            lowCount = findings.count {
                it.severity == com.example.android_manager.model.RiskLevel.LOW
            },
            findings = findings,
            timestamp = assessment.timestamp,
            scanDurationMs = durationMs
        )

        withContext(Dispatchers.IO) {
            historyRepo.saveScan(entry)
        }
    }


    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isLoading = true
                errorMessage = null
                try {
                    val scanResult = withContext(Dispatchers.IO) {
                        apkScannerRepo.scanApk(uri)
                    }
                    val assessment = withContext(Dispatchers.IO) {
                        riskRepo.assessApkResult(scanResult)
                    }

                    currentAssessment = assessment

                    saveAssessmentToHistory(
                        assessment = assessment,
                        scanType = ScanType.APK_FILE
                    )
                    selectedTab = RiskScreenTab.TARGET_AUDIT
                } catch (e: Exception) {
                    errorMessage = e.message ?: "Failed to evaluate selected APK file."
                } finally {
                    isLoading = false
                }
            }
        }
    }


    suspend fun loadTargetAssessment(targetPkg: String) {
        isLoading = true
        errorMessage = null
        try {
            val assessment = withContext(Dispatchers.IO) {
                riskRepo.assessInstalledApp(targetPkg)
            }

            currentAssessment = assessment

            saveAssessmentToHistory(
                assessment = assessment,
                scanType = ScanType.INSTALLED_APP
            )
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to assess risk for $targetPkg"
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    suspend fun loadDevicePosture(apps: List<AppInfo>) {
        try {
            val posture = withContext(Dispatchers.IO) {
                val nonSystem = apps.filter { !it.isSystemApp }.map { it.packageName }
                val targetList = if (nonSystem.isNotEmpty()) nonSystem else apps.map { it.packageName }
                riskRepo.assessDevicePosture(targetList.take(25))
            }
            devicePosture = posture
        } catch (e: Exception) {
            // Non-critical background failure
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        val apps = withContext(Dispatchers.IO) {
            appRepo.getInstalledApps()
        }
        installedApps = apps

        val initialTarget = initialPackageName
            ?: apps.firstOrNull { !it.isSystemApp }?.packageName

        if (initialTarget != null) {
            selectedApp = apps.find { it.packageName == initialTarget }
            loadTargetAssessment(initialTarget)
        } else {
            isLoading = false
        }

        loadDevicePosture(apps)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        // Screen Subheader & Module Title
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "SECURITY RISK ASSESSMENT",
                color = PrimaryText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Text(
                text = "Multi-vector attack surface & privilege evaluation",
                color = SecondaryText,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Tab bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                    .background(Surface, RoundedCornerShape(10.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RiskScreenTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (isSelected) CardBackground else Background.copy(alpha = 0f),
                                RoundedCornerShape(8.dp)
                            )
                            .border(
                                1.dp,
                                if (isSelected) Border else Background.copy(alpha = 0f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = tab }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) CyberCyan else SecondaryText,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.6.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    currentAssessment?.let {
                        loadTargetAssessment(it.packageName)
                    } ?: run { isRefreshing = false }
                }
            },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading && currentAssessment == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            color = CyberCyan,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "CALCULATING RISK TELEMETRY...",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(2.dp))
                    }

                    errorMessage?.let { error ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, RiskCritical, RoundedCornerShape(12.dp))
                                    .background(Surface, RoundedCornerShape(12.dp))
                                    .padding(14.dp)
                            ) {
                                Text(
                                    text = "ASSESSMENT ERROR",
                                    color = RiskCritical,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = error,
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    when (selectedTab) {
                        RiskScreenTab.TARGET_AUDIT -> {
                            currentAssessment?.let { assessment ->
                                item {
                                    RiskTargetBanner(
                                        targetName = assessment.targetName,
                                        packageName = assessment.packageName,
                                        versionName = assessment.versionName,
                                        isSystemApp = assessment.isSystemApp,
                                        onSelectAppClick = { showAppPicker = true },
                                        onSelectApkClick = {
                                            apkPicker.launch("application/vnd.android.package-archive")
                                        }
                                    )
                                }

                                item {
                                    RiskScoreCard(
                                        score = assessment.overallScore,
                                        riskLevel = assessment.overallLevel,
                                        label = "OVERALL RISK SCORE",
                                        targetName = assessment.targetName
                                    )
                                }

                                if (onNavigateToReport != null) {
                                    item {
                                        Button(
                                            onClick = { onNavigateToReport(assessment.packageName) },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("VIEW FULL SECURITY REPORT")
                                        }
                                    }
                                }

                                item {
                                    RiskSubscoreCard(assessment = assessment)
                                }

                                if (assessment.detectedCombinations.isNotEmpty()) {
                                    item {
                                        RiskCombinationsCard(combinations = assessment.detectedCombinations)
                                    }
                                }

                                item {
                                    ComponentExposureDetailCard(breakdown = assessment.componentBreakdown)
                                }

                                if (assessment.contributingFactors.isNotEmpty()) {
                                    item {
                                        ContributingFactorsCard(factors = assessment.contributingFactors)
                                    }
                                }

                                if (assessment.recommendations.isNotEmpty()) {
                                    item {
                                        RiskRecommendationsCard(recommendations = assessment.recommendations)
                                    }
                                }
                            }
                        }

                        RiskScreenTab.DEVICE_POSTURE -> {
                            item {
                                DevicePostureTabContent(
                                    posture = devicePosture,
                                    onSelectApp = { appPkg ->
                                        scope.launch {
                                            selectedTab = RiskScreenTab.TARGET_AUDIT
                                            loadTargetAssessment(appPkg)
                                        }
                                    }
                                )
                            }
                        }

                        RiskScreenTab.RISK_MATRIX -> {
                            item {
                                RiskMatrixTabContent()
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        if (showAppPicker) {
            AppSelectionBottomSheet(
                apps = installedApps,
                onAppSelected = { app ->
                    selectedApp = app
                    scope.launch {
                        loadTargetAssessment(app.packageName)
                    }
                },
                onDismiss = { showAppPicker = false }
            )
        }
    }
}