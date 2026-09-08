package com.example.android_manager.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.android_manager.model.AppInfo

class AppRepository(
    private val context: Context
) {

    fun getInstalledApps(): List<AppInfo> {

        val packageManager = context.packageManager

        return packageManager
            .getInstalledApplications(0)
            .map { applicationInfo ->

                val packageName = applicationInfo.packageName

                val versionName = try {
                    packageManager
                        .getPackageInfo(packageName, 0)
                        .versionName
                } catch (e: Exception) {
                    null
                }

                AppInfo(
                    name = applicationInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    versionName = versionName,
                    isSystemApp =
                        (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}