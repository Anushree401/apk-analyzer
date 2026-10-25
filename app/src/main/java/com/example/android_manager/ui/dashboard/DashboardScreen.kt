
package com.example.android_manager.ui.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.android_manager.data.dashboard.DashboardRepository
import com.example.android_manager.data.dashboard.DeviceSecurityPostureInfo
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.android_manager.model.ScanHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onApps: () -> Unit = {},
    onApkScanner: () -> Unit = {},
    onAuditPermissions: () -> Unit = onApps,
    onDevicePosture: () -> Unit = onApps,
    onNavigateToReport: (Long) -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember { DashboardRepository(context) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current

    var totalApps by remember { mutableStateOf<Int?>(null) }
    var activeApps by remember { mutableStateOf<Int?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    var dangerousApps by remember { mutableStateOf<Int?>(null) }
    var vulnerabilityCount by remember { mutableStateOf<Int?>(null) }
    var recentScans by remember { mutableStateOf<List<ScanHistoryEntry>>(emptyList()) }
    var devicePosture by remember { mutableStateOf<DeviceSecurityPostureInfo?>(null) }
    var topRiskiestApps by remember { mutableStateOf<List<com.example.android_manager.data.permissions.AppPermissionInfo>>(emptyList()) }

    suspend fun loadDashboard(showRefreshIndicator: Boolean = false) {
        if (showRefreshIndicator) isRefreshing = true

        try {
            val fresh = withContext(Dispatchers.IO) {
                repository.refresh()
            }
            totalApps = fresh.totalApps
            activeApps = fresh.totalProcesses
            dangerousApps = fresh.dangerousApps
            vulnerabilityCount = fresh.vulnerabilityCount
            recentScans = fresh.recentScans
            devicePosture = fresh.devicePosture
            topRiskiestApps = fresh.topRiskiestApps
        } finally {
            if (showRefreshIndicator) isRefreshing = false
        }
    }

    LaunchedEffect(Unit) {
        val cached = withContext(Dispatchers.IO) {
            repository.getCachedData()
        }
        totalApps = cached.totalApps
        loadDashboard()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    loadDashboard()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(
            onSettings = onSettings
        )

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    loadDashboard(showRefreshIndicator = true)
                }
            },
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))

                    Text(
                        "A N D R O I D   S E C U R I T Y",
                        color = CyberCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Dashboard",
                        color = PrimaryText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(5.dp))

                    Text(
                        "Your device security at a glance.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(CardBackground)
                            .border(1.dp, Border, RoundedCornerShape(18.dp))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "INSTALLED APPLICATIONS",
                                    color = SecondaryText,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.2.sp
                                )
                                Spacer(Modifier.height(7.dp))
                                Text(
                                    totalApps?.toString() ?: "—",
                                    color = PrimaryText,
                                    fontSize = 38.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Applications discovered",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "APK",
                                    color = CyberCyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .clip(CircleShape)
                                .background(CyberCyan)
                        )
                    }
                }

                item {
                    SectionTitle(
                        "Security indicators",
                        "Metrics from device auditing and APK analysis"
                    )
                }

                item {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        MetricCard(
                            label = "ACTIVE APPS",
                            value = activeApps?.toString() ?: "—",
                            note = "Best-effort visibility",
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            label = "DANGEROUS",
                            value = dangerousApps?.toString() ?: "—",
                            note = "Apps requesting dangerous permissions",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    MetricCard(
                        label = "VULNERABILITIES",
                        value = vulnerabilityCount?.toString() ?: "—",
                        note = "Findings from the latest saved scan per package",
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    DevicePostureCard(
                        posture = devicePosture,
                        onAuditPermissions = onAuditPermissions,
                        onApkScanner = onApkScanner,
                        onDevicePosture = onDevicePosture
                    )
                }

                item {
                    SectionTitle(
                        "Quick access",
                        "Choose a security tool"
                    )
                }

                item {
                    ActionCard(
                        number = "01",
                        category = "DEVICE AUDIT",
                        title = "Apps & Security Modules",
                        description = "Audit permissions, inspect activity and review security risks.",
                        action = "EXPLORE MODULES",
                        onClick = onApps
                    )
                }

                item {
                    ActionCard(
                        number = "02",
                        category = "APK ANALYSIS",
                        title = "Scan an APK",
                        description = "Inspect APK metadata and identify supported security indicators.",
                        action = "OPEN SCANNER",
                        onClick = onApkScanner
                    )
                }

                if (topRiskiestApps.isNotEmpty()) {
                    item {
                        SectionTitle(
                            "Top riskiest applications",
                            "Installed apps requesting multiple high-privilege capabilities"
                        )
                    }

                    items(topRiskiestApps.size) { index ->
                        topRiskiestApps.getOrNull(index)?.let { appInfo ->
                            Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackground)
                                .border(1.dp, Border, RoundedCornerShape(14.dp))
                                .clickable { onAuditPermissions() }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appInfo.app.name,
                                    color = PrimaryText,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "INSPECT ↗",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = appInfo.app.packageName,
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (appInfo.dangerousPermissions.isNotEmpty()) {
                                    Text(
                                        text = "${appInfo.dangerousPermissions.size} Dangerous",
                                        color = Color(0xFFFF9100),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                if (appInfo.specialPermissions.isNotEmpty()) {
                                    Text(
                                        text = "${appInfo.specialPermissions.size} Special",
                                        color = CyberCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                        }
                    }
                }

                item {
                    SectionTitle(
                        "Recent activity",
                        "Latest saved APK security scans"
                    )
                }

                if (recentScans.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackground)
                                .border(1.dp, Border, RoundedCornerShape(14.dp))
                                .padding(18.dp)
                        ) {
                            Text(
                                "No saved scans yet",
                                color = PrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                "Scan an APK and save its results to see security activity here.",
                                color = SecondaryText,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                } else {
                    items(recentScans.size) { index ->
                        recentScans.getOrNull(index)?.let { scan ->
                            Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBackground)
                                .border(1.dp, Border, RoundedCornerShape(14.dp))
                                .clickable { onNavigateToReport(scan.id) }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    scan.riskLevel.name,
                                    color = scan.riskLevel.color(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    SimpleDateFormat(
                                        "dd MMM, HH:mm",
                                        Locale.getDefault()
                                    ).format(Date(scan.timestamp)),
                                    color = SecondaryText,
                                    fontSize = 10.sp
                                )
                            }

                            Spacer(Modifier.height(9.dp))

                            Text(
                                scan.targetName,
                                color = PrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(5.dp))

                            Text(
                                "${scan.findingsCount} findings · ${scan.packageName}",
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                        }
                    }
                }

                item {
                    Text(
                        "Pull down to refresh device information",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = MutedText,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            color = PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            subtitle,
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    note: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .padding(15.dp)
    ) {
        Text(
            label,
            color = SecondaryText,
            fontSize = 10.sp,
            letterSpacing = 0.7.sp
        )
        Spacer(Modifier.height(10.dp))
        Text(
            value,
            color = PrimaryText,
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(
            note,
            color = MutedText,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun ActionCard(
    number: String,
    category: String,
    title: String,
    description: String,
    action: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$number / $category",
                color = CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                "↗",
                color = CyberCyan,
                fontSize = 18.sp
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            title,
            color = PrimaryText,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            description,
            color = SecondaryText,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(15.dp))

        Text(
            action,
            color = CyberCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
private fun DevicePostureCard(
    posture: DeviceSecurityPostureInfo?,
    onAuditPermissions: () -> Unit,
    onApkScanner: () -> Unit,
    onDevicePosture: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "DEVICE SECURITY POSTURE",
                color = CyberCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp
            )
            if (posture != null) {
                val allGood = !posture.isOsEol && !posture.isPatchOutdated && posture.isStorageEncrypted && !posture.isRooted && !posture.isInstallUnknownAppsAllowed
                Text(
                    text = if (allGood) "SYSTEM HEALTHY" else "ATTENTION NEEDED",
                    color = if (allGood) CyberCyan else Color(0xFFFFB300),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        if (posture == null) {
            Text(
                "Evaluating platform integrity & patch level...",
                color = MutedText,
                fontSize = 12.sp
            )
        } else {
            PostureItemRow(
                label = "OS Platform",
                value = "Android ${posture.androidVersion} (API ${posture.apiLevel})",
                badgeText = if (posture.isOsEol) "EOL (NO PATCHES)" else "ACTIVE SUPPORT",
                isWarning = posture.isOsEol
            )

            Spacer(Modifier.height(10.dp))

            val patchDetail = if (posture.patchAgeDays != null) {
                "${posture.securityPatch} (${posture.patchAgeDays}d old)"
            } else {
                posture.securityPatch
            }
            PostureItemRow(
                label = "Security Patch",
                value = patchDetail,
                badgeText = if (posture.isPatchOutdated) "OUTDATED (>90D)" else "UP TO DATE",
                isWarning = posture.isPatchOutdated
            )

            Spacer(Modifier.height(10.dp))

            PostureItemRow(
                label = "Encryption",
                value = posture.encryptionStatusLabel,
                badgeText = if (posture.isStorageEncrypted) "ENCRYPTED" else "UNENCRYPTED",
                isWarning = !posture.isStorageEncrypted
            )

            Spacer(Modifier.height(10.dp))

            val rootLabel = if (posture.isRooted) "ROOTED" else "NOT ROOTED"
            val adbLabel = if (posture.isAdbEnabled) "ADB ON" else "ADB OFF"
            PostureItemRow(
                label = "Integrity & ADB",
                value = "$rootLabel · $adbLabel",
                badgeText = if (posture.isRooted) "COMPROMISED" else if (posture.isAdbEnabled) "DEV MODE" else "SECURE",
                isWarning = posture.isRooted || posture.isAdbEnabled
            )

            Spacer(Modifier.height(10.dp))

            PostureItemRow(
                label = "Unknown Sources",
                value = if (posture.isInstallUnknownAppsAllowed) "Sideloading Allowed" else "Blocked / Restricted",
                badgeText = if (posture.isInstallUnknownAppsAllowed) "WARNING" else "PROTECTED",
                isWarning = posture.isInstallUnknownAppsAllowed
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onAuditPermissions)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "AUDIT APPS",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onApkScanner)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SCAN APK",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .clickable(onClick = onDevicePosture)
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "POSTURE MATRIX",
                        color = PrimaryText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PostureItemRow(
    label: String,
    value: String,
    badgeText: String,
    isWarning: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label.uppercase(),
                color = SecondaryText,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                value,
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isWarning) Color(0x22FFB300) else Color(0x1A00E5FF))
                .border(
                    1.dp,
                    if (isWarning) Color(0xFFFFB300) else CyberCyan.copy(alpha = 0.5f),
                    RoundedCornerShape(6.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                badgeText,
                color = if (isWarning) Color(0xFFFFB300) else CyberCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

