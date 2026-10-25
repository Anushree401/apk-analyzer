package com.example.android_manager.ui.process

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.android_manager.data.process.DeviceMemoryInfo
import com.example.android_manager.data.process.ProcessRepository
import com.example.android_manager.model.ProcessExitInfo
import com.example.android_manager.model.ProcessInfo
import com.example.android_manager.ui.components.APVMEmptyState
import com.example.android_manager.ui.components.APVMShimmerCard
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.verticalScrollbar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskCritical
import com.example.android_manager.ui.theme.RiskHigh
import com.example.android_manager.ui.theme.RiskLow
import com.example.android_manager.ui.theme.RiskMedium
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ProcessSortMode {
    IMPORTANCE,
    NAME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessManagerScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val repository = remember { ProcessRepository(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var currentProcess by remember { mutableStateOf<ProcessInfo?>(null) }
    var exitHistory by remember { mutableStateOf<List<ProcessExitInfo>>(emptyList()) }
    var usageAccess by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var backgroundProcesses by remember { mutableStateOf<List<ProcessInfo>>(emptyList()) }
    var recentlyActiveApps by remember { mutableStateOf<List<ProcessInfo>>(emptyList()) }
    var memoryInfo by remember { mutableStateOf<DeviceMemoryInfo?>(null) }

    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sortMode by rememberSaveable { mutableStateOf(ProcessSortMode.IMPORTANCE) }

    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    suspend fun refreshProcesses() {
        isRefreshing = true
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        try {
            val mem = withContext(Dispatchers.IO) { repository.getDeviceMemoryInfo() }
            memoryInfo = mem

            if (repository.hasUsageAccess()) {
                val overview = withContext(Dispatchers.IO) {
                    Triple(
                        repository.getCurrentForegroundApp(),
                        repository.getBackgroundProcesses(),
                        repository.getRecentlyActiveApps()
                    )
                }
                currentProcess = overview.first
                backgroundProcesses = overview.second
                recentlyActiveApps = overview.third
            }
            val exits = withContext(Dispatchers.IO) {
                repository.getProcessExitHistory()
            }
            exitHistory = exits
        } finally {
            isRefreshing = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                usageAccess = repository.hasUsageAccess()
                if (usageAccess) {
                    currentProcess = repository.getCurrentForegroundApp()
                }
                memoryInfo = repository.getDeviceMemoryInfo()
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
            val mem = withContext(Dispatchers.IO) { repository.getDeviceMemoryInfo() }
            val overview = withContext(Dispatchers.IO) {
                Triple(
                    repository.getCurrentForegroundApp(),
                    repository.getBackgroundProcesses(),
                    repository.getRecentlyActiveApps()
                )
            }
            memoryInfo = mem
            currentProcess = overview.first
            backgroundProcesses = overview.second
            recentlyActiveApps = overview.third
            isLoading = false
            delay(2000)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            exitHistory = withContext(Dispatchers.IO) {
                repository.getProcessExitHistory()
            }
            delay(5000)
        }
    }

    // Filter background and recent apps by search query
    val trimmedQuery = searchQuery.trim().lowercase(Locale.getDefault())

    val filteredBackground = remember(backgroundProcesses, trimmedQuery, sortMode) {
        val filtered = if (trimmedQuery.isEmpty()) {
            backgroundProcesses
        } else {
            backgroundProcesses.filter {
                it.processName.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
                        it.packageName.lowercase(Locale.getDefault()).contains(trimmedQuery)
            }
        }
        when (sortMode) {
            ProcessSortMode.IMPORTANCE -> filtered.sortedBy { it.importance }
            ProcessSortMode.NAME -> filtered.sortedBy { it.processName.lowercase(Locale.getDefault()) }
        }
    }

    val filteredRecent = remember(recentlyActiveApps, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            recentlyActiveApps
        } else {
            recentlyActiveApps.filter {
                it.processName.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
                        it.packageName.lowercase(Locale.getDefault()).contains(trimmedQuery)
            }
        }
    }

    val filteredExits = remember(exitHistory, trimmedQuery) {
        if (trimmedQuery.isEmpty()) {
            exitHistory
        } else {
            exitHistory.filter {
                it.processName.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
                        it.packageName.lowercase(Locale.getDefault()).contains(trimmedQuery) ||
                        it.reason.lowercase(Locale.getDefault()).contains(trimmedQuery)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { scope.launch { refreshProcesses() } },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .imePadding()
                        .verticalScrollbar(
                            scrollProgress = if (listState.layoutInfo.totalItemsCount > 1) {
                                listState.firstVisibleItemIndex.toFloat() / (listState.layoutInfo.totalItemsCount - 1)
                            } else 0f,
                            viewportRatio = 0.2f
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Process Manager",
                            color = PrimaryText,
                            fontSize = 22.sp
                        )
                        Text(
                            text = "Monitor live memory telemetry, foreground processes, and exit history",
                            color = SecondaryText,
                            fontSize = 13.sp
                        )
                        Spacer(Modifier.height(10.dp))
                    }

                    // Device Memory Telemetry Bar
                    item {
                        DeviceMemoryCard(memoryInfo = memoryInfo)
                    }

                    // Tabs: Live Telemetry vs Exit History
                    item {
                        TabRow(
                            selectedTabIndex = selectedTabIndex,
                            containerColor = Surface,
                            contentColor = CyberCyan,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                    color = CyberCyan,
                                    height = 2.dp
                                )
                            },
                            divider = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(Border)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Border, RoundedCornerShape(8.dp))
                        ) {
                            Tab(
                                selected = selectedTabIndex == 0,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTabIndex = 0
                                },
                                text = {
                                    Text(
                                        text = "ACTIVE (${filteredBackground.size + (if (currentProcess != null) 1 else 0)})",
                                        color = if (selectedTabIndex == 0) CyberCyan else SecondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTabIndex == 1,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedTabIndex = 1
                                },
                                text = {
                                    Text(
                                        text = "EXIT EVENTS (${filteredExits.size})",
                                        color = if (selectedTabIndex == 1) CyberCyan else SecondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                            )
                        }
                    }

                    // Search Filter Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = {
                                Text(
                                    text = if (selectedTabIndex == 0) "Filter active processes..." else "Filter crash & exit logs...",
                                    color = SecondaryText,
                                    fontSize = 13.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = SecondaryText,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = SecondaryText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Surface,
                                unfocusedContainerColor = Surface,
                                focusedBorderColor = CyberCyan,
                                unfocusedBorderColor = Border,
                                focusedTextColor = PrimaryText,
                                unfocusedTextColor = PrimaryText
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        )
                    }

                    if (!usageAccess) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, Border, RoundedCornerShape(12.dp))
                                    .background(CardBackground, RoundedCornerShape(12.dp))
                                    .padding(18.dp)
                            ) {
                                Text(
                                    text = "USAGE ACCESS REQUIRED",
                                    color = RiskMedium,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "APVM needs Usage Access permission to read real-time foreground apps and process telemetry on modern Android versions.",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(14.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val usageIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(usageIntent)
                                        } catch (_: Exception) {
                                            try {
                                                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(settingsIntent)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Graphite)
                                ) {
                                    Text(text = "OPEN USAGE SETTINGS", color = PrimaryText)
                                }
                            }
                        }
                    }

                    if (selectedTabIndex == 0) {
                        // TAB 0: LIVE PROCESSES
                        if (isLoading && usageAccess) {
                            items(3) {
                                APVMShimmerCard(modifier = Modifier.padding(vertical = 4.dp))
                            }
                        } else {
                            // Foreground app card
                            item {
                                Text(
                                    text = "CURRENT ACTIVITY",
                                    color = SecondaryText,
                                    fontSize = 12.sp
                                )
                                Spacer(Modifier.height(8.dp))
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Border, RoundedCornerShape(12.dp))
                                        .background(CardBackground, RoundedCornerShape(12.dp))
                                        .padding(18.dp)
                                ) {
                                    val process = currentProcess
                                    if (process != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = process.processName,
                                                    color = PrimaryText,
                                                    fontSize = 17.sp
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    text = process.packageName,
                                                    color = CyberCyan,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            Button(
                                                onClick = { openAppSettings(context, process.packageName) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Graphite),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(text = "SETTINGS", color = PrimaryText, fontSize = 11.sp)
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "NO FOREGROUND APP DETECTED",
                                            color = SecondaryText,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }

                            // Sort controls for background apps
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "BACKGROUND PROCESSES (${filteredBackground.size})",
                                        color = PrimaryText,
                                        fontSize = 14.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = if (sortMode == ProcessSortMode.IMPORTANCE) "BY IMPORTANCE" else "BY NAME (A-Z)",
                                            color = CyberCyan,
                                            fontSize = 11.sp,
                                            modifier = Modifier
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    sortMode = if (sortMode == ProcessSortMode.IMPORTANCE) {
                                                        ProcessSortMode.NAME
                                                    } else {
                                                        ProcessSortMode.IMPORTANCE
                                                    }
                                                }
                                                .border(1.dp, Border, RoundedCornerShape(4.dp))
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (filteredBackground.isEmpty()) {
                                item {
                                    if (searchQuery.isNotEmpty()) {
                                        APVMEmptyState(
                                            title = "No Matching Processes",
                                            description = "No active processes match \"$searchQuery\"."
                                        )
                                    } else {
                                        Text(
                                            text = "No background processes exposed by Android sandbox.",
                                            color = SecondaryText,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            } else {
                                itemsIndexed(
                                    items = filteredBackground,
                                    key = { index, process -> "${process.packageName}_${process.pid}_$index" }
                                ) { _, process ->
                                    BackgroundProcessCard(
                                        process = process,
                                        onSettingsClick = { openAppSettings(context, process.packageName) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }

                            // Recently Active
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "RECENTLY ACTIVE APPS (${filteredRecent.size})",
                                    color = PrimaryText,
                                    fontSize = 14.sp
                                )
                            }

                            if (filteredRecent.isEmpty()) {
                                item {
                                    Text(
                                        text = "No recently active applications recorded.",
                                        color = SecondaryText,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                itemsIndexed(
                                    items = filteredRecent,
                                    key = { index, app -> "${app.packageName}_${app.lastActiveTime}_$index" }
                                ) { _, app ->
                                    RecentlyActiveCard(
                                        process = app,
                                        onSettingsClick = { openAppSettings(context, app.packageName) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        }
                    } else {
                        // TAB 1: EXIT HISTORY
                        if (filteredExits.isEmpty()) {
                            item {
                                APVMEmptyState(
                                    title = if (searchQuery.isNotEmpty()) "No Matching Exit Events" else "No Process Exit Events",
                                    description = if (searchQuery.isNotEmpty()) {
                                        "No exit events match \"$searchQuery\"."
                                    } else {
                                        "Android has not recorded unexpected terminations or crashes in the recent lookback window."
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = filteredExits,
                                key = { index, event -> "${event.packageName}_${event.timestamp}_$index" }
                            ) { _, event ->
                                ProcessEventCard(
                                    event = event,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    }

                    item {
                        Spacer(Modifier.height(30.dp))
                    }
                }

                if (showScrollToTop) {
                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        containerColor = Surface,
                        contentColor = CyberCyan,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                            .size(44.dp)
                            .border(1.dp, Border, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeviceMemoryCard(
    memoryInfo: DeviceMemoryInfo?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DEVICE RAM TELEMETRY",
                color = SecondaryText,
                fontSize = 11.sp
            )
            if (memoryInfo?.lowMemory == true) {
                Text(
                    text = "LOW MEMORY",
                    color = RiskCritical,
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = "OPTIMAL",
                    color = RiskLow,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (memoryInfo != null) {
            val percentage = (memoryInfo.usedPercentage * 100).toInt()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${memoryInfo.usedMemFormatted} / ${memoryInfo.totalMemFormatted}",
                    color = PrimaryText,
                    fontSize = 18.sp
                )
                Text(
                    text = "$percentage% USED",
                    color = if (percentage > 85) RiskMedium else CyberCyan,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { memoryInfo.usedPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (memoryInfo.lowMemory || percentage > 85) RiskCritical else CyberCyan,
                trackColor = Surface
            )
        } else {
            Text(
                text = "Reading memory statistics...",
                color = SecondaryText,
                fontSize = 13.sp
            )
        }
    }
}

private fun openAppSettings(
    context: Context,
    packageName: String
) {
    try {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:$packageName".toUri()
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        try {
            val fallback = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
        } catch (_: Exception) {}
    }
}

@Composable
private fun ProcessEventCard(
    event: ProcessExitInfo,
    modifier: Modifier = Modifier
) {
    val formatter = remember {
        SimpleDateFormat("dd MMM, HH:mm:ss", Locale.getDefault())
    }

    val reasonColor = when (event.reason.uppercase(Locale.US)) {
        "CRASH", "NATIVE CRASH" -> RiskCritical
        "ANR" -> RiskHigh
        "LOW MEMORY", "EXCESSIVE RESOURCE USAGE" -> RiskMedium
        else -> SecondaryText
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = event.reason,
                color = reasonColor,
                fontSize = 12.sp
            )
            Text(
                text = formatter.format(Date(event.timestamp)),
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = event.processName,
            color = PrimaryText,
            fontSize = 15.sp
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = event.packageName,
            color = SecondaryText,
            fontSize = 12.sp
        )

        event.description?.let { desc ->
            if (desc.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = desc,
                    color = SecondaryText,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun BackgroundProcessCard(
    process: ProcessInfo,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = process.processName,
                color = PrimaryText,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "BG",
                color = SecondaryText,
                fontSize = 10.sp,
                modifier = Modifier
                    .border(1.dp, Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = process.packageName,
            color = SecondaryText,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (process.pid > 0) "PID ${process.pid} · Imp: ${process.importance}" else "Imp: ${process.importance}",
                color = Graphite,
                fontSize = 11.sp
            )

            Text(
                text = "SETTINGS",
                color = CyberCyan,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable { onSettingsClick() }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun RecentlyActiveCard(
    process: ProcessInfo,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember {
        SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(12.dp))
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = process.processName,
                color = PrimaryText,
                fontSize = 15.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "RECENT",
                color = RiskMedium,
                fontSize = 10.sp,
                modifier = Modifier
                    .border(1.dp, Border, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = process.packageName,
            color = SecondaryText,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Last active ${formatter.format(Date(process.lastActiveTime))}",
                color = Graphite,
                fontSize = 11.sp
            )

            Text(
                text = "SETTINGS",
                color = CyberCyan,
                fontSize = 11.sp,
                modifier = Modifier
                    .clickable { onSettingsClick() }
                    .padding(4.dp)
            )
        }
    }
}