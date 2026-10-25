
package com.example.android_manager.ui.permissions

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.permissions.AppPermissionInfo
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.ui.components.APVMEmptyState
import com.example.android_manager.ui.components.APVMErrorState
import com.example.android_manager.ui.components.APVMShimmerCard
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.keyboardFocusable
import com.example.android_manager.ui.components.verticalScrollbar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AuditorCyan = CyberCyan
private val AuditorCard = CardBackground
private val AuditorInner = Surface
private val AuditorBorder = Border
private val AuditorMuted = MutedText

private enum class PermissionFilter(val label: String) {
    ALL("All"),
    DANGEROUS("Dangerous"),
    SPECIAL("Special"),
    NORMAL("Normal")
}

enum class AppSortOption(val label: String) {
    RISK("Risk Level"),
    NAME("Name (A-Z)"),
    INSTALL_DATE("Install Date"),
    SIZE("App Size")
}

private enum class AppSource {
    EXTERNAL,
    SYSTEM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionAuditorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val appRepository = remember {
        AppRepository(context)
    }

    val permissionRepository = remember {
        PermissionRepository(context)
    }

    var auditedApps by remember {
        mutableStateOf<List<AppPermissionInfo>?>(null)
    }

    var selectedSourceName by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val selectedSource = selectedSourceName?.let { AppSource.valueOf(it) }

    var selectedFilterName by rememberSaveable {
        mutableStateOf(PermissionFilter.ALL.name)
    }
    val selectedFilter = PermissionFilter.valueOf(selectedFilterName)

    var selectedSortName by rememberSaveable {
        mutableStateOf(AppSortOption.RISK.name)
    }
    val selectedSort = AppSortOption.valueOf(selectedSortName)

    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    var isAuditing by remember {
        mutableStateOf(false)
    }

    var auditError by remember {
        mutableStateOf<String?>(null)
    }

    val listState = rememberLazyListState()
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    BackHandler(enabled = selectedSource != null || searchQuery.isNotEmpty()) {
        if (searchQuery.isNotEmpty()) {
            searchQuery = ""
        } else if (selectedSource != null) {
            selectedSourceName = null
        }
    }

    suspend fun performAudit() {
        if (isAuditing) return

        isAuditing = true
        auditError = null

        try {
            val result = withContext(Dispatchers.IO) {
                val apps = appRepository.getInstalledApps()
                permissionRepository.getAppPermissions(apps)
            }

            auditedApps = result
        } catch (e: Exception) {
            auditError = e.message ?: "Permission audit failed."
        } finally {
            isAuditing = false
        }
    }

    LaunchedEffect(Unit) {
        performAudit()
    }

    val allApps = auditedApps.orEmpty()

    val externalApps = allApps.filter {
        !it.app.isSystemApp
    }

    val systemApps = allApps.filter {
        it.app.isSystemApp
    }

    // These are permission totals, not counts of affected applications.
    val dangerousPermissionCount = allApps.sumOf {
        it.dangerousPermissions.size
    }

    val specialPermissionCount = allApps.sumOf {
        it.specialPermissions.size
    }

    val sourceApps = when (selectedSource) {
        AppSource.EXTERNAL -> externalApps
        AppSource.SYSTEM -> systemApps
        null -> emptyList()
    }

    val filteredApps = sourceApps.filter { appInfo ->
        val matchesSearch =
            appInfo.app.name.contains(searchQuery, ignoreCase = true) ||
                    appInfo.app.packageName.contains(searchQuery, ignoreCase = true) ||
                    appInfo.permissions.any {
                        it.name.contains(searchQuery, ignoreCase = true)
                    }

        val matchesFilter = when (selectedFilter) {
            PermissionFilter.ALL -> true

            PermissionFilter.DANGEROUS ->
                appInfo.dangerousPermissions.isNotEmpty()

            PermissionFilter.SPECIAL ->
                appInfo.specialPermissions.isNotEmpty()

            PermissionFilter.NORMAL ->
                appInfo.dangerousPermissions.isEmpty() &&
                        appInfo.specialPermissions.isEmpty()
        }

        matchesSearch && matchesFilter
    }

