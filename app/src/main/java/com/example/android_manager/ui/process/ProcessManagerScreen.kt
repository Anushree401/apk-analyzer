package com.example.android_manager.ui.process

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.android_manager.data.process.ProcessRepository
import com.example.android_manager.model.ProcessExitInfo
import com.example.android_manager.model.ProcessInfo
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ProcessManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ProcessRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var currentProcess by remember { mutableStateOf<ProcessInfo?>(null) }
    var exitHistory by remember { mutableStateOf<List<ProcessExitInfo>>(emptyList()) }
    var backgroundProcesses by remember { mutableStateOf<List<ProcessInfo>>(emptyList()) }
    var recentlyActiveApps by remember { mutableStateOf<List<ProcessInfo>>(emptyList()) }
    var usageAccess by remember { mutableStateOf(repository.hasUsageAccess()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var lastUpdated by remember { mutableStateOf<Long?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }

    fun refreshData() {
        if (isRefreshing) return

        scope.launch {
            isRefreshing = true
            loadError = null
            usageAccess = repository.hasUsageAccess()

            try {
                val result = withContext(Dispatchers.IO) {
                    Triple(
                        repository.getCurrentForegroundApp(),
                        repository.getBackgroundProcesses(),
                        repository.getRecentlyActiveApps()
                    )
                }

                currentProcess = result.first
                backgroundProcesses = result.second
                recentlyActiveApps = result.third
                exitHistory = withContext(Dispatchers.IO) {
                    repository.getProcessExitHistory()
                }
                lastUpdated = System.currentTimeMillis()
            } catch (exception: Exception) {
                if (exception is CancellationException) throw exception
                loadError = "Couldn't refresh process information. Try again."
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccess = repository.hasUsageAccess()
                refreshData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        refreshData()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Process Manager",
                            color = PrimaryText,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = "Application activity and process visibility",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }

                    IconButton(
                        onClick = { refreshData() },
                        enabled = !isRefreshing
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Refresh process information",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                lastUpdated?.let { timestamp ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "LAST UPDATED  ${formatTimestamp(timestamp, "HH:mm:ss")}",
                        color = SecondaryText,
                        fontSize = 10.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            if (!usageAccess) {
                item {
                    SectionCard {
                        Text(
                            text = "USAGE ACCESS REQUIRED",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.7.sp
                        )
                        Spacer(Modifier.height(7.dp))
                        Text(
                            text = "Allow APVM Usage Access to inspect recent application usage and estimate the current foreground app. Android may still restrict visibility into other apps' live processes.",
                            color = SecondaryText,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Graphite
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text("OPEN USAGE ACCESS SETTINGS")
                        }
                    }
                }
            }

            loadError?.let { message ->
                item {
                    SectionCard {
                        Text(
                            text = "REFRESH FAILED",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = message,
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { refreshData() }) {
                            Text("TRY AGAIN")
                        }
                    }
                }
            }

            item {
                SectionHeading(
                    title = "CURRENT ACTIVITY",
                    subtitle = "Foreground app detected from available usage data"
                )
                SectionCard {
                    when {
                        isLoading -> LoadingRow("Checking current activity?")
                        currentProcess != null -> {
                            val process = currentProcess!!
                            Text(
                                text = process.processName,
                                color = PrimaryText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = process.packageName,
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                            if (process.pid > 0) {
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = "PID ${process.pid}",
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    openAppSettings(context, process.packageName)
                                }
                            ) {
                                Text("APP SETTINGS")
                            }
                        }
                        !usageAccess -> EmptyState(
                            title = "Usage access is off",
                            description = "Enable Usage Access above, then return here and refresh."
                        )
                        else -> EmptyState(
                            title = "No foreground app identified",
                            description = "Android did not provide a usable foreground-app result at this time."
                        )
                    }
                }
            }

            item {
                SectionHeading(
                    title = "BACKGROUND / RUNNING",
                    subtitle = "Live process information exposed to APVM"
                )
            }

            if (isLoading) {
                item { SectionCard { LoadingRow("Loading process information?") } }
            } else if (backgroundProcesses.isEmpty()) {
                item {
                    SectionCard {
                        EmptyState(
                            title = "No visible background processes",
                            description = "Android may limit live-process visibility for third-party apps. This does not prove that no apps or services are running."
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Try refreshing. If this remains empty, it may be an Android visibility limitation rather than a detection failure.",
                            color = SecondaryText,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            } else {
                items(
                    items = backgroundProcesses,
                    key = { "${it.packageName}_${it.pid}" }
                ) { process ->
                    BackgroundProcessCard(
                        process = process,
                        onOpenSettings = {
                            openAppSettings(context, process.packageName)
                        }
                    )
                }
            }

            item {
                SectionHeading(
                    title = "RECENTLY ACTIVE",
                    subtitle = "Based on recorded app-usage events, not live process status"
                )
            }

            if (isLoading) {
                item { SectionCard { LoadingRow("Loading recent activity?") } }
            } else if (recentlyActiveApps.isEmpty()) {
                item {
                    SectionCard {
                        EmptyState(
                            title = "No recent activity available",
                            description = if (usageAccess) {
                                "No qualifying usage events were returned. Open a few apps and refresh, or check Usage Access."
                            } else {
                                "Enable Usage Access to let APVM read recent application usage."
                            }
                        )
                    }
                }
            } else {
                items(
                    items = recentlyActiveApps,
                    key = { "${it.packageName}_${it.lastActiveTime}" }
                ) { app ->
                    RecentlyActiveCard(
                        process = app,
                        onOpenSettings = {
                            openAppSettings(context, app.packageName)
                        }
                    )
                }
            }

            item {
                SectionHeading(
                    title = "RECENT PROCESS EVENTS",
                    subtitle = "Exit events reported by Android, when available"
                )
            }

            if (isLoading) {
                item { SectionCard { LoadingRow("Loading process events?") } }
            } else if (exitHistory.isEmpty()) {
                item {
                    SectionCard {
                        EmptyState(
                            title = "No process exit events available",
                            description = "Android may not expose exit history for every application. Events appear only when the system reports them and the device/API supports access."
                        )
                    }
                }
            } else {
                items(
                    items = exitHistory,
                    key = { "${it.packageName}_${it.timestamp}" }
                ) { event ->
                    ProcessEventCard(event = event)
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionHeading(
    title: String,
    subtitle: String? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = SecondaryText,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun SectionCard(
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Graphite.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        content()
    }
}

@Composable
private fun LoadingRow(message: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = message,
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            color = SecondaryText,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun BackgroundProcessCard(
    process: ProcessInfo,
    onOpenSettings: () -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = process.processName,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = process.packageName,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "BACKGROUND",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        if (process.pid > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "PID ${process.pid}",
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSettings) {
            Text("APP SETTINGS")
        }
    }
}

@Composable
private fun RecentlyActiveCard(
    process: ProcessInfo,
    onOpenSettings: () -> Unit
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = process.processName,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = process.packageName,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
            Text(
                text = "RECENT",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(Modifier.height(7.dp))
        Text(
            text = "Last active ${formatTimestamp(process.lastActiveTime, "HH:mm:ss")}",
            color = SecondaryText,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onOpenSettings) {
            Text("APP SETTINGS")
        }
    }
}

@Composable
private fun ProcessEventCard(
    event: ProcessExitInfo
) {
    SectionCard {
        Text(
            text = event.reason,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = event.processName,
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = event.packageName,
            color = SecondaryText,
            fontSize = 11.sp
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = formatTimestamp(event.timestamp, "dd MMM, HH:mm"),
            color = SecondaryText,
            fontSize = 11.sp
        )
        event.description?.takeIf { it.isNotBlank() }?.let { description ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = description,
                color = SecondaryText,
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
        }
    }
}

private fun formatTimestamp(
    timestamp: Long,
    pattern: String
): String {
    if (timestamp <= 0L) return "Unavailable"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(timestamp))
}

private fun openAppSettings(
    context: Context,
    packageName: String
) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        android.net.Uri.fromParts("package", packageName, null)
    )
    context.startActivity(intent)
}
