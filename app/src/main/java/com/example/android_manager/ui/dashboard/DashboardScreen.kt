package com.example.android_manager.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.android_manager.data.dashboard.DashboardRepository
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.FindingCard
import com.example.android_manager.ui.components.ModuleCard
import com.example.android_manager.ui.components.StatCard
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onPermissionAuditor: () -> Unit = {},
    onProcessManager: () -> Unit = {},
    onApkScanner: () -> Unit = {}
) {

    val context = LocalContext.current

    val repository =
        remember {
            DashboardRepository(context)
        }

    val scope =
        rememberCoroutineScope()

    var totalApps by remember {
        mutableStateOf<Int?>(null)
    }

    var totalProcesses by remember {
        mutableStateOf<Int?>(null)
    }

    var isRefreshing by remember {
        mutableStateOf(false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {

        val cached =
            withContext(Dispatchers.IO) {
                repository.getCachedData()
            }

        totalApps =
            cached.totalApps

        isRefreshing = true

        try {

            val fresh =
                withContext(Dispatchers.IO) {
                    repository.refresh()
                }

            totalApps =
                fresh.totalApps

            totalProcesses =
                fresh.totalProcesses

        } finally {

            isRefreshing = false
        }
    }

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                scope.launch {

                    isRefreshing = true

                    try {
                        val fresh = withContext(Dispatchers.IO) {
                            repository.refresh()
                        }

                        totalApps = fresh.totalApps
                        totalProcesses = fresh.totalProcesses

                    } finally {
                        isRefreshing = false
                    }
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    suspend fun refreshDashboard() {

        isRefreshing = true

        try {

            val fresh =
                withContext(Dispatchers.IO) {
                    repository.refresh()
                }

            totalApps =
                fresh.totalApps

            totalProcesses =
                fresh.totalProcesses

        } finally {

            isRefreshing = false
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
                    refreshDashboard()
                }
            },

            modifier =
                Modifier.weight(1f)
        ) {

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 20.dp
                    ),

                verticalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {

                item {

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text = "Security Dashboard",
                        color = PrimaryText
                    )

                    Text(
                        text =
                            "Android security overview",
                        color = SecondaryText
                    )

                    Spacer(
                        modifier =
                            Modifier.height(10.dp)
                    )
                }

                item {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        StatCard(
                            value =
                                totalApps
                                    ?.toString()
                                    ?: "—",

                            label = "Apps",

                            modifier =
                                Modifier.weight(1f)
                        )

                        StatCard(
                            value =
                                totalProcesses
                                    ?.toString()
                                    ?: "—",

                            label = "Active Apps",

                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }

                item {

                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        StatCard(
                            value = "12",
                            label = "Dangerous",
                            modifier =
                                Modifier.weight(1f)
                        )

                        StatCard(
                            value = "07",
                            label = "Vulnerabilities",
                            modifier =
                                Modifier.weight(1f)
                        )
                    }
                }

                item {

                    Text(
                        text = "Security Modules",
                        color = PrimaryText
                    )
                }


                item {

                    ModuleCard(
                        title =
                            "Permission Auditor",

                        description =
                            "Audit installed application permissions",

                        onClick =
                            onPermissionAuditor
                    )
                }


                item {

                    ModuleCard(
                        title =
                            "Process Manager",

                        description =
                            "Monitor recently active applications and process events",

                        onClick =
                            onProcessManager
                    )
                }


                item {

                    ModuleCard(
                        title =
                            "APK Security Scanner",

                        description =
                            "Analyze APK metadata and detect security vulnerabilities",

                        onClick =
                            onApkScanner
                    )
                }

                item {

                    Text(
                        text = "Recent Findings",
                        color = PrimaryText
                    )
                }


                item {

                    FindingCard(
                        severity = "MEDIUM",
                        title =
                            "Security monitoring active",
                        packageName = "APVM"
                    )
                }


                item {

                    Spacer(
                        modifier =
                            Modifier.height(20.dp)
                    )
                }
            }
        }

        APVMBottomBar()
    }
}