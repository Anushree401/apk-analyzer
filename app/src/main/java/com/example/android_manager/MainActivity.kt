package com.example.android_manager

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.example.android_manager.data.settings.SettingsRepository
import com.example.android_manager.navigation.AppNavigation
import com.example.android_manager.ui.theme.Android_ManagerTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private var incomingApkUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install modern AndroidX splash screen before super.onCreate
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Install uncaught crash telemetry logger
        installCrashHandler()

        // Phase 3.2: Clean up any stale scan artifacts in cache on startup
        cleanupTempScanCache()

        // Phase 3.3: Handle incoming APK files via VIEW or SEND intents
        handleIncomingApkIntent(intent)

        enableEdgeToEdge()

        setContent {
            val settingsRepo = remember { SettingsRepository(this@MainActivity) }
            val appSettings by settingsRepo.settingsFlow.collectAsState(initial = null)
            val isAmoled = appSettings?.isAmoledMode ?: false

            Android_ManagerTheme(amoled = isAmoled) {
                AppNavigation(initialApkUri = incomingApkUri)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingApkIntent(intent)
    }

    private fun handleIncomingApkIntent(intent: Intent?) {
        if (intent == null) return
        val uri = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
            }
            else -> null
        }
        if (uri != null) {
            incomingApkUri = uri
        }
    }

    private fun installCrashHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val crashFile = File(cacheDir, "apvm_crash.log")
                val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).format(java.util.Date())
                val sw = java.io.StringWriter()
                val pw = java.io.PrintWriter(sw)
                throwable.printStackTrace(pw)
                val stackTrace = sw.toString()

                val logEntry = buildString {
                    appendLine("=======================================================")
                    appendLine("APVM CRASH REPORT - $timestamp")
                    appendLine("Thread: ${thread.name} (id: ${thread.id})")
                    appendLine("Exception: ${throwable::class.java.name}: ${throwable.message}")
                    appendLine("-------------------------------------------------------")
                    appendLine(stackTrace)
                    appendLine("=======================================================")
                    appendLine()
                }

                crashFile.appendText(logEntry)
            } catch (_: Throwable) {
                // Ignore secondary failure
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun cleanupTempScanCache() {
        try {
            val cacheFiles = cacheDir.listFiles { _, name ->
                name.startsWith("apvm_scan_") && name.endsWith(".apk")
            }
            cacheFiles?.forEach { it.delete() }
        } catch (_: Exception) {
            // Non-blocking cleanup
        }
    }
}