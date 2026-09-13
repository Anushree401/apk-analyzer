package com.example.android_manager.ui.process

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
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
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext


@Composable
fun ProcessManagerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val repository = remember {
        ProcessRepository(context)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var hasUsageAccess by remember {
        mutableStateOf(
            repository.hasUsageAccess()
        )
    }

    var refreshKey by remember {
        mutableIntStateOf(0)
    }

    var exitRefreshKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {

        while (true) {

            delay(1000)

            if (repository.hasUsageAccess()) {
                refreshKey++
            }
        }
    }

    LaunchedEffect(Unit) {

        while (true) {

            delay(5000)

            exitRefreshKey++
        }
    }

    DisposableEffect(lifecycleOwner) {

        val observer =
            LifecycleEventObserver { _, event ->

                if (
                    event == Lifecycle.Event.ON_RESUME
                ) {

                    hasUsageAccess =
                        repository.hasUsageAccess()

                    if (hasUsageAccess) {
                        refreshKey++
                        exitRefreshKey++
                    }
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(
                observer
            )
        }
    }

    val processes by produceState<List<ProcessInfo>?>(
        initialValue = null,
        key1 = refreshKey,
        key2 = hasUsageAccess
    ) {

        if (!hasUsageAccess) {

            value = emptyList()

        } else {

            value =
                withContext(Dispatchers.IO) {

                    repository
                        .getRecentlyActiveApps()
                }
        }
    }

    val exitHistory =
        produceState<List<ProcessExitInfo>>(
            initialValue = emptyList(),
            key1 = exitRefreshKey
        ) {

            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R
            ) {

                value =
                    withContext(Dispatchers.IO) {
                        repository
                            .getProcessExitHistory()
                    }

            } else {

                value = emptyList()
            }
        }

    fun openAppInfo(packageName: String) {

        val intent =
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            ).apply {

                data =
                    Uri.parse(
                        "package:$packageName"
                    )
            }

        context.startActivity(intent)
    }

    val filteredProcesses =
        processes?.filter {

            it.processName.contains(
                searchQuery,
                ignoreCase = true
            )

        } ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {

        APVMTopBar(
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 20.dp
                )
        ) {

            Text(
                text = "PROCESS MANAGER",
                color = PrimaryText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(
                modifier = Modifier.height(6.dp)
            )

            Text(
                text =
                    when {

                        !hasUsageAccess ->
                            "USAGE ACCESS REQUIRED"

                        processes == null ->
                            "CALCULATING..."

                        else ->
                            "${processes!!.size} ACTIVE APPLICATIONS"
                    },
                color = SecondaryText,
                fontSize = 14.sp
            )

            Spacer(
                modifier = Modifier.height(18.dp)
            )

            if (!hasUsageAccess) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Surface,
                            RoundedCornerShape(12.dp)
                        )
                        .padding(18.dp)
                ) {

                    Text(
                        text = "USAGE ACCESS REQUIRED",
                        color = PrimaryText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            "APVM needs Usage Access to detect recently active applications.",
                        color = SecondaryText,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier = Modifier.height(14.dp)
                    )

                    Text(
                        text = "GRANT USAGE ACCESS",
                        color = PrimaryText,
                        modifier = Modifier
                            .background(
                                Graphite,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {

                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_USAGE_ACCESS_SETTINGS
                                    )
                                )
                            }
                            .padding(
                                horizontal = 16.dp,
                                vertical = 13.dp
                            ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )

                Text(
                    text =
                        "Enable APVM in Android's Usage Access settings, then return to this screen.",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(
                        horizontal = 4.dp
                    )
                )

            } else {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    ProcessStatCard(
                        value =
                            processes
                                ?.size
                                ?.toString()
                                ?: "!",
                        label = "PROCESSES",
                        modifier =
                            Modifier.weight(1f)
                    )

                    ProcessStatCard(
                        value = "—",
                        label = "RAM MB",
                        modifier =
                            Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    BasicTextField(
                        value = searchQuery,

                        onValueChange = {
                            searchQuery = it
                        },

                        modifier = Modifier
                            .weight(1f)
                            .background(
                                Surface,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(
                                horizontal = 14.dp,
                                vertical = 13.dp
                            ),

                        singleLine = true,

                        textStyle = TextStyle(
                            color = PrimaryText,
                            fontSize = 14.sp
                        ),

                        cursorBrush =
                            SolidColor(PrimaryText),

                        decorationBox = {
                                innerTextField ->

                            if (
                                searchQuery.isEmpty()
                            ) {

                                Text(
                                    text =
                                        "Search processes...",
                                    color =
                                        SecondaryText,
                                    fontSize = 14.sp
                                )
                            }

                            innerTextField()
                        }
                    )

                    Spacer(
                        modifier = Modifier.size(10.dp)
                    )

                    Text(
                        text = "REFRESH",
                        color = PrimaryText,
                        modifier = Modifier
                            .background(
                                Graphite,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {

                                refreshKey++
                                exitRefreshKey++
                            }
                            .padding(
                                horizontal = 14.dp,
                                vertical = 13.dp
                            ),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }


        Spacer(
            modifier = Modifier.height(16.dp)
        )

        if (hasUsageAccess) {

            if (processes == null) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    CircularProgressIndicator(
                        modifier =
                            Modifier.size(28.dp),
                        color = Graphite
                    )

                    Spacer(
                        modifier =
                            Modifier.height(12.dp)
                    )

                    Text(
                        text =
                            "Loading process information...",
                        color = SecondaryText
                    )
                }

            } else if (
                filteredProcesses.isEmpty()
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        text =
                            "NO RECENTLY ACTIVE APPLICATIONS",
                        color = PrimaryText,
                        fontSize = 14.sp,
                        fontWeight =
                            FontWeight.Medium
                    )

                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )

                    Text(
                        text =
                            if (searchQuery.isEmpty()) {
                                "No application activity was detected."
                            } else {
                                "No applications match your search."
                            },
                        color = SecondaryText,
                        fontSize = 13.sp
                    )
                }

            } else {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = 20.dp
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    /*
                     * ACTIVE APPLICATIONS
                     */
                    item {

                        Text(
                            text = "RECENTLY ACTIVE",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Medium
                        )
                    }

                    items(
                        items = filteredProcesses,
                        key = {
                            "${it.processName}_${it.pid}"
                        }
                    ) { process ->

                        ProcessCard(
                            process = process,
                            onTerminate = {
                                openAppInfo(
                                    process.processName
                                )
                            },
                            onHibernate = {
                                openAppInfo(
                                    process.processName
                                )
                            }
                        )
                    }


                    /*
                     * PROCESS EXIT HISTORY
                     */
                    item {

                        Spacer(
                            modifier =
                                Modifier.height(14.dp)
                        )

                        Text(
                            text =
                                "RECENT PROCESS EVENTS",
                            color = PrimaryText,
                            fontSize = 14.sp,
                            fontWeight =
                                FontWeight.Medium
                        )

                        Spacer(
                            modifier =
                                Modifier.height(4.dp)
                        )

                        Text(
                            text =
                                "Android-reported process exit history",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    }


                    if (
                        exitHistory.value.isEmpty()
                    ) {

                        item {

                            Text(
                                text =
                                    "No recent process exit events available.",
                                color = SecondaryText,
                                fontSize = 13.sp,
                                modifier =
                                    Modifier.padding(
                                        vertical = 8.dp
                                    )
                            )
                        }

                    } else {

                        items(
                            items = exitHistory.value,
                            key = {
                                "${it.packageName}_${it.timestamp}_${it.reason}"
                            }
                        ) { exit ->

                            ProcessExitCard(
                                exit = exit
                            )
                        }
                    }


                    item {

                        Spacer(
                            modifier =
                                Modifier.height(80.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .background(
                Surface,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Text(
            text = value,
            color = PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = label,
            color = SecondaryText,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ProcessCard(
    process: ProcessInfo,
    onTerminate: () -> Unit,
    onHibernate: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Surface,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween,
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = process.processName,
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight =
                        FontWeight.Medium
                )

                Spacer(
                    modifier =
                        Modifier.height(5.dp)
                )

                Text(
                    text =
                        if (process.pid >= 0) {
                            "PID ${process.pid}"
                        } else {
                            "PID unavailable"
                        },
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            }

            Text(
                text =
                    if (process.isForeground) {
                        "FOREGROUND"
                    } else {
                        "RECENT"
                    },
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight =
                    FontWeight.Medium
            )
        }


        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            Text(
                text = "RAM",
                color = SecondaryText,
                fontSize = 12.sp
            )

            Text(
                text =
                    if (process.memoryMb > 0) {
                        "${process.memoryMb} MB"
                    } else {
                        "Unavailable"
                    },
                color = PrimaryText,
                fontSize = 12.sp
            )
        }


        Spacer(
            modifier = Modifier.height(14.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {

            Text(
                text = "TERMINATE",
                color = PrimaryText,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Graphite,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable {
                        onTerminate()
                    }
                    .padding(
                        vertical = 11.dp
                    ),
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.Medium
            )

            Text(
                text = "HIBERNATE",
                color = PrimaryText,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        Graphite,
                        RoundedCornerShape(9.dp)
                    )
                    .clickable {
                        onHibernate()
                    }
                    .padding(
                        vertical = 11.dp
                    ),
                fontSize = 12.sp,
                fontWeight =
                    FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProcessExitCard(
    exit: ProcessExitInfo
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Surface,
                RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Text(
            text = exit.processName,
            color = PrimaryText,
            fontSize = 15.sp,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = exit.reason,
            color = PrimaryText,
            fontSize = 12.sp,
            fontWeight =
                FontWeight.Medium
        )

        Spacer(
            modifier = Modifier.height(5.dp)
        )

        Text(
            text = exit.packageName,
            color = SecondaryText,
            fontSize = 11.sp
        )

        exit.description?.let { description ->

            Spacer(
                modifier = Modifier.height(7.dp)
            )

            Text(
                text = description,
                color = SecondaryText,
                fontSize = 12.sp
            )
        }
    }
}