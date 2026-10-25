
package com.example.android_manager.data.dashboard

import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.data.permissions.AppPermissionInfo
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.data.process.ProcessRepository
import com.example.android_manager.model.ScanHistoryEntry
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

data class DeviceSecurityPostureInfo(
    val androidVersion: String,
    val apiLevel: Int,
    val securityPatch: String,
    val isPatchOutdated: Boolean,
    val patchAgeDays: Long?,
    val isOsEol: Boolean,
    val isStorageEncrypted: Boolean,
    val encryptionStatusLabel: String,
    val isRooted: Boolean,
    val isAdbEnabled: Boolean,
    val isInstallUnknownAppsAllowed: Boolean
)

data class DashboardData(
    val totalApps: Int,
    val totalProcesses: Int,
    val dangerousApps: Int,
    val vulnerabilityCount: Int,
    val recentScans: List<ScanHistoryEntry>,
    val devicePosture: DeviceSecurityPostureInfo,
    val topRiskiestApps: List<AppPermissionInfo> = emptyList()
)

class DashboardRepository(private val context: Context) {

    private val appRepository = AppRepository(context)
    private val permissionRepository = PermissionRepository(context)
    private val processRepository = ProcessRepository(context)
    private val cacheRepository = DashboardCacheRepository(context)
    private val historyRepository = ScanHistoryRepository(context)

    fun getCachedData(): DashboardCachedData =
        cacheRepository.getCachedData()

    suspend fun refresh(): DashboardData {
        val apps = appRepository.getInstalledApps()
        val totalApps = apps.size

        // Cache only the installed-app total.
        cacheRepository.saveTotalApps(totalApps)

        val appPermissions =
            permissionRepository.getAppPermissions(apps)

        // Count apps requesting at least one dangerous permission.
        val dangerousApps = appPermissions.count { appInfo ->
            appInfo.dangerousPermissions.isNotEmpty()
        }

        // Best-effort activity visibility, not a complete OS process count.
        val foreground = processRepository.getCurrentForegroundApp()
        val background = processRepository.getBackgroundProcesses()

        val activePackages = buildSet {
            foreground?.packageName?.let(::add)
            background.forEach { add(it.packageName) }
        }

        val history = historyRepository.getAllHistorySnapshot()
            .sortedByDescending { it.timestamp }

        // Keep the newest scan per package so repeated scans do not
        // inflate the dashboard's vulnerability total.
        val latestScansByPackage = history
            .filter { it.packageName.isNotBlank() }
            .distinctBy { it.packageName }

        val vulnerabilityCount = latestScansByPackage.sumOf {
            it.findingsCount
        }

        val topRiskiest = appPermissions
            .filter { !it.app.isSystemApp && (it.dangerousPermissions.isNotEmpty() || it.specialPermissions.isNotEmpty()) }
            .sortedByDescending { it.dangerousPermissions.size * 2 + it.specialPermissions.size }
            .take(4)

        val posture = computeDeviceSecurityPosture()

        return DashboardData(
            totalApps = totalApps,
            totalProcesses = activePackages.size,
            dangerousApps = dangerousApps,
            vulnerabilityCount = vulnerabilityCount,
            recentScans = history.take(5),
            devicePosture = posture,
            topRiskiestApps = topRiskiest
        )
    }

    @Suppress("DEPRECATION")
    private fun computeDeviceSecurityPosture(): DeviceSecurityPostureInfo {
        val androidVersion = Build.VERSION.RELEASE ?: "Unknown"
        val apiLevel = Build.VERSION.SDK_INT
        val isOsEol = apiLevel < 31 // Android 11 and older

        val securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Build.VERSION.SECURITY_PATCH ?: "Unknown"
        } else {
            "Pre-Android 6.0"
        }

        var patchAgeDays: Long? = null
        var isPatchOutdated = false
        if (securityPatch.isNotBlank() && securityPatch != "Unknown") {
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val patchDate = sdf.parse(securityPatch)
                if (patchDate != null) {
                    val diffMs = System.currentTimeMillis() - patchDate.time
                    val days = diffMs / (1000 * 60 * 60 * 24)
                    patchAgeDays = days
                    isPatchOutdated = days > 90
                }
            } catch (_: Exception) {
                // Ignore parsing errors on vendor-custom patch string formats
            }
        }

        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val encStatus = dpm?.storageEncryptionStatus ?: DevicePolicyManager.ENCRYPTION_STATUS_UNSUPPORTED
        val isStorageEncrypted = encStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                encStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
        val encryptionStatusLabel = when (encStatus) {
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE -> "Active (Hardware-backed)"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER -> "Active (Per-user)"
            DevicePolicyManager.ENCRYPTION_STATUS_INACTIVE -> "Inactive (Unencrypted)"
            DevicePolicyManager.ENCRYPTION_STATUS_ACTIVATING -> "Activating"
            else -> "Unsupported / Unknown"
        }

        val isRooted = checkRootHeuristics()

        val isAdbEnabled = try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (_: Exception) {
            false
        }

        val isInstallUnknownAppsAllowed = try {
            @Suppress("DEPRECATION")
            Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
        } catch (_: Exception) {
            false
        }

        return DeviceSecurityPostureInfo(
            androidVersion = androidVersion,
            apiLevel = apiLevel,
            securityPatch = securityPatch,
            isPatchOutdated = isPatchOutdated,
            patchAgeDays = patchAgeDays,
            isOsEol = isOsEol,
            isStorageEncrypted = isStorageEncrypted,
            encryptionStatusLabel = encryptionStatusLabel,
            isRooted = isRooted,
            isAdbEnabled = isAdbEnabled,
            isInstallUnknownAppsAllowed = isInstallUnknownAppsAllowed
        )
    }

    private fun checkRootHeuristics(): Boolean {
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) return true

        val suPaths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        return try {
            suPaths.any { File(it).exists() }
        } catch (_: Exception) {
            false
        }
    }
}

