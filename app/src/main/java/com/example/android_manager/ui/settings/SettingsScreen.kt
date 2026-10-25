package com.example.android_manager.ui.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.data.settings.AppSettings
import com.example.android_manager.data.settings.SettingsRepository
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.verticalScrollbar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onTriggerOnboarding: () -> Unit = {}
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val repository = remember { SettingsRepository(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val settings by repository.settingsFlow.collectAsState(initial = AppSettings())

    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showLicensesDialog by remember { mutableStateOf(false) }
    var showCrashLogDialog by remember { mutableStateOf(false) }
    var crashLogText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            APVMTopBar(
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScrollbar(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "CONFIGURATION",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Application Settings",
                    color = PrimaryText,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Manage audit scopes, local storage, and privacy controls.",
                    color = SecondaryText,
                    fontSize = 13.sp
                )
            }

            // Preferences Section
            item {
                SectionHeader("AUDIT & DISPLAY PREFERENCES")
            }

            item {
                SettingsCard {
                    SettingsSwitchRow(
                        title = "AMOLED Deep Black Theme",
                        subtitle = "Enable pure #000000 black background to maximize OLED contrast and battery efficiency",
                        checked = settings.isAmoledMode,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setAmoledMode(checked) }
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Include System Applications",
                        subtitle = "Analyze pre-installed and manufacturer system packages during permission audits",
                        checked = settings.showSystemApps,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setShowSystemApps(checked) }
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Auto-Refresh on Resume",
                        subtitle = "Automatically re-evaluate device state and active processes when reopening APVM",
                        checked = settings.autoRefresh,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setAutoRefresh(checked) }
                        }
                    )

                    SettingsDivider()

                    SettingsSwitchRow(
                        title = "Haptic Interaction Feedback",
                        subtitle = "Provide tactile feedback when selecting security actions or triggering scans",
                        checked = settings.hapticFeedback,
                        onCheckedChange = { checked ->
                            scope.launch { repository.setHapticFeedback(checked) }
                        }
                    )
                }
            }

            // Storage & Maintenance Section
            item {
                SectionHeader("LOCAL STORAGE & MAINTENANCE")
            }

            item {
                SettingsCard {
                    SettingsActionRow(
                        title = "Purge APK Analysis Cache",
                        subtitle = "Remove temporary unpacked APK binaries and decompiled staging files",
                        actionLabel = "PURGE CACHE",
                        actionColor = CyberCyan,
                        onClick = {
                            scope.launch {
                                val bytes = repository.clearApkCache()
                                val mb = String.format(java.util.Locale.US, "%.1f", bytes / (1024.0 * 1024.0))
                                snackbarHostState.showSnackbar("Cache cleaned: $mb MB recovered.")
                            }
                        }
                    )

                    SettingsDivider()

                    SettingsActionRow(
                        title = "Clear Scan History Records",
                        subtitle = "Permanently remove all previous APK security analysis reports from device database",
                        actionLabel = "CLEAR ALL",
                        actionColor = Color(0xFFFF5252),
                        onClick = {
                            showClearHistoryDialog = true
                        }
                    )

                    SettingsDivider()

                    SettingsActionRow(
                        title = "Crash Telemetry Diagnostic Dump",
                        subtitle = "Inspect or clear local diagnostic crash traces recorded on unexpected runtime terminations",
                        actionLabel = "VIEW LOG",
                        actionColor = CyberCyan,
                        onClick = {
                            crashLogText = repository.getCrashLog() ?: "No crash dumps recorded. APVM has not experienced uncaught exceptions."
                            showCrashLogDialog = true
                        }
                    )
                }
            }

            // Architecture & Privacy Guarantee Section
            item {
                SectionHeader("ARCHITECTURAL INTEGRITY & PRIVACY")
            }

            item {
                SettingsCard {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                "100% AIR-GAPPED ON-DEVICE ENGINE",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        ArchitectureFactItem(
                            label = "INTERNET PERMISSION",
                            detail = "Not requested or declared in Manifest. APVM cannot establish remote sockets."
                        )

                        Spacer(Modifier.height(8.dp))

                        ArchitectureFactItem(
                            label = "STATIC ANALYSIS",
                            detail = "Manifest extraction, certificate hashing, and DEX scanning run locally on the JVM."
                        )

                        Spacer(Modifier.height(8.dp))

                        ArchitectureFactItem(
                            label = "VIRUSTOTAL LOOKUP",
                            detail = "Dispatched via client browser URL intent using SHA-256 cert fingerprint. Zero files uploaded."
                        )

                        Spacer(Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface)
                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                                .clickable { onTriggerOnboarding() }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "VIEW SECURITY ONBOARDING TOUR",
                                color = PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Surface)
                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                                .clickable { showLicensesDialog = true }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "VIEW OPEN SOURCE LICENSES",
                                color = PrimaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "APVM v1.0.0 (Build 2026.10)",
                        color = MutedText,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Android Process, Permission & Vulnerability Manager",
                        color = MutedText,
                        fontSize = 10.sp
                    )
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    "Clear All Scan History?",
                    color = PrimaryText,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "This will permanently delete all stored APK vulnerability and security reports. This action cannot be undone.",
                    color = SecondaryText,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearHistoryDialog = false
                        scope.launch {
                            val success = repository.clearScanHistory()
                            if (success) {
                                snackbarHostState.showSnackbar("All scan history records deleted.")
                            } else {
                                snackbarHostState.showSnackbar("Failed to clear scan history.")
                            }
                        }
                    }
                ) {
                    Text("DELETE ALL", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearHistoryDialog = false }) {
                    Text("CANCEL", color = SecondaryText)
                }
            },
            containerColor = CardBackground,
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (showLicensesDialog) {
        OpenSourceLicensesDialog(
            onDismiss = { showLicensesDialog = false }
        )
    }

    if (showCrashLogDialog) {
        CrashLogDialog(
            logText = crashLogText,
            onDismiss = { showCrashLogDialog = false },
            onClear = {
                val cleared = repository.clearCrashLog()
                if (cleared) {
                    crashLogText = "Crash log cleared successfully."
                    scope.launch { snackbarHostState.showSnackbar("Crash log cleared.") }
                }
            },
            onShare = {
                try {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "APVM Crash Diagnostic Dump")
                        putExtra(Intent.EXTRA_TEXT, crashLogText)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(shareIntent, "Share APVM Crash Dump").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(chooser)
                } catch (_: Exception) {}
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = SecondaryText,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, Border, RoundedCornerShape(16.dp))
    ) {
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Border)
    )
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = SecondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Background,
                checkedTrackColor = CyberCyan,
                uncheckedThumbColor = SecondaryText,
                uncheckedTrackColor = Surface
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    actionLabel: String,
    actionColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                color = PrimaryText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                color = SecondaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Box(
            modifier = Modifier
                .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(actionColor.copy(alpha = 0.12f))
                .border(1.dp, actionColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = actionLabel,
                color = actionColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}

@Composable
private fun ArchitectureFactItem(label: String, detail: String) {
    Column {
        Text(
            text = label,
            color = PrimaryText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = detail,
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun OpenSourceLicensesDialog(onDismiss: () -> Unit) {
    val licenses = listOf(
        "Jetpack Compose" to "Android Jetpack modern declarative UI toolkit\nApache License 2.0\nhttps://developer.android.com/jetpack/compose",
        "Room Persistence Library" to "SQLite object-mapping abstraction layer by Google\nApache License 2.0\nhttps://developer.android.com/training/data-storage/room",
        "Material Components 3" to "Material You dynamic and adaptive design system\nApache License 2.0\nhttps://github.com/material-components/material-components-android",
        "Kotlin Coroutines & Flow" to "Asynchronous concurrency and reactive streams for Kotlin\nApache License 2.0\nhttps://github.com/Kotlin/kotlinx.coroutines",
        "DataStore Preferences" to "Transactional, asynchronous persistent key-value storage\nApache License 2.0\nhttps://developer.android.com/topic/libraries/architecture/datastore",
        "Google Gson" to "High-performance JSON serialization and parsing library\nApache License 2.0\nhttps://github.com/google/gson",
        "AndroidX Core Splashscreen" to "Backwards-compatible animated launch screen engine\nApache License 2.0\nhttps://developer.android.com/develop/ui/views/launch/splash-screen"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Open Source Licenses",
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        "APVM is built using trusted open-source software under permissive licensing:",
                        color = SecondaryText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                items(licenses.size) { index ->
                    val (name, detail) = licenses[index]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Surface)
                            .border(1.dp, Border, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(name, color = CyberCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(detail, color = SecondaryText, fontSize = 11.sp, lineHeight = 15.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = CyberCyan, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun CrashLogDialog(
    logText: String,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onShare: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Crash Diagnostic Log",
                color = PrimaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Local diagnostic trace recorded in private app cache. No telemetry is transmitted over the network.",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Surface)
                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            Text(
                                text = logText,
                                color = PrimaryText,
                                fontSize = 10.sp,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onClear) {
                    Text("CLEAR", color = Color(0xFFFF5252), fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onShare) {
                    Text("SHARE", color = CyberCyan, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = onDismiss) {
                    Text("CLOSE", color = SecondaryText)
                }
            }
        },
        containerColor = CardBackground,
        shape = RoundedCornerShape(16.dp)
    )
}
