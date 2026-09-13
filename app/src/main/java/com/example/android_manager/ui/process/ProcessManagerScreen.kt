package com.example.android_manager.ui.process

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.net.toUri

@Composable
fun ProcessManagerScreen(
    onBack: () -> Unit
) {

    val context = androidx.compose.ui.platform.LocalContext.current
    val repository = remember {
        ProcessRepository(context)
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    var currentProcess by remember {
        mutableStateOf<ProcessInfo?>(null)
    }

    var exitHistory by remember {
        mutableStateOf<List<ProcessExitInfo>>(emptyList())
    }

    var usageAccess by remember {
        mutableStateOf(false)
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var backgroundProcesses by remember {
        mutableStateOf<List<ProcessInfo>>(emptyList())
    }

    var recentlyActiveApps by remember {
        mutableStateOf<List<ProcessInfo>>(emptyList())
    }

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (event == Lifecycle.Event.ON_RESUME) {

                    usageAccess =
                        repository.hasUsageAccess()

                    if (usageAccess) {

                        currentProcess =
                            repository.getCurrentForegroundApp()
                    }
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(usageAccess) {

        if (!usageAccess) {
            isLoading = false
            return@LaunchedEffect
        }

        while (true) {

            val overview =
                withContext(Dispatchers.IO) {

                    Triple(
                        repository.getCurrentForegroundApp(),
                        repository.getBackgroundProcesses(),
                        repository.getRecentlyActiveApps()
                    )
                }

            currentProcess = overview.first
            backgroundProcesses = overview.second
            recentlyActiveApps = overview.third

            isLoading = false

            delay(1000)
        }
    }

    LaunchedEffect(Unit) {

        while (true) {

            exitHistory =
                withContext(Dispatchers.IO) {
                    repository.getProcessExitHistory()
                }

            delay(5000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {

        APVMTopBar(
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            item {

                Spacer(
                    Modifier.height(8.dp)
                )

                Text(
                    text = "Process Manager",
                    color = PrimaryText,
                    fontSize = 22.sp
                )

                Text(
                    text = "Monitor application activity and process events",
                    color = SecondaryText,
                    fontSize = 13.sp
                )

                Spacer(
                    Modifier.height(10.dp)
                )
            }

            if (!usageAccess) {

                item {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                Border,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(18.dp)
                    ) {

                        Text(
                            text = "USAGE ACCESS REQUIRED",
                            color = PrimaryText,
                            fontSize = 14.sp
                        )

                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            text =
                                "APVM needs Usage Access to detect which application is currently in the foreground.",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )

                        Spacer(
                            Modifier.height(14.dp)
                        )

                        Button(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_USAGE_ACCESS_SETTINGS
                                    )
                                )
                            },
                            colors =
                                ButtonDefaults.buttonColors(
                                    containerColor = Graphite
                                )
                        ) {

                            Text(
                                text = "OPEN SETTINGS",
                                color = PrimaryText
                            )
                        }
                    }
                }
            }

            item {

                Text(
                    text = "CURRENT ACTIVITY",
                    color = SecondaryText,
                    fontSize = 12.sp
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            Border,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(18.dp)
                ) {

                    if (isLoading) {

                        Row(
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Graphite
                            )

                            Spacer(
                                Modifier.size(12.dp)
                            )

                            Text(
                                text = "DETECTING...",
                                color = SecondaryText
                            )
                        }

                    } else {

                        val process = currentProcess

                        if (process != null) {

                            Text(
                                text = process.processName,
                                color = PrimaryText,
                                fontSize = 17.sp
                            )

                            Spacer(
                                Modifier.height(5.dp)
                            )

                            Text(
                                text =
                                    "Foreground application detected",
                                color = SecondaryText,
                                fontSize = 12.sp
                            )

                            Spacer(
                                Modifier.height(14.dp)
                            )

                            Row(
                                horizontalArrangement =
                                    Arrangement.spacedBy(10.dp)
                            ) {

                                Button(
                                    onClick = {
                                        openAppSettings(
                                            context,
                                            process.processName
                                        )
                                    },
                                    colors =
                                        ButtonDefaults.buttonColors(
                                            containerColor = Graphite
                                        )
                                ) {

                                    Text(
                                        text = "APP SETTINGS",
                                        color = PrimaryText
                                    )
                                }
                            }

                        } else {

                            Text(
                                text = "NO EXTERNAL APP DETECTED",
                                color = SecondaryText,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            item {

                Text(
                    text = "BACKGROUND / RUNNING",
                    color = PrimaryText,
                    fontSize = 15.sp
                )
            }

            if (backgroundProcesses.isEmpty()) {

                item {

                    Text(
                        text =
                            "No background processes currently exposed by Android.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }

            } else {

                items(
                    backgroundProcesses,
                    key = {
                        "${it.packageName}_${it.pid}"
                    }
                ) { process ->

                    BackgroundProcessCard(
                        process = process
                    )
                }
            }

            item {

                Text(
                    text = "RECENTLY ACTIVE",
                    color = PrimaryText,
                    fontSize = 15.sp
                )
            }

            if (recentlyActiveApps.isEmpty()) {

                item {

                    Text(
                        text = "No recently active applications.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }

            } else {

                items(
                    recentlyActiveApps,
                    key = {
                        "${it.packageName}_${it.lastActiveTime}"
                    }
                ) { app ->

                    RecentlyActiveCard(
                        process = app
                    )
                }
            }

            item {

                Text(
                    text = "RECENT PROCESS EVENTS",
                    color = PrimaryText,
                    fontSize = 15.sp
                )
            }

            if (exitHistory.isEmpty()) {

                item {

                    Text(
                        text =
                            "No process exit events reported by Android.",
                        color = SecondaryText,
                        fontSize = 12.sp
                    )
                }

            } else {

                items(
                    exitHistory,
                    key = {
                        "${it.packageName}_${it.timestamp}"
                    }
                ) { event ->

                    ProcessEventCard(
                        event = event
                    )
                }
            }

            item {
                Spacer(
                    Modifier.height(30.dp)
                )
            }
        }
    }
}


private fun openAppSettings(
    context: android.content.Context,
    packageName: String
) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        "package:$packageName".toUri()
    )

    context.startActivity(intent)
}


@Composable
private fun ProcessEventCard(
    event: ProcessExitInfo
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "dd MMM, HH:mm",
                Locale.getDefault()
            )
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Border,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Text(
            text = event.reason,
            color = PrimaryText,
            fontSize = 13.sp
        )

        Spacer(
            Modifier.height(5.dp)
        )

        Text(
            text = event.processName,
            color = PrimaryText,
            fontSize = 15.sp
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text = event.packageName,
            color = SecondaryText,
            fontSize = 12.sp
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text = formatter.format(
                Date(event.timestamp)
            ),
            color = SecondaryText,
            fontSize = 11.sp
        )

        event.description?.let {

            Spacer(
                Modifier.height(6.dp)
            )

            Text(
                text = it,
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun BackgroundProcessCard(
    process: ProcessInfo
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Border,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = process.processName,
                color = PrimaryText,
                fontSize = 15.sp
            )

            Text(
                text = "BACKGROUND",
                color = SecondaryText,
                fontSize = 10.sp
            )
        }

        Spacer(
            Modifier.height(5.dp)
        )

        Text(
            text = process.packageName,
            color = SecondaryText,
            fontSize = 12.sp
        )

        if (process.pid > 0) {

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                text = "PID ${process.pid}",
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun RecentlyActiveCard(
    process: ProcessInfo
) {

    val formatter =
        remember {
            SimpleDateFormat(
                "HH:mm:ss",
                Locale.getDefault()
            )
        }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Border,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = process.processName,
                color = PrimaryText,
                fontSize = 15.sp
            )

            Text(
                text = "RECENT",
                color = SecondaryText,
                fontSize = 10.sp
            )
        }

        Spacer(
            Modifier.height(5.dp)
        )

        Text(
            text = process.packageName,
            color = SecondaryText,
            fontSize = 12.sp
        )

        Spacer(
            Modifier.height(4.dp)
        )

        Text(
            text =
                "Last active ${formatter.format(
                    Date(process.lastActiveTime)
                )}",
            color = SecondaryText,
            fontSize = 11.sp
        )
    }
}