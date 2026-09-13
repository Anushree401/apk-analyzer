package com.example.android_manager.data.process

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
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
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE)
                    as UsageStatsManager

        val endTime = System.currentTimeMillis()

        // Look back 1 hour so the list contains recently used apps.
        val startTime = endTime - (60 * 60 * 1000)

        val usageEvents =
            usageStatsManager.queryEvents(startTime, endTime)

        val lastEvents = mutableMapOf<String, Long>()

        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {

            usageEvents.getNextEvent(event)

            if (
                event.eventType ==
                UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType ==
                UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastEvents[event.packageName] =
                    event.timeStamp
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
}