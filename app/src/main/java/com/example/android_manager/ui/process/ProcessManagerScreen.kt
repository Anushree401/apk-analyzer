package com.example.android_manager.ui.process

import android.content.Intent
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.foundation.text.BasicTextField

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.android_manager.data.process.ProcessRepository
import com.example.android_manager.model.ProcessInfo
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface

@Composable
fun ProcessManagerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var refreshKey by remember {
        mutableStateOf(0)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var hasUsageAccess by remember {
        mutableStateOf(
            ProcessRepository(context).hasUsageAccess()
        )
    }

    /*
     * Re-check Usage Access every time the app
     * comes back to the foreground.
     */
    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {

                hasUsageAccess =
                    ProcessRepository(context).hasUsageAccess()

                if (hasUsageAccess) {
                    refreshKey++
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    /*
     * null = loading
     * emptyList = finished, nothing found
     * non-empty = real data
     */
    val processes by produceState<List<ProcessInfo>?>(
        initialValue = null,
        key1 = context,
        key2 = refreshKey,
        key3 = hasUsageAccess
    ) {

        if (!hasUsageAccess) {

            value = emptyList()

        } else {

            value = withContext(Dispatchers.IO) {
                ProcessRepository(context)
                    .getRecentlyActiveApps()
            }
        }
    }

    val filteredProcesses = processes?.filter {
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

        APVMTopBar()

        Text(
            text = "← BACK",
            color = SecondaryText,
            modifier = Modifier
                .padding(
                    horizontal = 20.dp,
                    vertical = 8.dp
                )
                .clickable {
                    onBack()
                }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
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
                text = when {

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

            /*
             * USAGE ACCESS REQUIRED
             */
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
                        text = "APVM needs Usage Access to detect recently active applications.",
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
                    text = "Enable APVM in Android's Usage Access settings, then return to this screen.",
                    color = SecondaryText,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(
                        horizontal = 4.dp
                    )
                )

            } else {

                /*
                 * PROCESS STATISTICS
                 */

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    ProcessStatCard(
                        value =
                            processes?.size?.toString()
                                ?: "!",
                        label = "PROCESSES",
                        modifier = Modifier.weight(1f)
                    )

                    ProcessStatCard(
                        value =
                            processes
                                ?.sumOf {
                                    it.memoryMb
                                }
                                ?.toString()
                                ?: "!",
                        label = "RAM MB",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(
                    modifier = Modifier.height(16.dp)
                )

                /*
                 * SEARCH + REFRESH
                 */

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

                            if (searchQuery.isEmpty()) {

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

        /*
         * PROCESS RESULTS
         */

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

            } else if (filteredProcesses.isEmpty()) {

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
                        .padding(horizontal = 20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        items = filteredProcesses,
                        key = {
                            "${it.processName}_${it.pid}"
                        }
                    ) { process ->

                        ProcessCard(process)
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
    process: ProcessInfo
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
    }
}