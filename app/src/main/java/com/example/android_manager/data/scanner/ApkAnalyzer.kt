package com.example.android_manager.data.scanner

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ProviderInfo
import android.content.pm.ServiceInfo
import com.example.android_manager.model.ApkInfo
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.VulnerabilityFinding
import java.io.File
import java.util.zip.ZipFile

data class ApkAnalysisResult(
    val apkInfo: ApkInfo,
    val findings: List<VulnerabilityFinding>
)

class ApkAnalyzer(
    private val context: Context
) {

    private val packageManager = context.packageManager

    fun analyze(apkFile: File): ApkAnalysisResult {

        val packageInfo = packageManager.getPackageArchiveInfo(
            apkFile.absolutePath,
            PackageManager.GET_PERMISSIONS or
                    PackageManager.GET_ACTIVITIES or
                    PackageManager.GET_SERVICES or
                    PackageManager.GET_RECEIVERS or
                    PackageManager.GET_PROVIDERS
        ) ?: throw IllegalArgumentException(
            "Unable to read APK information"
        )

        val applicationInfo = packageInfo.applicationInfo
            ?: throw IllegalArgumentException(
                "Unable to read application information"
            )

        applicationInfo.sourceDir = apkFile.absolutePath
        applicationInfo.publicSourceDir = apkFile.absolutePath

        val apkInfo = ApkInfo(
            packageName = packageInfo.packageName,
            versionName = packageInfo.versionName,
            versionCode = getVersionCode(packageInfo),
            minSdk = applicationInfo.minSdkVersion,
            targetSdk = applicationInfo.targetSdkVersion,
            appName = applicationInfo.loadLabel(packageManager).toString()
        )

        val findings = mutableListOf<VulnerabilityFinding>()

        analyzePermissions(
            packageInfo,
            findings
        )

        analyzeActivities(
            packageInfo,
            findings
        )

        analyzeServices(
            packageInfo,
            findings
        )

        analyzeReceivers(
            packageInfo,
            findings
        )

        analyzeProviders(
            packageInfo,
            findings
        )

        analyzeDexFiles(
            apkFile,
            findings
        )

        return ApkAnalysisResult(
            apkInfo = apkInfo,
            findings = findings
        )
    }

    private fun getVersionCode(
        packageInfo: PackageInfo
    ): Long {

        return if (android.os.Build.VERSION.SDK_INT >= 28) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun analyzePermissions(
        packageInfo: PackageInfo,
        findings: MutableList<VulnerabilityFinding>
    ) {

        val permissions = packageInfo.requestedPermissions
            ?: return

        val dangerousPermissions = permissions.filter {
            try {
                val permissionInfo =
                    packageManager.getPermissionInfo(
                        it,
                        0
                    )

                val protection =
                    permissionInfo.protectionLevel and
                            android.content.pm.PermissionInfo.PROTECTION_MASK_BASE

                protection ==
                        android.content.pm.PermissionInfo.PROTECTION_DANGEROUS

            } catch (e: Exception) {
                false
            }
        }

        if (dangerousPermissions.isNotEmpty()) {

            findings.add(
                VulnerabilityFinding(
                    name = "Dangerous Permissions Requested",
                    severity = RiskLevel.MEDIUM,
                    affectedComponent = packageInfo.packageName,
                    evidence = dangerousPermissions.joinToString(
                        separator = "\n"
                    ),
                    impact = "The application requests permissions that provide access to sensitive device resources.",
                    recommendation = "Review whether each dangerous permission is required for the application's intended functionality."
                )
            )
        }
    }

    private fun analyzeActivities(
        packageInfo: PackageInfo,
        findings: MutableList<VulnerabilityFinding>
    ) {

        packageInfo.activities?.forEach { activity ->

            if (
                activity.exported &&
                activity.permission.isNullOrBlank()
            ) {

                findings.add(
                    VulnerabilityFinding(
                        name = "Exported Activity Without Permission",
                        severity = RiskLevel.HIGH,
                        affectedComponent = activity.name,
                        evidence = "Activity is exported and does not declare a permission.",
                        impact = "Other applications may be able to invoke this activity directly.",
                        recommendation = "Restrict the activity with an appropriate permission or set exported to false when external access is not required."
                    )
                )
            }
        }
    }

    private fun analyzeServices(
        packageInfo: PackageInfo,
        findings: MutableList<VulnerabilityFinding>
    ) {

        packageInfo.services?.forEach { service ->

            if (
                service.exported &&
                service.permission.isNullOrBlank()
            ) {

                findings.add(
                    VulnerabilityFinding(
                        name = "Exported Service Without Permission",
                        severity = RiskLevel.HIGH,
                        affectedComponent = service.name,
                        evidence = "Service is exported and does not declare a permission.",
                        impact = "Other applications may be able to interact with the service.",
                        recommendation = "Set exported to false when external access is unnecessary or protect the service with an appropriate permission."
                    )
                )
            }
        }
    }

    private fun analyzeReceivers(
        packageInfo: PackageInfo,
        findings: MutableList<VulnerabilityFinding>
    ) {

        packageInfo.receivers?.forEach { receiver ->

            if (
                receiver.exported &&
                receiver.permission.isNullOrBlank()
            ) {

                findings.add(
                    VulnerabilityFinding(
                        name = "Exported Broadcast Receiver Without Permission",
                        severity = RiskLevel.HIGH,
                        affectedComponent = receiver.name,
                        evidence = "Broadcast receiver is exported and does not declare a permission.",
                        impact = "Other applications may be able to send broadcasts directly to the receiver.",
                        recommendation = "Restrict the receiver with a permission or disable external access when it is not required."
                    )
                )
            }
        }
    }

    private fun analyzeProviders(
        packageInfo: PackageInfo,
        findings: MutableList<VulnerabilityFinding>
    ) {

        packageInfo.providers?.forEach { provider ->

            if (
                provider.exported &&
                provider.readPermission.isNullOrBlank() &&
                provider.writePermission.isNullOrBlank()
            ) {

                findings.add(
                    VulnerabilityFinding(
                        name = "Exported Content Provider Without Permission",
                        severity = RiskLevel.CRITICAL,
                        affectedComponent = provider.name,
                        evidence = "Content provider is exported without read or write permission protection.",
                        impact = "Other applications may potentially access or modify provider data.",
                        recommendation = "Restrict provider access using appropriate read/write permissions or disable external exposure."
                    )
                )
            }
        }
    }

    private fun analyzeDexFiles(
        apkFile: File,
        findings: MutableList<VulnerabilityFinding>
    ) {

        var webViewDetected = false
        var javascriptEnabled = false
        var javascriptInterfaceDetected = false

        try {

            ZipFile(apkFile).use { zip ->

                zip.entries().asSequence()
                    .filter {
                        it.name.endsWith(".dex")
                    }
                    .forEach { entry ->

                        val bytes = zip.getInputStream(entry)
                            .use { it.readBytes() }

                        val content =
                            String(
                                bytes,
                                Charsets.ISO_8859_1
                            )

                        if (
                            content.contains(
                                "android/webkit/WebView"
                            )
                        ) {
                            webViewDetected = true
                        }

                        if (
                            content.contains(
                                "setJavaScriptEnabled"
                            )
                        ) {
                            javascriptEnabled = true
                        }

                        if (
                            content.contains(
                                "addJavascriptInterface"
                            )
                        ) {
                            javascriptInterfaceDetected = true
                        }
                    }
            }

        } catch (e: Exception) {

            findings.add(
                VulnerabilityFinding(
                    name = "DEX Analysis Incomplete",
                    severity = RiskLevel.LOW,
                    affectedComponent = null,
                    evidence = e.message ?: "Unable to inspect DEX files.",
                    impact = "Some static code indicators may not have been detected.",
                    recommendation = "Perform deeper static analysis using a dedicated APK analysis pipeline."
                )
            )

            return
        }

        if (webViewDetected) {

            findings.add(
                VulnerabilityFinding(
                    name = "WebView Detected",
                    severity = RiskLevel.MEDIUM,
                    affectedComponent = null,
                    evidence = "android.webkit.WebView reference detected in DEX files.",
                    impact = "WebView introduces an additional attack surface and should be securely configured.",
                    recommendation = "Review WebView configuration, URL handling, JavaScript settings and local file access."
                )
            )
        }

        if (javascriptEnabled) {

            findings.add(
                VulnerabilityFinding(
                    name = "JavaScript Enabled in WebView",
                    severity = RiskLevel.HIGH,
                    affectedComponent = null,
                    evidence = "setJavaScriptEnabled reference detected in DEX files.",
                    impact = "JavaScript increases the WebView attack surface, particularly when untrusted content is loaded.",
                    recommendation = "Disable JavaScript unless it is required and ensure untrusted content cannot execute privileged operations."
                )
            )
        }

        if (javascriptInterfaceDetected) {

            findings.add(
                VulnerabilityFinding(
                    name = "JavaScript Interface Detected",
                    severity = RiskLevel.HIGH,
                    affectedComponent = null,
                    evidence = "addJavascriptInterface reference detected in DEX files.",
                    impact = "JavaScript interfaces can expose native application functionality to WebView content.",
                    recommendation = "Review all JavaScript interfaces and ensure only safe, explicitly intended methods are exposed."
                )
            )
        }
    }
}