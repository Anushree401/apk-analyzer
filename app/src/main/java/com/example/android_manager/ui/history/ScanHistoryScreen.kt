package com.example.android_manager.ui.history

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.model.ScanType
import com.example.android_manager.ui.components.APVMEmptyState
import com.example.android_manager.ui.components.APVMShimmerCard
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.verticalScrollbar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText

/**
 * Main Scan History screen displaying logged APK and system audit records
 * with live search, severity filtering, inspection sheets, pull-to-refresh,
 * undoable deletion, and persistent state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    onBack: () -> Unit,
    onNavigateToScan: () -> Unit = {},
    onNavigateToReport: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    var allEntries by remember { mutableStateOf<List<ScanHistoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Phase 1.3: State survives rotation and process death
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedRiskFilterName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedRiskFilter = selectedRiskFilterName?.let { runCatching { RiskLevel.valueOf(it) }.getOrNull() }

    var selectedScanTypeName by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedScanType = selectedScanTypeName?.let { runCatching { ScanType.valueOf(it) }.getOrNull() }

    var selectedSortName by rememberSaveable { mutableStateOf(HistorySortOption.DATE_DESC.name) }
    val selectedSort = runCatching { HistorySortOption.valueOf(selectedSortName) }.getOrDefault(HistorySortOption.DATE_DESC)

    var selectedEntryForDetail by remember { mutableStateOf<ScanHistoryEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<ScanHistoryEntry?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    var comparePair by remember { mutableStateOf<Pair<ScanHistoryEntry, ScanHistoryEntry>?>(null) }
    var compareTargetEntry by remember { mutableStateOf<ScanHistoryEntry?>(null) }
    var perAppHistoryPackage by remember { mutableStateOf<String?>(null) }

    // Phase 1.2: BackHandler collapses filters/search first before navigating back
    val hasActiveFilters = searchQuery.isNotBlank() || selectedRiskFilter != null || selectedScanType != null
    BackHandler(enabled = hasActiveFilters) {
        searchQuery = ""
        selectedRiskFilterName = null
        selectedScanTypeName = null
    }

    // Collect scan records from Room
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            repository.getAllHistory().collectLatest { list ->
                withContext(Dispatchers.Main) {
                    allEntries = list
                    isLoading = false
                    isRefreshing = false
                }
            }
        }
    }

    // Filter and sort entries based on criteria
    val filteredEntries = remember(allEntries, searchQuery, selectedRiskFilter, selectedScanType, selectedSort) {
        val filtered = allEntries.filter { entry ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                entry.targetName.contains(searchQuery, ignoreCase = true) ||
                        entry.packageName.contains(searchQuery, ignoreCase = true)
            }
            val matchesRisk = selectedRiskFilter == null || entry.riskLevel == selectedRiskFilter
            val matchesType = selectedScanType == null || entry.scanType == selectedScanType
            matchesSearch && matchesRisk && matchesType
        }

        when (selectedSort) {
            HistorySortOption.DATE_DESC -> filtered.sortedByDescending { it.timestamp }
            HistorySortOption.DATE_ASC -> filtered.sortedBy { it.timestamp }
            HistorySortOption.SCORE_DESC -> filtered.sortedByDescending { it.riskScore }
            HistorySortOption.SCORE_ASC -> filtered.sortedBy { it.riskScore }
        }
    }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 2 }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(Background),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
        ) {
            APVMTopBar(onBack = onBack)

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    scope.launch(Dispatchers.IO) {
                        delay(400) // Brief feedback
                        withContext(Dispatchers.Main) { isRefreshing = false }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            repeat(5) {
                                APVMShimmerCard()
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            val threatCount = allEntries.count { it.criticalCount > 0 || it.highCount > 0 }
                            val cleanCount = allEntries.count { it.findingsCount == 0 }

                            ScanHistoryMetricsHeader(
                                totalCount = allEntries.size,
                                threatCount = threatCount,
                                cleanCount = cleanCount,
                                onClearAllClick = {
                                    if (allEntries.isNotEmpty()) {
                                        showClearAllDialog = true
                                    }
                                }
                            )

                            ScanHistoryFilterBar(
                                searchQuery = searchQuery,
                                onSearchQueryChange = { searchQuery = it },
                                selectedRiskFilter = selectedRiskFilter,
                                onRiskFilterSelect = { selectedRiskFilterName = it?.name },
                                selectedScanType = selectedScanType,
                                onScanTypeSelect = { selectedScanTypeName = it?.name },
                                selectedSort = selectedSort,
                                onSortSelect = { selectedSortName = it.name }
                            )

                            if (filteredEntries.isEmpty()) {
                                APVMEmptyState(
                                    title = if (allEntries.isEmpty()) "No Scans Recorded" else "No Matching Scans",
                                    description = if (allEntries.isEmpty())
                                        "Select an APK or audit an installed app to generate scan records."
                                    else
                                        "Try adjusting your risk filter or search query.",
                                    actionLabel = if (allEntries.isEmpty()) "Scan APK" else "Clear Filters",
                                    onActionClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (allEntries.isEmpty()) {
                                            onNavigateToScan()
                                        } else {
                                            searchQuery = ""
                                            selectedRiskFilterName = null
                                            selectedScanTypeName = null
                                        }
                                    }
                                )
                            } else {
                                val totalItems = filteredEntries.size
                                val visibleFraction = (listState.layoutInfo.visibleItemsInfo.size.toFloat() / totalItems.coerceAtLeast(1)).coerceIn(0.1f, 1f)
                                val scrollProgress = if (totalItems > 1) {
                                    listState.firstVisibleItemIndex.toFloat() / (totalItems - 1)
                                } else 0f

                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .verticalScrollbar(
                                            scrollProgress = scrollProgress,
                                            viewportRatio = visibleFraction
                                        ),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(
                                        items = filteredEntries,
                                        key = { index, entry -> "${entry.id}_${entry.timestamp}_$index" }
                                    ) { _, entry ->
                                        Box(modifier = Modifier.animateItem()) {
                                            ScanHistoryCard(
                                                entry = entry,
                                                onClick = { selectedEntryForDetail = entry },
                                                onDeleteClick = {
                                                    // Phase 1.8: Undoable single item deletion with instant DB removal
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    scope.launch {
                                                        withContext(Dispatchers.IO) {
                                                            repository.deleteScan(entry.id)
                                                        }
                                                        val result = snackbarHostState.showSnackbar(
                                                            message = "Removed scan for ${entry.targetName}",
                                                            actionLabel = "UNDO",
                                                            duration = SnackbarDuration.Short
                                                        )
                                                        if (result == SnackbarResult.ActionPerformed) {
                                                            withContext(Dispatchers.IO) {
                                                                repository.saveScan(entry)
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Floating scroll-to-top button
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
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // Detail Inspection Sheet
    selectedEntryForDetail?.let { entry ->
        ScanHistoryDetailSheet(
            entry = entry,
            onDismiss = { selectedEntryForDetail = null },
            onViewFullReport = onNavigateToReport,
            onCompare = { selected ->
                val otherForSamePkg = allEntries.firstOrNull { it.packageName == selected.packageName && it.id != selected.id }
                if (otherForSamePkg != null) {
                    val baseline = if (otherForSamePkg.timestamp < selected.timestamp) otherForSamePkg else selected
                    val current = if (otherForSamePkg.timestamp < selected.timestamp) selected else otherForSamePkg
                    comparePair = Pair(baseline, current)
                } else {
                    compareTargetEntry = selected
                }
            },
            onTimeline = { pkg ->
                perAppHistoryPackage = pkg
            }
        )
    }

    // Per-App History Timeline Dialog
    perAppHistoryPackage?.let { pkg ->
        PerAppHistoryDialog(
            packageName = pkg,
            allScans = allEntries,
            onDismiss = { perAppHistoryPackage = null },
            onCompare = { baseline, current ->
                comparePair = Pair(baseline, current)
                perAppHistoryPackage = null
            }
        )
    }

    // Scan Comparison Dialog
    comparePair?.let { pair ->
        ScanCompareDialog(
            baselineScan = pair.first,
            currentScan = pair.second,
            onDismiss = { comparePair = null }
        )
    }

    // Picker when comparing against any other scan
    compareTargetEntry?.let { target ->
        val candidateEntries = allEntries.filter { it.id != target.id }
        if (candidateEntries.isEmpty()) {
            AlertDialog(
                onDismissRequest = { compareTargetEntry = null },
                title = { Text("No Other Scans Available", color = PrimaryText, fontWeight = FontWeight.Bold) },
                text = { Text("You need at least two scans saved in history to compare differences.", color = SecondaryText, fontSize = 13.sp) },
                confirmButton = {
                    TextButton(onClick = { compareTargetEntry = null }) {
                        Text("OK", color = CyberCyan)
                    }
                },
                containerColor = CardBackground,
                shape = RoundedCornerShape(16.dp)
            )
        } else {
            Dialog(
                onDismissRequest = { compareTargetEntry = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxSize(0.75f)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Border, RoundedCornerShape(18.dp)),
                    color = CardBackground
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "CHOOSE SCAN TO COMPARE",
                                color = CyberCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            IconButton(onClick = { compareTargetEntry = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = SecondaryText)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Comparing with: ${target.targetName} (v${target.versionName})",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScrollbar(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(
                                items = candidateEntries,
                                key = { index, other -> "${other.id}_${other.timestamp}_$index" }
                            ) { _, other ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Surface)
                                        .border(1.dp, Border, RoundedCornerShape(10.dp))
                                        .clickable {
                                            val baseline = if (other.timestamp < target.timestamp) other else target
                                            val current = if (other.timestamp < target.timestamp) target else other
                                            comparePair = Pair(baseline, current)
                                            compareTargetEntry = null
                                        }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(other.targetName, color = PrimaryText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        Text("v${other.versionName} • Risk ${other.riskScore}/100 [${other.riskLevel.name}]", color = SecondaryText, fontSize = 11.sp)
                                    }
                                    Text("SELECT ⇄", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Clear All Confirmation Dialog
    if (showClearAllDialog) {
        ScanHistoryDeleteDialog(
            isClearAll = true,
            onConfirm = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch(Dispatchers.IO) {
                    repository.clearHistory()
                }
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}