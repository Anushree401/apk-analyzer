package com.example.android_manager.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.CyberCyan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Main Scan History screen displaying logged APK and system audit records
 * with live search, severity filtering, inspection sheets, and deletion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryScreen(
    onBack: () -> Unit,
    onNavigateToScan: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember { ScanHistoryRepository(context) }
    val scope = rememberCoroutineScope()

    var allEntries by remember { mutableStateOf<List<ScanHistoryEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var searchQuery by remember { mutableStateOf("") }
    var selectedRiskFilter by remember { mutableStateOf<RiskLevel?>(null) }
    var selectedEntryForDetail by remember { mutableStateOf<ScanHistoryEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<ScanHistoryEntry?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    // Collect scan records from Room
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            repository.getAllHistory().collectLatest { list ->
                withContext(Dispatchers.Main) {
                    allEntries = list
                    isLoading = false
                }
            }
        }
    }

    // Filter entries based on search text and risk chip
    val filteredEntries = remember(allEntries, searchQuery, selectedRiskFilter) {
        allEntries.filter { entry ->
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                entry.targetName.contains(searchQuery, ignoreCase = true) ||
                        entry.packageName.contains(searchQuery, ignoreCase = true)
            }
            val matchesRisk = selectedRiskFilter == null || entry.riskLevel == selectedRiskFilter
            matchesSearch && matchesRisk
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyberCyan)
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
                        onRiskFilterSelect = { selectedRiskFilter = it }
                    )

                    if (filteredEntries.isEmpty()) {
                        ScanHistoryEmptyState(
                            isFiltered = allEntries.isNotEmpty(),
                            onResetFilter = {
                                searchQuery = ""
                                selectedRiskFilter = null
                            },
                            onStartScan = onNavigateToScan
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredEntries, key = { it.id }) { entry ->
                                ScanHistoryCard(
                                    entry = entry,
                                    onClick = { selectedEntryForDetail = entry },
                                    onDeleteClick = { entryToDelete = entry }
                                )
                            }
                        }
                    }
                }
            }
        }

        APVMBottomBar(modifier = Modifier.padding(12.dp))
    }

    // Detail Inspection Sheet
    selectedEntryForDetail?.let { entry ->
        ScanHistoryDetailSheet(
            entry = entry,
            onDismiss = { selectedEntryForDetail = null }
        )
    }

    // Delete Single Item Confirmation Dialog
    entryToDelete?.let { entry ->
        ScanHistoryDeleteDialog(
            isClearAll = false,
            targetName = entry.targetName,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    repository.deleteScan(entry.id)
                }
                entryToDelete = null
            },
            onDismiss = { entryToDelete = null }
        )
    }

    // Clear All Confirmation Dialog
    if (showClearAllDialog) {
        ScanHistoryDeleteDialog(
            isClearAll = true,
            onConfirm = {
                scope.launch(Dispatchers.IO) {
                    repository.clearHistory()
                }
                showClearAllDialog = false
            },
            onDismiss = { showClearAllDialog = false }
        )
    }
}