    val sortedApps = remember(filteredApps, selectedSort) {
        when (selectedSort) {
            AppSortOption.RISK -> filteredApps.sortedByDescending { it.dangerousPermissions.size * 2 + it.specialPermissions.size }
            AppSortOption.NAME -> filteredApps.sortedBy { it.app.name.lowercase(Locale.getDefault()) }
            AppSortOption.INSTALL_DATE -> filteredApps.sortedByDescending { it.app.firstInstallTime }
            AppSortOption.SIZE -> filteredApps.sortedByDescending { it.app.apkSizeBytes }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        val totalItems = (if (selectedSource == null) 6 else (6 + filteredApps.size)).coerceAtLeast(1)
        val visibleFraction = (listState.layoutInfo.visibleItemsInfo.size.toFloat() / totalItems).coerceIn(0.1f, 1f)
        val scrollProgress = if (totalItems > 1) {
            listState.firstVisibleItemIndex.toFloat() / (totalItems - 1)
        } else 0f

        PullToRefreshBox(
            isRefreshing = isAuditing,
            onRefresh = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                scope.launch { performAudit() }
            },
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
                        .verticalScrollbar(
                            scrollProgress = scrollProgress,
                            viewportRatio = visibleFraction
                        ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Page header
                    item {
                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = "DEVICE AUDIT / 01",
                            color = AuditorCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.5.sp
                        )

                        Spacer(Modifier.height(7.dp))

                        Text(
                            text = if (selectedSource == null) {
                                "Permission Auditor"
                            } else if (selectedSource == AppSource.EXTERNAL) {
                                "External Apps"
                            } else {
                                "Internal / Android"
                            },
                            color = PrimaryText,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(5.dp))

                        Text(
                            text = when {
                                isAuditing ->
                                    "AUDITING INSTALLED APPLICATIONS..."

                                auditedApps == null ->
                                    "PREPARING PERMISSION AUDIT..."

                                selectedSource == AppSource.EXTERNAL ->
                                    "${externalApps.size} EXTERNAL APPLICATIONS"

                                selectedSource == AppSource.SYSTEM ->
                                    "${systemApps.size} SYSTEM APPLICATIONS"

                                else ->
                                    "${allApps.size} APPLICATIONS AUDITED"
                            },
                            color = SecondaryText,
                            fontSize = 10.sp,
                            letterSpacing = 0.7.sp
                        )

                        Spacer(Modifier.height(10.dp))
                    }

                    // Audit progress
                    if (isAuditing && auditedApps == null) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                repeat(4) {
                                    APVMShimmerCard()
                                }
                            }
                        }
                    }

                    // Audit errors
                    if (auditError != null) {
                        item {
                            APVMErrorState(
                                message = auditError.orEmpty(),
                                title = "Audit Failed",
                                onRetry = {
                                    scope.launch {
                                        performAudit()
                                    }
                                }
                            )
                        }
                    }

                    // Main source-selection page
                    if (auditedApps != null && selectedSource == null) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                AuditorStatCard(
                                    value = allApps.size.toString(),
                                    label = "APPLICATIONS",
                                    modifier = Modifier.weight(1f)
                                )

                                AuditorStatCard(
                                    value = dangerousPermissionCount.toString(),
                                    label = "DANGEROUS PERMS",
                                    modifier = Modifier.weight(1f),
                                    accent = true
                                )

                                AuditorStatCard(
                                    value = specialPermissionCount.toString(),
                                    label = "SPECIAL PERMS",
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = "APPLICATION SOURCES",
                                color = PrimaryText,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Choose which group you want to inspect.",
                                color = SecondaryText,
                                fontSize = 12.sp
                            )
                        }

                        item {
                            AppSourceCard(
                                title = "External Apps",
                                description = "Inspect non-system apps, requested permissions and sensitive access.",
                                count = externalApps.size,
                                symbol = "EXT",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSourceName = AppSource.EXTERNAL.name
                                    searchQuery = ""
                                    selectedFilterName = PermissionFilter.ALL.name
                                }
                            )
                        }

                        item {
                            AppSourceCard(
                                title = "Internal / Android",
                                description = "Explore system-flagged apps and their requested permissions.",
                                count = systemApps.size,
                                symbol = "SYS",
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    selectedSourceName = AppSource.SYSTEM.name
                                    searchQuery = ""
                                    selectedFilterName = PermissionFilter.ALL.name
                                }
                            )
                        }

                        item {
                            Text(
                                text = "DANGEROUS and SPECIAL totals count permission entries across all audited apps, not unique apps.",
                                color = AuditorMuted,
                                fontSize = 10.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 3.dp, bottom = 16.dp)
                            )
                        }
                    }

                    // Selected source: search and filters
                    if (auditedApps != null && selectedSource != null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AuditorCard)
                                    .border(
                                        1.dp,
                                        AuditorBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        selectedSourceName = null
                                        searchQuery = ""
                                        selectedFilterName = PermissionFilter.ALL.name
                                    }
                                    .padding(horizontal = 13.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "←",
                                    color = AuditorCyan,
                                    fontSize = 20.sp
                                )

                                Spacer(Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "APPLICATION SOURCES",
                                        color = SecondaryText,
                                        fontSize = 9.sp,
                                        letterSpacing = 0.7.sp
                                    )

                                    Spacer(Modifier.height(3.dp))

                                    Text(
                                        text = "Back to categories",
                                        color = PrimaryText,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Text(
                                    text = if (selectedSource == AppSource.EXTERNAL) {
                                        "EXTERNAL"
                                    } else {
                                        "SYSTEM"
                                    },
                                    color = AuditorCyan,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(AuditorCard)
                                    .border(
                                        1.dp,
                                        AuditorBorder,
                                        RoundedCornerShape(11.dp)
                                    )
                                    .keyboardFocusable(shape = RoundedCornerShape(11.dp))
                                    .imePadding()
                                    .padding(horizontal = 12.dp, vertical = 13.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "⌕",
                                    color = AuditorMuted,
                                    fontSize = 20.sp
                                )

                                Spacer(Modifier.width(8.dp))

                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search apps or permissions...",
                                            color = AuditorMuted,
                                            fontSize = 12.sp
                                        )
                                    }

                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = PrimaryText,
                                            fontSize = 12.sp
                                        ),
                                        cursorBrush = SolidColor(AuditorCyan),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                if (searchQuery.isNotEmpty()) {
                                    Text(
                                        text = "CLEAR",
                                        color = AuditorCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                searchQuery = ""
                                            }
                                            .padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                PermissionFilter.entries.forEach { filter ->
                                    FilterChip(
                                        label = filter.label,
                                        selected = selectedFilter == filter,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedFilterName = filter.name
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SORT BY:",
                                    color = AuditorMuted,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(end = 2.dp)
                                )
                                AppSortOption.entries.forEach { sortOption ->
                                    val isSelected = selectedSort == sortOption
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) AuditorCyan.copy(alpha = 0.12f) else AuditorInner)
                                            .border(1.dp, if (isSelected) AuditorCyan else AuditorBorder, RoundedCornerShape(6.dp))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                selectedSortName = sortOption.name
                                            }
                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = sortOption.label,
                                            color = if (isSelected) AuditorCyan else SecondaryText,
                                            fontSize = 9.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${sortedApps.size} RESULTS",
                                    color = AuditorMuted,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = if (isAuditing) "REFRESHING..." else "AUDIT",
                                    color = AuditorCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        scope.launch {
                                            performAudit()
                                        }
                                    }
                                )
                            }
                        }

                        if (sortedApps.isEmpty()) {
                            item {
                                APVMEmptyState(
                                    title = "No Matching Applications",
                                    description = if (searchQuery.isNotEmpty())
                                        "No applications match \"$searchQuery\"."
                                    else
                                        "No applications match the active permission filter.",
                                    actionLabel = "Reset Filter",
                                    onActionClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        searchQuery = ""
                                        selectedFilterName = PermissionFilter.ALL.name
                                    }
                                )
                            }
                        } else {
                            itemsIndexed(
                                items = sortedApps,
                                key = { index, appInfo -> "${appInfo.app.packageName}_$index" }
                            ) { _, appInfo ->
                                PermissionAppCard(
                                    appInfo = appInfo,
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }

                        item {
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }

                if (showScrollToTop) {
                    FloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        containerColor = AuditorInner,
                        contentColor = AuditorCyan,
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(20.dp)
                            .size(44.dp)
                            .border(1.dp, AuditorBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Scroll to top",
                            tint = AuditorCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditorStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(13.dp))
            .background(AuditorCard)
            .border(
                1.dp,
                AuditorBorder,
                RoundedCornerShape(13.dp)
            )
            .padding(horizontal = 10.dp, vertical = 14.dp)
    ) {
        Text(
            text = value,
            color = if (accent) AuditorCyan else PrimaryText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = label,
            color = SecondaryText,
            fontSize = 8.sp,
            letterSpacing = 0.2.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun AppSourceCard(
    title: String,
    description: String,
    count: Int,
    symbol: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(AuditorCard)
            .border(
                1.dp,
                AuditorBorder,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AuditorInner),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = symbol,
                    color = AuditorCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.width(13.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = PrimaryText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = description,
                    color = SecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "↗",
                color = AuditorCyan,
                fontSize = 20.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AuditorBorder)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$count APPLICATIONS",
                color = AuditorCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp
            )

            Text(
                text = "VIEW DETAILS",
                color = AuditorMuted,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(9.dp))
            .background(
                if (selected) AuditorCyan.copy(alpha = 0.12f)
                else AuditorCard
            )
            .border(
                1.dp,
                if (selected) AuditorCyan else AuditorBorder,
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) AuditorCyan else SecondaryText,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun PermissionCountPill(
    label: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(7.dp))
            .background(AuditorInner)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = count.toString(),
            color = if (count > 0) AuditorCyan else SecondaryText,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = label,
            color = SecondaryText,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun AuditorActionButton(
    label: String,
    modifier: Modifier = Modifier,
    isDestructive: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isDestructive) Color(0xFF331616) else AuditorInner)
            .border(
                1.dp,
                if (isDestructive) Color(0xFFFF5252).copy(alpha = 0.5f) else AuditorBorder,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isDestructive) Color(0xFFFF5252) else AuditorCyan,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun PermissionAppCard(
    appInfo: AppPermissionInfo,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var expanded by remember(appInfo.app.packageName) {
        mutableStateOf(false)
    }

    val dangerousCount = appInfo.dangerousPermissions.size
    val specialCount = appInfo.specialPermissions.size
    val normalCount = appInfo.permissions.count {
        it.category == "NORMAL"
    }
    val otherCount = appInfo.permissions.count {
        it.category == "OTHER"
    }

    val isSystemApp = appInfo.app.isSystemApp

    val category = when {
        specialCount > 0 -> "SPECIAL"
        dangerousCount > 0 -> "DANGEROUS"
        else -> "NORMAL"
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(AuditorCard)
            .border(
                1.dp,
                if (expanded) AuditorCyan.copy(alpha = 0.55f)
                else AuditorBorder,
                RoundedCornerShape(15.dp)
            )
            .combinedClickable(
                onClick = { expanded = !expanded },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Package Name", appInfo.app.packageName)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied ${appInfo.app.packageName}", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }
            )
            .padding(15.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = appInfo.app.name,
                    color = PrimaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = appInfo.app.packageName,
                    color = AuditorMuted,
                    fontSize = 10.sp
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = if (isSystemApp) "SYSTEM / ANDROID" else "EXTERNAL APP",
                    color = if (isSystemApp) AuditorCyan else SecondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = category,
                    color = if (
                        category == "DANGEROUS" ||
                        category == "SPECIAL"
                    ) AuditorCyan else SecondaryText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = if (expanded) "LESS  −" else "DETAILS  +",
                    color = AuditorMuted,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            PermissionCountPill("Dangerous", dangerousCount)
            PermissionCountPill("Special", specialCount)
            PermissionCountPill("Normal", normalCount + otherCount)
        }

        if (expanded) {
            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AuditorBorder)
            )
            Spacer(Modifier.height(14.dp))

            // Quick App Actions Bar
            Text(
                text = "APP ACTIONS",
                color = AuditorCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(appInfo.app.packageName)
                if (launchIntent != null) {
                    AuditorActionButton(label = "OPEN") {
                        try {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(launchIntent)
                        } catch (_: Exception) {}
                    }
                }

                AuditorActionButton(label = "APP INFO") {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${appInfo.app.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {}
                }

                AuditorActionButton(label = "COPY PKG") {
                    try {
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Package Name", appInfo.app.packageName)
                        clipboard.setPrimaryClip(clip)
                        android.widget.Toast.makeText(context, "Copied ${appInfo.app.packageName}", android.widget.Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {}
                }

                AuditorActionButton(label = "PLAY STORE ↗") {
                    try {
                        val playIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${appInfo.app.packageName}")).apply {
                            setPackage("com.android.vending")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(playIntent)
                    } catch (_: Exception) {
                        try {
                            val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${appInfo.app.packageName}")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(webIntent)
                        } catch (_: Exception) {}
                    }
                }

                if (!isSystemApp) {
                    AuditorActionButton(label = "UNINSTALL", isDestructive = true) {
                        try {
                            val intent = Intent(Intent.ACTION_DELETE).apply {
                                data = Uri.parse("package:${appInfo.app.packageName}")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                }

                AuditorActionButton(label = "SHARE") {
                    try {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "App: ${appInfo.app.name}")
                            putExtra(Intent.EXTRA_TEXT, "Package: ${appInfo.app.packageName}\nVersion: ${appInfo.app.versionName ?: "N/A"}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        val chooser = Intent.createChooser(sendIntent, "Share App").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(chooser)
                    } catch (_: Exception) {}
                }

                val certSha256 = appInfo.app.certificateSha256
                if (!certSha256.isNullOrBlank()) {
                    val rawHash = certSha256.replace(":", "").lowercase()
                    AuditorActionButton(label = "VIRUSTOTAL ↗") {
                        try {
                            val vtIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.virustotal.com/gui/file/$rawHash")).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(vtIntent)
                        } catch (_: Exception) {}
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = "METADATA & PROVENANCE",
                color = AuditorCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(AuditorInner)
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isSystemApp) {
                        "System-flagged application"
                    } else {
                        "Non-system application"
                    },
                    color = PrimaryText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Source: ${appInfo.app.installSource ?: if (isSystemApp) "System Image" else "Sideloaded"}",
                    color = SecondaryText,
                    fontSize = 10.sp
                )

                if (appInfo.app.apkSizeBytes > 0L) {
                    val sizeMb = "%.1f MB".format(appInfo.app.apkSizeBytes / (1024f * 1024f))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "APK Size: $sizeMb",
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }

                if (appInfo.app.firstInstallTime > 0L) {
                    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(appInfo.app.firstInstallTime))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Installed: $dateStr",
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }

                if (appInfo.app.lastUpdateTime > 0L) {
                    val updateDateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(appInfo.app.lastUpdateTime))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Last Update: $updateDateStr",
                        color = SecondaryText,
                        fontSize = 10.sp
                    )
                }

                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Version: ${appInfo.app.versionName ?: "Unknown"}",
                    color = SecondaryText,
                    fontSize = 10.sp
                )

                if (!appInfo.app.certificateSha256.isNullOrBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "CERTIFICATE SHA-256",
                        color = AuditorCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(Modifier.height(2.dp))
                    SelectionContainer {
                        Text(
                            text = appInfo.app.certificateSha256,
                            color = SecondaryText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "PERMISSION BREAKDOWN",
                color = AuditorCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Spacer(Modifier.height(12.dp))

            val groups = listOf(
                "DANGEROUS PERMISSIONS" to appInfo.dangerousPermissions,
                "SPECIAL PERMISSIONS" to appInfo.specialPermissions,
                "NORMAL PERMISSIONS" to appInfo.permissions.filter {
                    it.category == "NORMAL"
                },
                "OTHER PERMISSIONS" to appInfo.permissions.filter {
                    it.category == "OTHER"
                }
            )

            if (appInfo.permissions.isEmpty()) {
                Text(
                    text = "No requested permissions were found.",
                    color = SecondaryText,
                    fontSize = 12.sp
                )
            } else {
                groups.forEach { (heading, permissions) ->
                    if (permissions.isNotEmpty()) {
                        Text(
                            text = heading,
                            color = SecondaryText,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.7.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        permissions.forEach { permission ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = permission.name,
                                        color = if (permission.category == "DANGEROUS" || permission.category == "SPECIAL") AuditorCyan else PrimaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Text(
                                        text = permission.category,
                                        color = if (permission.category == "DANGEROUS" || permission.category == "SPECIAL") AuditorCyan else SecondaryText,
                                        fontSize = 8.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Spacer(Modifier.height(3.dp))

                                Text(
                                    text = permission.explanation.plainSummary,
                                    color = PrimaryText,
                                    fontSize = 10.sp,
                                    lineHeight = 14.sp
                                )

                                if (permission.category == "DANGEROUS" || permission.category == "SPECIAL") {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "• Risk: ${permission.explanation.securityRisk}",
                                        color = SecondaryText,
                                        fontSize = 9.sp,
                                        lineHeight = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}
