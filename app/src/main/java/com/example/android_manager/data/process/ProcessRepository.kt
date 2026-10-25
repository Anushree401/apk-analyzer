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
import java.util.Locale

data class DeviceMemoryInfo(
    val totalMemBytes: Long,
    val availMemBytes: Long,
    val lowMemory: Boolean,
    val thresholdBytes: Long
) {
    val usedMemBytes: Long get() = (totalMemBytes - availMemBytes).coerceAtLeast(0L)
    val usedPercentage: Float get() = if (totalMemBytes > 0) usedMemBytes.toFloat() / totalMemBytes else 0f
    val usedMemFormatted: String get() = String.format(Locale.US, "%.1f GB", usedMemBytes / (1024.0 * 1024.0 * 1024.0))
    val totalMemFormatted: String get() = String.format(Locale.US, "%.1f GB", totalMemBytes / (1024.0 * 1024.0 * 1024.0))
}

/**
 * Reads application-usage events, device memory metrics, and limited process telemetry.
 * 100% on-device architecture with zero network dependency.
 */
class ProcessRepository(
    private val context: Context
) {
    private val packageManager = context.packageManager

    fun hasUsageAccess(): Boolean {
        val appOpsManager =
            context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager

        return appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun getDeviceMemoryInfo(): DeviceMemoryInfo {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return DeviceMemoryInfo(
            totalMemBytes = memoryInfo.totalMem,
            availMemBytes = memoryInfo.availMem,
            lowMemory = memoryInfo.lowMemory,
            thresholdBytes = memoryInfo.threshold
        )
    }

    fun getCurrentForegroundApp(): ProcessInfo? {
        if (!hasUsageAccess()) return null

        val event = getLatestForegroundEvent() ?: return null
        if (event.first == context.packageName) return null

        return createProcessInfo(
            packageName = event.first,
            state = ProcessState.FOREGROUND,
            lastActiveTime = event.second
        )
    }

    fun getBackgroundProcesses(): List<ProcessInfo> {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val currentForegroundPackage = getLatestForegroundEvent()?.first
        val runningProcesses = activityManager.runningAppProcesses ?: return emptyList()

        return runningProcesses
            .filter { process ->
                process.pid != Process.myPid() &&
                        process.pkgList.none { it == context.packageName }
            }
            .flatMap { process ->
                process.pkgList.mapNotNull { packageName ->
                    if (
                        packageName == context.packageName ||
                        packageName == currentForegroundPackage
                    ) {
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
            .sortedBy { it.processName.lowercase(LocaleHolder.locale) }
    }

    fun getRecentlyActiveApps(): List<ProcessInfo> {
        if (!hasUsageAccess()) return emptyList()

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val endTime = System.currentTimeMillis()
        val startTime = endTime - RECENT_ACTIVITY_WINDOW_MS
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        val latestActivity = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (isForegroundEvent(event.eventType) && event.packageName != context.packageName) {
                val previous = latestActivity[event.packageName] ?: 0L
                if (event.timeStamp > previous) {
                    latestActivity[event.packageName] = event.timeStamp
                }
            }
        }

        val backgroundPackages = getBackgroundProcesses().map { it.packageName }.toSet()
        val currentForeground = getLatestForegroundEvent()?.first

        return latestActivity
            .filter { (packageName, _) ->
                packageName != currentForeground && packageName !in backgroundPackages
            }
            .mapNotNull { (packageName, timestamp) ->
                createProcessInfo(
                    packageName = packageName,
                    state = ProcessState.RECENTLY_ACTIVE,
                    lastActiveTime = timestamp
                )
            }
            .sortedByDescending { it.lastActiveTime }
    }

    fun getProcessOverview(): List<ProcessInfo> {
        val result = mutableListOf<ProcessInfo>()
        val foreground = getCurrentForegroundApp()
        if (foreground != null) {
            result.add(foreground)
        }
        result.addAll(getBackgroundProcesses())
        result.addAll(getRecentlyActiveApps())
        return result.distinctBy { it.packageName }
    }

    fun getRunningProcessCount(): Int {
        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val processes = activityManager.runningAppProcesses ?: return 0

        return processes
            .filter { process ->
                process.pid != Process.myPid() &&
                        process.pkgList.none { it == context.packageName }
            }
            .distinctBy { it.pid }
            .size
    }

    private fun getLatestForegroundEvent(): Pair<String, Long>? {
        if (!hasUsageAccess()) return null

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val startTime = endTime - FOREGROUND_LOOKBACK_WINDOW_MS
        val usageEvents = usageStatsManager.queryEvents(startTime, endTime)

        var latestEvent: Pair<String, Long>? = null
        val event = UsageEvents.Event()

        while (usageEvents.hasNextEvent()) {
            usageEvents.getNextEvent(event)
            if (isForegroundEvent(event.eventType)) {
                val previousTimestamp = latestEvent?.second ?: 0L
                if (event.timeStamp > previousTimestamp) {
                    latestEvent = event.packageName to event.timeStamp
                }
            }
        }

        return latestEvent
    }

    private fun isForegroundEvent(eventType: Int): Boolean {
        return eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
    }

    private fun createProcessInfo(
        packageName: String,
        state: ProcessState,
        pid: Int = 0,
        importance: Int = 0,
        lastActiveTime: Long = 0L
    ): ProcessInfo? {
        val applicationInfo = try {
            packageManager.getApplicationInfo(packageName, 0)
        } catch (_: Exception) {
            return null
        }

        val appName = applicationInfo.loadLabel(packageManager).toString()

        return ProcessInfo(
            processName = appName,
            packageName = packageName,
            pid = pid,
            memoryMb = 0,
            importance = importance,
            isForeground = state == ProcessState.FOREGROUND,
            state = state,
            lastActiveTime = lastActiveTime
        )
    }

    fun getProcessExitHistory(): List<ProcessExitInfo> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val packages = packageManager.getInstalledApplications(0)
        val result = mutableListOf<ProcessExitInfo>()

        for (app in packages) {
            try {
                val exits = activityManager.getHistoricalProcessExitReasons(
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
                // Some system packages do not expose exit reasons. Continue.
            }
        }

        return result
            .sortedByDescending { it.timestamp }
            .take(MAX_EXIT_EVENTS)
    }

    private fun getExitReasonName(reason: Int): String {
        return when (reason) {
            ApplicationExitInfo.REASON_ANR -> "ANR"
            ApplicationExitInfo.REASON_CRASH -> "CRASH"
            ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE CRASH"
            ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "DEPENDENCY DIED"
            ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE RESOURCE USAGE"
            ApplicationExitInfo.REASON_EXIT_SELF -> "APP EXITED"
            ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "INITIALIZATION FAILURE"
            ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW MEMORY"
            ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "PERMISSION CHANGE"
            ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
            ApplicationExitInfo.REASON_USER_REQUESTED -> "USER REQUESTED"
            ApplicationExitInfo.REASON_UNKNOWN -> "UNKNOWN"
            else -> "UNKNOWN"
        }
    }

    private object LocaleHolder {
        val locale: Locale = Locale.getDefault()
    }

    private companion object {
        const val RECENT_ACTIVITY_WINDOW_MS = 30 * 60 * 1000L
        const val FOREGROUND_LOOKBACK_WINDOW_MS = 10 * 60 * 1000L
        const val MAX_EXIT_EVENTS = 50
    }
}