package com.example.android_manager.data.process

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.ApplicationExitInfo
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import com.example.android_manager.model.ProcessExitInfo
import com.example.android_manager.model.ProcessInfo

class ProcessRepository(
    private val context: Context
) {

    fun hasUsageAccess(): Boolean {
        val appOpsManager =
            context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        return appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun getRecentlyActiveApps(): List<ProcessInfo> {

        if (!hasUsageAccess()) {
            return emptyList()
        }

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - (60 * 60 * 1000)

        val usageEvents =
            usageStatsManager.queryEvents(startTime, endTime)

        val lastEvents = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event)

            if (
                event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastEvents[event.packageName] = event.timeStamp
            }
        }

        return lastEvents.entries
            .sortedByDescending { it.value }
            .mapIndexed { index, entry ->

                ProcessInfo(
                    processName = entry.key,
                    pid = -1,
                    memoryMb = 0,
                    importance = index,
                    isForeground = index == 0
                )
            }
    }

    fun getProcessExitHistory(): List<ProcessExitInfo> {

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE)
                    as ActivityManager

        val packages =
            context.packageManager.getInstalledApplications(0)

        val result = mutableListOf<ProcessExitInfo>()

        for (app in packages) {

            try {

                val exits =
                    activityManager.getHistoricalProcessExitReasons(
                        app.packageName,
                        0,
                        5
                    )

                for (exit in exits) {

                    result.add(
                        ProcessExitInfo(
                            packageName = app.packageName,
                            processName = exit.processName ?: app.packageName,
                            timestamp = exit.timestamp,
                            reason = getExitReasonName(exit.reason),
                            description = exit.description
                        )
                    )
                }

            } catch (_: Exception) {
                // Some packages may not expose historical information.
            }
        }

        return result
            .sortedByDescending { it.timestamp }
            .take(50)
    }

    private fun getExitReasonName(reason: Int): String {

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

            ApplicationExitInfo.REASON_OTHER ->
                "OTHER"

            ApplicationExitInfo.REASON_PERMISSION_CHANGE ->
                "PERMISSION CHANGE"

            ApplicationExitInfo.REASON_SIGNALED ->
                "SIGNALED"

            ApplicationExitInfo.REASON_UNKNOWN ->
                "UNKNOWN"

            ApplicationExitInfo.REASON_USER_REQUESTED ->
                "USER REQUESTED"

            else ->
                "UNKNOWN"
        }
    }
}