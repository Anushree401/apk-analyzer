package com.example.android_manager.data.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import com.example.android_manager.model.AppInfo

import android.content.pm.PackageManager
import android.os.Build
import java.io.File
import java.security.MessageDigest

class AppRepository(
    private val context: Context
) {

    fun getInstalledApps(): List<AppInfo> {
        val packageManager = context.packageManager

        return packageManager
            .getInstalledApplications(0)
            .map { applicationInfo ->
                val packageName = applicationInfo.packageName
                val isSystem = (applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0

                val packageInfo = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                    } else {
                        @Suppress("DEPRECATION")
                        packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                    }
                } catch (_: Exception) {
                    try {
                        packageManager.getPackageInfo(packageName, 0)
                    } catch (_: Exception) {
                        null
                    }
                }

                val installSource = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        val sourceInfo = packageManager.getInstallSourceInfo(packageName)
                        val installer = sourceInfo.installingPackageName ?: sourceInfo.initiatingPackageName
                        when (installer) {
                            "com.android.vending" -> "Google Play"
                            "com.google.android.packageinstaller" -> "Package Installer"
                            null -> if (isSystem) "System Image" else "Sideloaded"
                            else -> installer
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val installer = packageManager.getInstallerPackageName(packageName)
                        when (installer) {
                            "com.android.vending" -> "Google Play"
                            null -> if (isSystem) "System Image" else "Sideloaded"
                            else -> installer
                        }
                    }
                } catch (_: Exception) {
                    if (isSystem) "System Image" else "Sideloaded"
                }

                val apkSizeBytes = try {
                    File(applicationInfo.sourceDir).length()
                } catch (_: Exception) {
                    0L
                }

                val certSha256 = try {
                    val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo?.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
                            ?: packageInfo?.signingInfo?.signingCertificateHistory?.firstOrNull()?.toByteArray()
                    } else {
                        @Suppress("DEPRECATION")
                        packageInfo?.signatures?.firstOrNull()?.toByteArray()
                    }
                    if (certBytes != null) {
                        val digest = MessageDigest.getInstance("SHA-256").digest(certBytes)
                        digest.joinToString(":") { "%02X".format(it) }
                    } else null
                } catch (_: Exception) {
                    null
                }

                AppInfo(
                    name = applicationInfo.loadLabel(packageManager).toString(),
                    packageName = packageName,
                    versionName = packageInfo?.versionName,
                    isSystemApp = isSystem,
                    installSource = installSource,
                    firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
                    lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
                    apkSizeBytes = apkSizeBytes,
                    certificateSha256 = certSha256
                )
            }
            .sortedBy { it.name.lowercase() }
    }
}