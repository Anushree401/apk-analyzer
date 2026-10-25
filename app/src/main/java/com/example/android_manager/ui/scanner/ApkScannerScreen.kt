package com.example.android_manager.ui.scanner

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.data.report.SecurityReportRepository
import com.example.android_manager.data.risk.RiskAssessmentRepository
import com.example.android_manager.data.scanner.ApkAnalysisResult
import com.example.android_manager.data.scanner.ApkScannerRepository
import com.example.android_manager.model.AppInfo
import com.example.android_manager.ui.components.APVMEmptyState
import com.example.android_manager.ui.components.APVMErrorState
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.verticalScrollbar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ApkScannerScreen(
    modifier: Modifier = Modifier,
    initialUri: Uri? = null,
    onBack: () -> Unit = {},
    onNavigateToRiskAssessment: (() -> Unit)? = null,
    onNavigateToReport: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var selectedUri by remember { mutableStateOf<Uri?>(initialUri) }
    var selectedName by remember { mutableStateOf<String?>(null) }
    var analysisResult by remember { mutableStateOf<ApkAnalysisResult?>(null) }
    var isScanning by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanStageText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var activeScanJob by remember { mutableStateOf<Job?>(null) }
    var showAppPickerDialog by remember { mutableStateOf(false) }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    BackHandler(enabled = isScanning || analysisResult != null || showAppPickerDialog) {
        if (showAppPickerDialog) {
            showAppPickerDialog = false
        } else if (isScanning) {
            activeScanJob?.cancel()
            activeScanJob = null
            isScanning = false
            errorMessage = "Scan cancelled by user."
        } else if (analysisResult != null) {
            analysisResult = null
            selectedUri = null
            selectedName = null
        } else {
            onBack()
        }
    }

    val apkPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            selectedUri = uri
            selectedName = uri.lastPathSegment ?: "Selected APK"
            analysisResult = null
            errorMessage = null
        }
    }

    fun cancelActiveScan() {
        activeScanJob?.cancel()
        activeScanJob = null
        isScanning = false
        errorMessage = "Scan aborted by user."
        scope.launch(Dispatchers.IO) {
            try {
                context.cacheDir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("apvm_scan_")) {
                        file.delete()
                    }
                }
            } catch (_: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    fun executeScan(targetUri: Uri) {
        val freeSpaceBytes = context.cacheDir.usableSpace
        if (freeSpaceBytes < 100 * 1024 * 1024) { // Less than 100MB
            errorMessage = "Low disk space warning: Device has less than 100MB available cache storage. Please free up space before staging and scanning APK archives."
            return
        }

        activeScanJob?.cancel()
        activeScanJob = scope.launch {
            isScanning = true
            errorMessage = null
            analysisResult = null
            scanProgress = 0.10f
            scanStageText = "INITIALIZING: Creating isolated staging sandbox..."

            val startTime = System.currentTimeMillis()

            try {
                delay(120)
                scanProgress = 0.30f
                scanStageText = "STAGE 1 / 3: Reading manifest, signatures & components..."

                val result = withContext(Dispatchers.IO) {
                    ApkScannerRepository(context).scanApk(targetUri)
                }

                scanProgress = 0.70f
                scanStageText = "STAGE 2 / 3: Inspecting DEX bytecodes for security indicators..."
                delay(150)

                scanProgress = 0.90f
                scanStageText = "STAGE 3 / 3: Computing composite risk score & persistence..."

                withContext(Dispatchers.IO) {
                    try {
                        val durationMs = System.currentTimeMillis() - startTime
                        val risk = RiskAssessmentRepository(context).assessApkResult(result)
                        val report = SecurityReportRepository(context).buildFromApkAnalysis(result, risk)
                        SecurityReportRepository.setActiveReport(report)
                        ScanHistoryRepository(context).recordApkScan(
                            result = result,
                            risk = risk,
                            durationMs = durationMs
                        )
                    } catch (_: Exception) {
                        // Non-blocking report persistence
                    }
                }

                scanProgress = 1.0f
                scanStageText = "ANALYSIS COMPLETE"
                analysisResult = result

            } catch (e: CancellationException) {
                errorMessage = "Scan aborted by user."
                withContext(Dispatchers.IO) {
                    try {
                        context.cacheDir.listFiles()?.filter { it.name.startsWith("apvm_scan_") }?.forEach { it.delete() }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                val msg = e.message ?: ""
                errorMessage = when {
                    msg.contains("zip", ignoreCase = true) || msg.contains("archive", ignoreCase = true) || msg.contains("EOF", ignoreCase = true) ->
                        "Corrupt APK file: The selected file is not a valid, intact Android Package archive or is corrupted."
                    msg.isNotBlank() -> msg
                    else -> "APK static analysis failed: Unable to parse package manifest or bytecode."
                }
            } finally {
                isScanning = false
                activeScanJob = null
            }
        }
    }

    LaunchedEffect(selectedUri) {
        val uri = selectedUri ?: return@LaunchedEffect
        executeScan(uri)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            APVMTopBar(onBack = onBack)

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .verticalScrollbar(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "STATIC SECURITY AUDIT",
                        color = CyberCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "APK Deep Scanner",
                        color = PrimaryText,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Inspect APK binaries or installed packages for dangerous permissions, exported components, and hardcoded secrets.",
                        color = SecondaryText,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Scanner Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CyberCyan)
                                .clickable(enabled = !isScanning) {
                                    apkPicker.launch("application/vnd.android.package-archive")
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    tint = Background,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "SELECT APK FILE",
                                    color = Background,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .defaultMinSize(minHeight = 46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardBackground)
                                .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                .clickable(enabled = !isScanning) {
                                    showAppPickerDialog = true
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Smartphone,
                                    contentDescription = null,
                                    tint = CyberCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "AUDIT INSTALLED APP",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.6.sp
                                )
                            }
                        }
                    }
                }

                // Scanning Progress Card
                if (isScanning) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "ANALYSIS IN PROGRESS",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "${(scanProgress * 100).toInt()}%",
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(Modifier.height(10.dp))

                            LinearProgressIndicator(
                                progress = { scanProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape),
                                color = CyberCyan,
                                trackColor = com.example.android_manager.ui.theme.Surface
                            )

                            Spacer(Modifier.height(12.dp))

                            Text(
                                text = scanStageText,
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            selectedName?.let { name ->
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Target: $name",
                                    color = SecondaryText,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0x22FF5252))
                                    .border(1.dp, Color(0xFFFF5252).copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .clickable { cancelActiveScan() }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .align(Alignment.End),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ABORT SCAN",
                                    color = Color(0xFFFF5252),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }

                // Error State Card
                if (errorMessage != null && !isScanning) {
                    item {
                        APVMErrorState(
                            title = "Scan Failed",
                            message = errorMessage ?: "Unknown error during static analysis.",
                            onRetry = {
                                selectedUri?.let { executeScan(it) }
                            }
                        )
                    }
                }

                // Empty State (no APK chosen)
                if (analysisResult == null && !isScanning && errorMessage == null) {
                    item {
                        APVMEmptyState(
                            title = "No Package Selected",
                            description = "Choose an APK from storage or select an already installed application to run on-device static security analysis."
                        )
                    }
                }

                // Analysis Result Header & Metadata Card
                analysisResult?.let { result ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardBackground)
                                .border(1.dp, Border, RoundedCornerShape(16.dp))
                                .padding(18.dp)
                        ) {
                            Text(
                                "TARGET METADATA",
                                color = CyberCyan,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )

                            Spacer(Modifier.height(10.dp))

                            Text(
                                text = result.apkInfo.appName ?: result.apkInfo.packageName,
                                color = PrimaryText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = result.apkInfo.packageName,
                                color = SecondaryText,
                                fontSize = 12.sp
                            )

                            Spacer(Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetadataPill(
                                    label = "VERSION",
                                    value = result.apkInfo.versionName ?: "v${result.apkInfo.versionCode ?: 0}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetadataPill(
                                    label = "TARGET SDK",
                                    value = "API ${result.apkInfo.targetSdk ?: "Unknown"}",
                                    modifier = Modifier.weight(1f)
                                )
                                MetadataPill(
                                    label = "FINDINGS",
                                    value = "${result.findings.size} issues",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            if (onNavigateToReport != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CyberCyan)
                                        .clickable(onClick = onNavigateToReport)
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "VIEW COMPLETE SECURITY REPORT",
                                        color = Background,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                }
                            }

                            if (onNavigateToRiskAssessment != null) {
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(com.example.android_manager.ui.theme.Surface)
                                        .border(1.dp, Border, RoundedCornerShape(8.dp))
                                        .clickable(onClick = onNavigateToRiskAssessment)
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "VIEW COMPOSITE RISK ASSESSMENT",
                                        color = PrimaryText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        letterSpacing = 0.6.sp
                                    )
                                }
                            }
                        }
                    }

                    if (result.findings.isEmpty()) {
                        item {
                            APVMEmptyState(
                                title = "Zero Security Flags",
                                description = "No static vulnerabilities, dangerous combinations, or suspicious bytecodes were detected in this package."
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "SECURITY FINDINGS (${result.findings.size})",
                                color = SecondaryText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.1.sp,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        itemsIndexed(
                            items = result.findings,
                            key = { index, finding -> "${finding.name}:${finding.affectedComponent}:$index" }
                        ) { _, finding ->
                            ApkFindingCard(finding = finding)
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(40.dp))
                }
            }
        }

        // Scroll to top button
        AnimatedVisibility(
            visible = showScrollToTop,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 20.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    scope.launch { listState.animateScrollToItem(0) }
                },
                containerColor = CardBackground,
                contentColor = CyberCyan,
                shape = CircleShape,
                modifier = Modifier
                    .size(46.dp)
                    .border(1.dp, Border, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Scroll to top"
                )
            }
        }
    }

    if (showAppPickerDialog) {
        InstalledAppPickerDialog(
            onDismiss = { showAppPickerDialog = false },
            onSelectApp = { app ->
                showAppPickerDialog = false
                try {
                    val pInfo = context.packageManager.getPackageInfo(app.packageName, 0)
                    val sourceDir = pInfo.applicationInfo?.sourceDir
                    if (sourceDir != null) {
                        val apkFile = File(sourceDir)
                        if (apkFile.exists() && apkFile.canRead()) {
                            selectedName = "${app.name} (${app.packageName})"
                            selectedUri = Uri.fromFile(apkFile)
                        } else {
                            errorMessage = "APK file is inaccessible for ${app.packageName}."
                        }
                    } else {
                        errorMessage = "Cannot locate source APK on device for ${app.packageName}."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error resolving APK: ${e.message}"
                }
            }
        )
    }
}

