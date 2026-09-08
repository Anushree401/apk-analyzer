package com.example.android_manager.data.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import com.example.android_manager.model.AppInfo

data class PermissionEntry(
    val name: String,
    val description: String,
    val category: String
)

data class AppPermissionInfo(
    val app: AppInfo,
    val permissions: List<PermissionEntry>
) {
    val dangerousPermissions: List<PermissionEntry>
        get() = permissions.filter { it.category == "DANGEROUS" }

    val specialPermissions: List<PermissionEntry>
        get() = permissions.filter { it.category == "SPECIAL" }
}

class PermissionRepository(
    private val context: Context
) {

    private val packageManager = context.packageManager

    private val specialPermissions = setOf(
        Manifest.permission.SYSTEM_ALERT_WINDOW,
        Manifest.permission.WRITE_SETTINGS,
        Manifest.permission.REQUEST_INSTALL_PACKAGES,
        Manifest.permission.PACKAGE_USAGE_STATS,
        Manifest.permission.ACCESS_NOTIFICATION_POLICY,
        Manifest.permission.MANAGE_EXTERNAL_STORAGE
    )

    fun getAppPermissions(
        apps: List<AppInfo>
    ): List<AppPermissionInfo> {

        return apps.map { app ->

            val permissions = try {

                val packageInfo = packageManager.getPackageInfo(
                    app.packageName,
                    PackageManager.GET_PERMISSIONS
                )

                packageInfo.requestedPermissions
                    ?.mapNotNull { permissionName ->

                        createPermissionEntry(permissionName)
                    }
                    ?: emptyList()

            } catch (e: Exception) {
                emptyList()
            }

            AppPermissionInfo(
                app = app,
                permissions = permissions
            )
        }
    }

    private fun createPermissionEntry(
        permissionName: String
    ): PermissionEntry {

        val category = classifyPermission(permissionName)

        val description = try {

            val permissionInfo =
                packageManager.getPermissionInfo(
                    permissionName,
                    0
                )

            permissionInfo.loadDescription(packageManager)
                ?.toString()
                ?: "No description available."

        } catch (e: Exception) {

            "No description available."
        }

        return PermissionEntry(
            name = permissionName.substringAfterLast("."),
            description = description,
            category = category
        )
    }

    private fun classifyPermission(
        permissionName: String
    ): String {

        if (permissionName in specialPermissions) {
            return "SPECIAL"
        }

        return try {

            val permissionInfo =
                packageManager.getPermissionInfo(
                    permissionName,
                    0
                )

            val baseProtection =
                permissionInfo.protectionLevel and
                        PermissionInfo.PROTECTION_MASK_BASE

            when (baseProtection) {

                PermissionInfo.PROTECTION_DANGEROUS ->
                    "DANGEROUS"

                PermissionInfo.PROTECTION_NORMAL ->
                    "NORMAL"

                else ->
                    "OTHER"
            }

        } catch (e: Exception) {

            "OTHER"
        }
    }
}