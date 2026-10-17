
package com.example.android_manager.ui.permissions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.permissions.AppPermissionInfo
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.ui.components.APVMTopBar
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

private enum class AppSource {
    EXTERNAL,
    SYSTEM
}

@Composable
fun PermissionAuditorScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val appRepository = remember {
        AppRepository(context)
    }

    val permissionRepository = remember {
        PermissionRepository(context)
    }

    var auditedApps by remember {
        mutableStateOf<List<AppPermissionInfo>?>(null)
    }

    var selectedSource by remember {
        mutableStateOf<AppSource?>(null)
    }

    var selectedFilter by remember {
        mutableStateOf(PermissionFilter.ALL)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    var isAuditing by remember {
        mutableStateOf(false)
    }

    var auditError by remember {
        mutableStateOf<String?>(null)
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar(onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
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
                            .clip(RoundedCornerShape(16.dp))
                            .background(AuditorCard)
                            .border(
                                1.dp,
                                AuditorBorder,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = AuditorCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(30.dp)
                        )

                        Spacer(Modifier.height(14.dp))

                        Text(
                            text = "AUDITING APPLICATIONS",
                            color = PrimaryText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = "Reading requested permissions and classifying access levels.",
                            color = SecondaryText,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Audit errors
            if (auditError != null) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuditorCard)
                            .border(
                                1.dp,
                                AuditorBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(15.dp)
                    ) {
                        Text(
                            text = "AUDIT FAILED",
                            color = AuditorCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = auditError.orEmpty(),
                            color = SecondaryText,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = "RETRY AUDIT",
                            color = AuditorCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    performAudit()
                                }
                            }
                        )
                    }
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
                            selectedSource = AppSource.EXTERNAL
                            searchQuery = ""
                            selectedFilter = PermissionFilter.ALL
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
                            selectedSource = AppSource.SYSTEM
                            searchQuery = ""
                            selectedFilter = PermissionFilter.ALL
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
                                selectedSource = null
                                searchQuery = ""
                                selectedFilter = PermissionFilter.ALL
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
                                    .clickable { searchQuery = "" }
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
                        PermissionFilter.values().forEach { filter ->
                            FilterChip(
                                label = filter.label,
                                selected = selectedFilter == filter,
                                onClick = { selectedFilter = filter },
                                modifier = Modifier.weight(1f)
                            )
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
                            text = "${filteredApps.size} RESULTS",
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
                                scope.launch {
                                    performAudit()
                                }
                            }
                        )
                    }
                }

                if (filteredApps.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(13.dp))
                                .background(AuditorCard)
                                .border(
                                    1.dp,
                                    AuditorBorder,
                                    RoundedCornerShape(13.dp)
                                )
                                .padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "NO MATCHING APPLICATIONS",
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(Modifier.height(6.dp))

                            Text(
                                text = "Try another filter or search term.",
                                color = SecondaryText,
                                fontSize = 11.sp
                            )
                        }
                    }
                } else {
                    items(
                        items = filteredApps,
                        key = { it.app.packageName }
                    ) { appInfo ->
                        PermissionAppCard(appInfo = appInfo)
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
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
private fun PermissionAppCard(
    appInfo: AppPermissionInfo
) {
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
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(AuditorCard)
            .border(
                1.dp,
                if (expanded) AuditorCyan.copy(alpha = 0.55f)
                else AuditorBorder,
                RoundedCornerShape(15.dp)
            )
            .clickable { expanded = !expanded }
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

            Text(
                text = "APPLICATION SOURCE",
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

                Spacer(Modifier.height(5.dp))

                Text(
                    text = if (isSystemApp) {
                        "Android marks this application as a system app. It may be part of Android or supplied by the device manufacturer."
                    } else {
                        "Android does not mark this application as a system app. It may have been installed by the user or another installer."
                    },
                    color = SecondaryText,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )

                Spacer(Modifier.height(7.dp))

                Text(
                    text = "Version: ${appInfo.app.versionName ?: "Unknown"}",
                    color = SecondaryText,
                    fontSize = 10.sp
                )
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
                                Text(
                                    text = permission.name,
                                    color = PrimaryText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = permission.description,
                                    color = SecondaryText,
                                    fontSize = 10.sp,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(3.dp))
                    }
                }
            }
        }
    }
}
