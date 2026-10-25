package com.example.android_manager

import com.example.android_manager.data.permissions.AppPermissionInfo
import com.example.android_manager.data.permissions.PermissionEntry
import com.example.android_manager.data.process.DeviceMemoryInfo
import com.example.android_manager.data.settings.AppSettings
import com.example.android_manager.model.AppInfo
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.VulnerabilityFinding
import com.example.android_manager.ui.history.PerAppScanDiff
import com.example.android_manager.ui.history.calculateScanDiff
import com.example.android_manager.ui.permissions.AppSortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class AppPolishFeaturesTest {

    @Test
    fun testDeviceMemoryInfoCalculations() {
        val totalBytes = 8L * 1024L * 1024L * 1024L // 8 GB
        val availBytes = 2L * 1024L * 1024L * 1024L // 2 GB
        val thresholdBytes = 500L * 1024L * 1024L // 500 MB

        val memInfo = DeviceMemoryInfo(
            totalMemBytes = totalBytes,
            availMemBytes = availBytes,
            lowMemory = false,
            thresholdBytes = thresholdBytes
        )

        val expectedUsed = 6L * 1024L * 1024L * 1024L
        assertEquals(expectedUsed, memInfo.usedMemBytes)
        assertEquals(0.75f, memInfo.usedPercentage, 0.001f)
        assertEquals("6.0 GB", memInfo.usedMemFormatted)
        assertEquals("8.0 GB", memInfo.totalMemFormatted)
        assertFalse(memInfo.lowMemory)
    }

    @Test
    fun testDeviceMemoryZeroTotalSafety() {
        val memInfo = DeviceMemoryInfo(
            totalMemBytes = 0L,
            availMemBytes = 0L,
            lowMemory = true,
            thresholdBytes = 0L
        )

        assertEquals(0L, memInfo.usedMemBytes)
        assertEquals(0f, memInfo.usedPercentage, 0.001f)
        assertTrue(memInfo.lowMemory)
    }

    @Test
    fun testAppSettingsDefaults() {
        val settings = AppSettings()
        assertFalse(settings.showSystemApps)
        assertTrue(settings.autoRefresh)
        assertTrue(settings.hapticFeedback)
        assertTrue(settings.isAmoledMode)
        assertFalse(settings.onboardingCompleted)
    }

    @Test
    fun testAppSortingByVariousOptions() {
        val app1 = AppPermissionInfo(
            app = AppInfo("Alpha Scanner", "com.alpha.scan", "1.0", false, firstInstallTime = 1000L, apkSizeBytes = 15_000_000L),
            permissions = listOf(
                PermissionEntry("android.permission.CAMERA", "Camera", "DANGEROUS")
            )
        )
        val app2 = AppPermissionInfo(
            app = AppInfo("Beta Messenger", "com.beta.chat", "2.1", false, firstInstallTime = 2000L, apkSizeBytes = 50_000_000L),
            permissions = listOf(
                PermissionEntry("android.permission.CAMERA", "Camera", "DANGEROUS"),
                PermissionEntry("android.permission.RECORD_AUDIO", "Mic", "DANGEROUS"),
                PermissionEntry("android.permission.SYSTEM_ALERT_WINDOW", "Overlay", "SPECIAL")
            )
        )
        val app3 = AppPermissionInfo(
            app = AppInfo("Gamma Tools", "com.gamma.tools", "0.5", true, firstInstallTime = 500L, apkSizeBytes = 5_000_000L),
            permissions = emptyList()
        )

        val apps = listOf(app1, app2, app3)

        // 1. Sort by RISK (highest dangerous * 2 + special footprint first)
        val sortedByRisk = apps.sortedByDescending { it.dangerousPermissions.size * 2 + it.specialPermissions.size }
        assertEquals("com.beta.chat", sortedByRisk[0].app.packageName)
        assertEquals("com.alpha.scan", sortedByRisk[1].app.packageName)
        assertEquals("com.gamma.tools", sortedByRisk[2].app.packageName)

        // 2. Sort by NAME (A-Z)
        val sortedByName = apps.sortedBy { it.app.name.lowercase(Locale.getDefault()) }
        assertEquals("Alpha Scanner", sortedByName[0].app.name)
        assertEquals("Beta Messenger", sortedByName[1].app.name)
        assertEquals("Gamma Tools", sortedByName[2].app.name)

        // 3. Sort by INSTALL_DATE (newest first)
        val sortedByDate = apps.sortedByDescending { it.app.firstInstallTime }
        assertEquals(2000L, sortedByDate[0].app.firstInstallTime)
        assertEquals(1000L, sortedByDate[1].app.firstInstallTime)
        assertEquals(500L, sortedByDate[2].app.firstInstallTime)

        // 4. Sort by SIZE (largest APK first)
        val sortedBySize = apps.sortedByDescending { it.app.apkSizeBytes }
        assertEquals(50_000_000L, sortedBySize[0].app.apkSizeBytes)
        assertEquals(15_000_000L, sortedBySize[1].app.apkSizeBytes)
        assertEquals(5_000_000L, sortedBySize[2].app.apkSizeBytes)
    }

    @Test
    fun testPerAppScanDiffCalculation() {
        val olderFindings = listOf(
            VulnerabilityFinding(
                name = "Insecure SharedPreferences",
                severity = RiskLevel.HIGH,
                affectedComponent = "com.example.Prefs",
                evidence = "World readable",
                impact = "Data exposure",
                recommendation = "Encrypt"
            ),
            VulnerabilityFinding(
                name = "Cleartext HTTP Traffic",
                severity = RiskLevel.MEDIUM,
                affectedComponent = "com.example.Network",
                evidence = "usesCleartextTraffic=true",
                impact = "MitM",
                recommendation = "Use HTTPS"
            )
        )

        val newerFindings = listOf(
            VulnerabilityFinding(
                name = "Insecure SharedPreferences",
                severity = RiskLevel.HIGH,
                affectedComponent = "com.example.Prefs",
                evidence = "World readable",
                impact = "Data exposure",
                recommendation = "Encrypt"
            ),
            VulnerabilityFinding(
                name = "Exported Activity Injection",
                severity = RiskLevel.CRITICAL,
                affectedComponent = "com.example.AuthActivity",
                evidence = "exported=true without permission",
                impact = "Hijack",
                recommendation = "Set exported false"
            )
        )

        val diff = calculateScanDiff(
            newerScore = 80,
            olderScore = 55,
            newerFindings = newerFindings,
            olderFindings = olderFindings
        )

        assertEquals(25, diff.scoreDelta)
        assertEquals(1, diff.newFindings.size)
        assertEquals("Exported Activity Injection", diff.newFindings[0].name)
        assertEquals(1, diff.resolvedFindings.size)
        assertEquals("Cleartext HTTP Traffic", diff.resolvedFindings[0].name)
    }
}
