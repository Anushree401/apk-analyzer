
package com.example.android_manager.data.dashboard

import android.content.Context
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.data.process.ProcessRepository
import com.example.android_manager.model.ScanHistoryEntry

data class DashboardData(
    val totalApps: Int,
    val totalProcesses: Int,
    val dangerousApps: Int,
    val vulnerabilityCount: Int,
    val recentScans: List<ScanHistoryEntry>
)

class DashboardRepository(context: Context) {

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

        return DashboardData(
            totalApps = totalApps,
            totalProcesses = activePackages.size,
            dangerousApps = dangerousApps,
            vulnerabilityCount = vulnerabilityCount,
            recentScans = history.take(5)
        )
    }
}
