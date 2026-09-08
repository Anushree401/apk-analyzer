package com.example.android_manager.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.FindingCard
import com.example.android_manager.ui.components.ModuleCard
import com.example.android_manager.ui.components.StatCard
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.background
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.data.process.ProcessRepository
import com.example.android_manager.model.ProcessInfo

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    onNavigateToPermissions: () -> Unit = {},
    onNavigateToProcessManager: () -> Unit = {}
) {
    val context = LocalContext.current

    val installedApps = produceState<Int?>(
        initialValue = null,
        key1 = context
    ) {
        value = withContext(Dispatchers.IO) {
            AppRepository(context).getInstalledApps().size
        }
    }

    val activeProcesses = produceState<List<ProcessInfo>?>(
        initialValue = null,
        key1 = context
    ) {
        value = withContext(Dispatchers.IO) {
            ProcessRepository(context).getRecentlyActiveApps()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {

        APVMTopBar()

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "DEVICE SECURITY STATUS",
                    color = SecondaryText
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "MEDIUM RISK",
                    color = PrimaryText
                )

                Text(
                    text = "68 / 100",
                    color = PrimaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        value = installedApps.value?.toString() ?: "!",
                        label = "APPS",
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        value = activeProcesses.value?.size?.toString() ?: "!",
                        label = "PROCESSES",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        value = "12",
                        label = "DANGEROUS",
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        value = "07",
                        label = "VULNERABILITIES",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "QUICK MODULES",
                    color = SecondaryText
                )
            }

            item {
                ModuleCard(
                    title = "Process Manager",
                    onClick = onNavigateToProcessManager
                )
            }

            item {
                ModuleCard(
                    title = "Permission Auditor",
                    onClick = onNavigateToPermissions
                )
            }

            item {
                ModuleCard(
                    title = "APK Scanner",
                    onClick = {}
                )
            }

            item {
                ModuleCard(
                    title = "Risk Assessment",
                    onClick = {}
                )
            }

            item {
                ModuleCard(
                    title = "Reports",
                    onClick = {}
                )
            }

            item {
                ModuleCard(
                    title = "History",
                    onClick = {}
                )
            }

            item {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "RECENT FINDINGS",
                    color = SecondaryText
                )
            }

            items(
                listOf(
                    Triple(
                        "HIGH",
                        "Insecure WebView",
                        "com.unknown.malware.app"
                    ),
                    Triple(
                        "MEDIUM",
                        "Sensitive Permission: RECORD_AUDIO",
                        "com.social.app"
                    )
                )
            ) { finding ->

                FindingCard(
                    severity = finding.first,
                    title = finding.second,
                    packageName = finding.third
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        APVMBottomBar()
    }
}