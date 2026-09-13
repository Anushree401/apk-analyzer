package com.example.android_manager.data.process

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.ApplicationExitInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.example.android_manager.model.ProcessExitInfo
import com.example.android_manager.model.ProcessInfo
import com.example.android_manager.model.ProcessState

class ProcessRepository(
    private val context: Context
) {

    private val packageManager = context.packageManager

    fun hasUsageAccess(): Boolean {

        val appOpsManager =
            context.getSystemService(Context.APP_OPS_SERVICE)
                    as AppOpsManager

        return appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun getCurrentForegroundApp(): ProcessInfo? {

        if (!hasUsageAccess()) {
            return null
        }

        val packageName =
            getLatestForegroundPackage()
                ?: return null

        if (packageName == context.packageName) {
            return null
        }

        return createProcessInfo(
            packageName = packageName,
            state = ProcessState.FOREGROUND,
            lastActiveTime = System.currentTimeMillis()
        )
    }

    fun getBackgroundProcesses(): List<ProcessInfo> {

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE)
                    as ActivityManager

        val currentForegroundPackage =
            getLatestForegroundPackage()

        val runningProcesses =
            activityManager.runningAppProcesses
                ?: return emptyList()

        return runningProcesses
            .filter { process ->

                process.pid != Process.myPid() &&

                        process.pkgList.none {
                            it == context.packageName
                        }
            }
            .flatMap { process ->

                process.pkgList.mapNotNull { packageName ->

                    if (packageName == context.packageName) {
                        return@mapNotNull null
                    }

                    if (packageName == currentForegroundPackage) {
                        return@mapNotNull null
                    }

                    createProcessInfo(
                        packageName = packageName,
                        state = ProcessState.BACKGROUND,
                        pid = process.pid,
                        importance = process.importance
                    )
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.processName.lowercase() }
    }

    fun getRecentlyActiveApps(): List<ProcessInfo> {

        if (!hasUsageAccess()) {
            return emptyList()
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val endTime = System.currentTimeMillis()

        // Last 30 minutes.
        val startTime =
            endTime - (30 * 60 * 1000)

        val usageEvents =
            usageStatsManager.queryEvents(
                startTime,
                endTime
            )

        val latestActivity =
            mutableMapOf<String, Long>()

        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event)

            if (
                event.eventType ==
                UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType ==
                UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {

                if (
                    event.packageName != context.packageName
                ) {

                    val previous =
                        latestActivity[event.packageName]
                            ?: 0L

                    if (event.timeStamp > previous) {
                        latestActivity[event.packageName] =
                            event.timeStamp
                    }
                }
            }
        }

        val backgroundPackages =
            getBackgroundProcesses()
                .map { it.packageName }
                .toSet()

        val currentForeground =
            getLatestForegroundPackage()

        return latestActivity
            .filter { (packageName, _) ->

                packageName != currentForeground &&
                        packageName !in backgroundPackages
            }
            .mapNotNull { (packageName, timestamp) ->

                createProcessInfo(
                    packageName = packageName,
                    state = ProcessState.RECENTLY_ACTIVE,
                    lastActiveTime = timestamp
                )
            }
            .sortedByDescending {
                it.lastActiveTime
            }
    }

    fun getProcessOverview(): List<ProcessInfo> {

        val result =
            mutableListOf<ProcessInfo>()

        val foreground =
            getCurrentForegroundApp()

        if (foreground != null) {
            result.add(foreground)
        }

        result.addAll(
            getBackgroundProcesses()
        )

        result.addAll(
            getRecentlyActiveApps()
        )

        return result
            .distinctBy { it.packageName }
    }

    fun getRunningProcessCount(): Int {

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE)
                    as ActivityManager

        val processes =
            activityManager.runningAppProcesses
                ?: return 0

        return processes
            .filter { process ->

                process.pid != Process.myPid() &&

                        process.pkgList.none {
                            it == context.packageName
                        }
            }
            .distinctBy { it.pid }
            .size
    }

    private fun getLatestForegroundPackage(): String? {

        if (!hasUsageAccess()) {
            return null
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val endTime =
            System.currentTimeMillis()

        val startTime =
            endTime - (10 * 60 * 1000)

        val usageEvents =
            usageStatsManager.queryEvents(
                startTime,
                endTime
            )

        var latestPackage: String? = null
        var latestTimestamp = 0L

        val event =
            UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event)

            if (
                event.eventType ==
                UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType ==
                UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {

                if (
                    event.timeStamp >
                    latestTimestamp
                ) {

                    latestTimestamp =
                        event.timeStamp

                    latestPackage =
                        event.packageName
                }
            }
        }

        return latestPackage
    }

    private fun createProcessInfo(
        packageName: String,
        state: ProcessState,
        pid: Int = -1,
        importance: Int = 0,
        lastActiveTime: Long = 0L
    ): ProcessInfo? {

        val applicationInfo = try {

            packageManager.getApplicationInfo(
                packageName,
                0
            )

        } catch (_: Exception) {

            return null
        }

        val appName =
            applicationInfo
                .loadLabel(packageManager)
                .toString()

        return ProcessInfo(
            processName = appName,
            packageName = packageName,
            pid = pid,
            memoryMb = 0,
            importance = importance,
            isForeground =
                state == ProcessState.FOREGROUND,
            state = state,
            lastActiveTime = lastActiveTime
        )
    }

    fun getProcessExitHistory(): List<ProcessExitInfo> {

        if (
            Build.VERSION.SDK_INT <
            Build.VERSION_CODES.R
        ) {
            return emptyList()
        }

        val activityManager =
            context.getSystemService(
                Context.ACTIVITY_SERVICE
            ) as ActivityManager

        val packages =
            packageManager
                .getInstalledApplications(0)

        val result =
            mutableListOf<ProcessExitInfo>()

        for (app in packages) {

            try {

                val exits =
                    activityManager
                        .getHistoricalProcessExitReasons(
                            app.packageName,
                            0,
                            5
                        )

                for (exit in exits) {

                    result.add(
                        ProcessExitInfo(
                            packageName =
                                app.packageName,
                            processName =
                                exit.processName
                                    ?: app.packageName,
                            timestamp =
                                exit.timestamp,
                            reason =
                                getExitReasonName(
                                    exit.reason
                                ),
                            description =
                                exit.description
                        )
                    )
                }

            } catch (_: Exception) {
            }
        }

        return result
            .sortedByDescending {
                it.timestamp
            }
            .take(50)
    }

    private fun getExitReasonName(
        reason: Int
    ): String {

        return when (reason) {

            ApplicationExitInfo.REASON_ANR ->
                "ANR"

            ApplicationExitInfo.REASON_CRASH ->
                "CRASH"

            ApplicationExitInfo.REASON_CRASH_NATIVE ->
                "NATIVE CRASH"

            ApplicationExitInfo.REASON_DEPENDENCY_DIED ->
                "DEPENDENCY DIED"

            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE ->
                "EXCESSIVE RESOURCE USAGE"

            ApplicationExitInfo.REASON_EXIT_SELF ->
                "APP EXITED"

            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE ->
                "INITIALIZATION FAILURE"

            ApplicationExitInfo.REASON_LOW_MEMORY ->
                "LOW MEMORY"

            ApplicationExitInfo.REASON_PERMISSION_CHANGE ->
                "PERMISSION CHANGE"

            ApplicationExitInfo.REASON_SIGNALED ->
                "SIGNALED"

            ApplicationExitInfo.REASON_USER_REQUESTED ->
                "USER REQUESTED"

            ApplicationExitInfo.REASON_UNKNOWN ->
                "UNKNOWN"

            else ->
                "UNKNOWN"
        }
    }
}