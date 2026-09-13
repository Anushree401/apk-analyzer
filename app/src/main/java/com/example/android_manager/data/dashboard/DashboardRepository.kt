package com.example.android_manager.data.dashboard

import android.content.Context
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.process.ProcessRepository

data class DashboardData(
    val totalApps: Int,
    val totalProcesses: Int
)

class DashboardRepository(context: Context) {

    private val appRepository = AppRepository(context)
    private val processRepository = ProcessRepository(context)
    private val cacheRepository = DashboardCacheRepository(context)

    fun getCachedData(): DashboardCachedData =
        cacheRepository.getCachedData()

    fun refresh(): DashboardData {

        val apps = appRepository.getInstalledApps()
        val totalApps = apps.size

        cacheRepository.saveTotalApps(totalApps)

        val foreground =
            processRepository.getCurrentForegroundApp()

        val background =
            processRepository.getBackgroundProcesses()

        val activePackages =
            buildSet {

                foreground?.packageName?.let {
                    add(it)
                }

                background.forEach {
                    add(it.packageName)
                }
            }

        val totalActiveApps =
            activePackages.size

        return DashboardData(
            totalApps = totalApps,
            totalProcesses = totalActiveApps
        )
    }
}