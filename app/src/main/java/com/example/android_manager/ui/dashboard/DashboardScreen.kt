
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
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.android_manager.model.ScanHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Cyan = Color(0xFF00DCC0)
private val CardBg = Color(0xFF101010)
private val InnerBg = Color(0xFF1A1A1A)
private val CardBorder = Color(0xFF292929)
private val Muted = Color(0xFF777777)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onApps: () -> Unit = {},
    onApkScanner: () -> Unit = {}
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
        APVMTopBar()

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
                        color = Cyan,
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
                            .background(CardBg)
                            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
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
                                    .background(InnerBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "APK",
                                    color = Cyan,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(Modifier.height(18.dp))

                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(CircleShape)
                                .background(InnerBg)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(0.72f)
                                    .height(3.dp)
                                    .clip(CircleShape)
                                    .background(Cyan)
                            )
                        }
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
                                .background(CardBg)
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
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
                        val scan = recentScans[index]

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBg)
                                .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    scan.riskLevel.name,
                                    color = Cyan,
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

                item {
                    Text(
                        "Pull down to refresh device information",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        color = Muted,
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
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
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
            color = Muted,
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
            .background(CardBg)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
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
                color = Cyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp
            )
            Text(
                "↗",
                color = Cyan,
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
            color = Cyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