@Composable
private fun MetadataPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(com.example.android_manager.ui.theme.Surface)
            .border(1.dp, Border, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = value,
            color = PrimaryText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun InstalledAppPickerDialog(
    onDismiss: () -> Unit,
    onSelectApp: (AppInfo) -> Unit
) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = AppRepository(context).getInstalledApps()
                .sortedBy { it.name.lowercase() }
            apps = list
            isLoading = false
        }
    }

    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) apps
        else apps.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxSize(0.85f)
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
                        text = "AUDIT INSTALLED APP",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = SecondaryText
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or package...", color = MutedText, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = Border,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        cursorColor = CyberCyan
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = SecondaryText)
                    }
                )

                Spacer(Modifier.height(14.dp))

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Reading installed packages...", color = SecondaryText, fontSize = 12.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScrollbar(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(
                            items = filteredApps,
                            key = { index, app -> "${app.packageName}_$index" }
                        ) { _, app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(com.example.android_manager.ui.theme.Surface)
                                    .border(1.dp, Border, RoundedCornerShape(10.dp))
                                    .clickable { onSelectApp(app) }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.name,
                                        color = PrimaryText,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = app.packageName,
                                        color = SecondaryText,
                                        fontSize = 11.sp
                                    )
                                }

                                Text(
                                    text = "AUDIT ↗",
                                    color = CyberCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}