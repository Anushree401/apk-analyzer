package com.example.android_manager.ui.scanner

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.android_manager.data.scanner.ApkAnalysisResult
import com.example.android_manager.data.scanner.ApkScannerRepository
import com.example.android_manager.ui.components.APVMBottomBar
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ApkScannerScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {}
) {

    val context = LocalContext.current

    var selectedUri by remember {
        mutableStateOf<Uri?>(null)
    }

    var selectedName by remember {
        mutableStateOf<String?>(null)
    }

    var analysisResult by remember {
        mutableStateOf<ApkAnalysisResult?>(null)
    }

    var isScanning by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    val apkPicker =
        rememberLauncherForActivityResult(
            contract =
                ActivityResultContracts.GetContent()
        ) { uri ->

            selectedUri = uri
            analysisResult = null
            errorMessage = null

            selectedName =
                uri?.lastPathSegment
        }

    LaunchedEffect(selectedUri) {

        val uri = selectedUri ?: return@LaunchedEffect

        isScanning = true
        errorMessage = null

        try {

            val result =
                withContext(Dispatchers.IO) {

                    ApkScannerRepository(
                        context
                    ).scanApk(uri)
                }

            analysisResult = result

        } catch (e: Exception) {

            errorMessage =
                e.message ?: "APK analysis failed."

        } finally {

            isScanning = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
    ) {

        APVMTopBar(onBack = onBack)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            item {

                Spacer(
                    modifier = Modifier.height(8.dp)
                )

                Text(
                    text = "APK SCANNER",
                    color = PrimaryText
                )

                Spacer(
                    modifier = Modifier.height(6.dp)
                )

                Text(
                    text = "select an APK to perform static security analysis",
                    color = SecondaryText
                )

                Spacer(
                    modifier = Modifier.height(18.dp)
                )

                Button(
                    onClick = {
                        apkPicker.launch(
                            "application/vnd.android.package-archive"
                        )
                    },
                    enabled = !isScanning
                ) {
                    Text("SELECT APK")
                }
            }

            selectedName?.let { name ->

                item {

                    Text(
                        text = "selected: $name",
                        color = SecondaryText
                    )
                }
            }

            if (isScanning) {

                item {

                    Text(
                        text = "ANALYZING APK...",
                        color = PrimaryText
                    )

                    Spacer(
                        modifier = Modifier.height(6.dp)
                    )

                    Text(
                        text = "reading metadata, manifest, components and DEX files",
                        color = SecondaryText
                    )
                }
            }

            errorMessage?.let { error ->

                item {

                    Text(
                        text = "SCAN FAILED",
                        color = PrimaryText
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        text = error,
                        color = SecondaryText
                    )
                }
            }

            analysisResult?.let { result ->

                item {

                    Text(
                        text = "APK INFORMATION",
                        color = PrimaryText
                    )

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    Text(
                        text = "app: ${result.apkInfo.appName}",
                        color = SecondaryText
                    )

                    Text(
                        text = "package: ${result.apkInfo.packageName}",
                        color = SecondaryText
                    )

                    Text(
                        text = "version: ${result.apkInfo.versionName ?: "unknown"}",
                        color = SecondaryText
                    )

                    Text(
                        text = "target sdk: ${result.apkInfo.targetSdk ?: "unknown"}",
                        color = SecondaryText
                    )

                    Text(
                        text = "findings: ${result.findings.size}",
                        color = SecondaryText
                    )
                }

                if (result.findings.isEmpty()) {

                    item {

                        Spacer(
                            modifier = Modifier.height(10.dp)
                        )

                        Text(
                            text = "NO FINDINGS DETECTED",
                            color = PrimaryText
                        )

                        Text(
                            text = "No configured static security indicators were detected.",
                            color = SecondaryText
                        )
                    }

                } else {

                    item {

                        Text(
                            text = "SECURITY FINDINGS",
                            color = PrimaryText
                        )
                    }

                    items(
                        items = result.findings,
                        key = {
                            "${it.name}:${it.affectedComponent}"
                        }
                    ) { finding ->

                        ApkFindingCard(
                            finding = finding
                        )
                    }
                }
            }
        }

        APVMBottomBar()
    }
}