package com.example.android_manager.data.dashboard

import android.content.Context
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.process.ProcessRepository

data class DashboardData(
    val totalApps: Int,
    val totalProcesses: Int
)

class DashboardRepository(
    context: Context
) {

    private val appRepository =
        AppRepository(context)

    private val processRepository =
        ProcessRepository(context)

    private val cacheRepository =
        DashboardCacheRepository(context)

    fun getCachedData(): DashboardCachedData {
        return cacheRepository.getCachedData()
    }

    fun refresh(): DashboardData {

        /*
         * Apps are recalculated and then cached.
         */
        val apps =
            appRepository.getInstalledApps()

        val totalApps =
            apps.size

        cacheRepository.saveTotalApps(
            totalApps = totalApps
        )

        val processes =
            if (
                processRepository.hasUsageAccess()
            ) {
                processRepository
                    .getRecentlyActiveApps()
            } else {
                emptyList()
            }

        return DashboardData(
            totalApps = totalApps,
            totalProcesses = processes.size
        )
    }
}