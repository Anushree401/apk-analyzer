package com.example.android_manager.data.dashboard

import android.content.Context

data class DashboardCachedData(
    val totalApps: Int?,
    val lastUpdated: Long?
)

class DashboardCacheRepository(
    context: Context
) {

    private val preferences =
        context.getSharedPreferences(
            "apvm_dashboard_cache",
            Context.MODE_PRIVATE
        )

    fun getCachedData(): DashboardCachedData {

        val hasApps =
            preferences.contains(KEY_TOTAL_APPS)

        return DashboardCachedData(
            totalApps =
                if (hasApps) {
                    preferences.getInt(
                        KEY_TOTAL_APPS,
                        0
                    )
                } else {
                    null
                },

            lastUpdated =
                if (
                    preferences.contains(KEY_LAST_UPDATED)
                ) {
                    preferences.getLong(
                        KEY_LAST_UPDATED,
                        0L
                    )
                } else {
                    null
                }
        )
    }

    fun saveTotalApps(
        totalApps: Int
    ) {

        preferences.edit()
            .putInt(
                KEY_TOTAL_APPS,
                totalApps
            )
            .putLong(
                KEY_LAST_UPDATED,
                System.currentTimeMillis()
            )
            .apply()
    }

    companion object {

        private const val KEY_TOTAL_APPS =
            "total_apps"

        private const val KEY_LAST_UPDATED =
            "last_updated"
    }
}