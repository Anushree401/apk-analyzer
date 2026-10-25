
package com.example.android_manager.data.report

import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.data.permissions.PermissionEntry
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.data.risk.RiskAssessmentRepository
import com.example.android_manager.data.scanner.ApkAnalysisResult
import com.example.android_manager.data.scanner.ApkScannerRepository
import com.example.android_manager.model.AppInfo
import com.example.android_manager.model.ComponentExposureBreakdown
import com.example.android_manager.model.ReportAppMetadata
import com.example.android_manager.model.ReportExecutiveSummary
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.model.RiskCategory
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.RiskRecommendation
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.model.ScanType
import com.example.android_manager.model.SecurityReport
import com.example.android_manager.model.VulnerabilityFinding

/**
 * Generates and reconstructs security reports for APK scans,
 * installed applications, and historical scans.
 */
class SecurityReportRepository(
    private val context: Context
) {
    private val appRepo = AppRepository(context)
    private val permRepo = PermissionRepository(context)
    private val riskRepo = RiskAssessmentRepository(context)
    private val scannerRepo = ApkScannerRepository(context)
    private val historyRepo = ScanHistoryRepository(context)

    companion object {
        @Volatile
        private var activeReport: SecurityReport? = null

        fun setActiveReport(report: SecurityReport) {
            activeReport = report
        }

        fun getActiveReport(): SecurityReport? = activeReport

        fun clearActiveReport() {
            activeReport = null
        }

        /**
         * Resolves a targeted, actionable mitigation for a security factor or finding.
         * Eliminates generic fallback messages.
         */
        fun resolveRecommendation(
            factorId: String,
            title: String,
            category: RiskCategory,
            evidence: String? = null
        ): String {
            // 1. Permission combinations
            if (factorId.startsWith("COMB_") || title.startsWith("Risky Combo:", ignoreCase = true)) {
                val comboName = title.removePrefix("Risky Combo:").trim()
                return when {
                    comboName.contains("Camera & Audio", ignoreCase = true) ||
                    comboName.contains("Camera", ignoreCase = true) && comboName.contains("Audio", ignoreCase = true) ->
                        "Audit whether both camera and microphone access are required simultaneously. Use system intents (MediaStore.ACTION_IMAGE_CAPTURE) for on-demand media capture to avoid holding persistent hardware permissions."

                    comboName.contains("Audio", ignoreCase = true) ->
                        "Review the necessity of microphone access alongside internet transmission. Request RECORD_AUDIO dynamically with explicit runtime rationale and visual recording indicators."

                    comboName.contains("Package Installation", ignoreCase = true) ||
                    comboName.contains("Trojan", ignoreCase = true) ||
                    comboName.contains("Dropper", ignoreCase = true) ->
                        "Remove android.permission.REQUEST_INSTALL_PACKAGES. Distribute application updates through verified app stores (Google Play) rather than self-updating unverified binaries."

                    comboName.contains("SMS", ignoreCase = true) ->
                        "Replace SMS reading permissions with the Google SMS Retriever API or SMS User Consent API to verify phone numbers without accessing user messages."

                    comboName.contains("Geolocation", ignoreCase = true) ||
                    comboName.contains("Tracking", ignoreCase = true) ->
                        "Remove ACCESS_BACKGROUND_LOCATION unless continuous background telemetry is indispensable. Rely on foreground service location or coarse location."

                    comboName.contains("Overlay", ignoreCase = true) ||
                    comboName.contains("Accessibility", ignoreCase = true) ->
                        "Remove SYSTEM_ALERT_WINDOW and BIND_ACCESSIBILITY_SERVICE unless building a dedicated assistive tool. Avoid UI overlays that can be abused for touch hijacking."

                    comboName.contains("Contact", ignoreCase = true) ->
                        "Use the system contact picker (Intent.ACTION_PICK) to access only user-selected contacts instead of holding broad READ_CONTACTS and telephony identifiers."

                    comboName.contains("Call Log", ignoreCase = true) ->
                        "Remove READ_CALL_LOG unless the application is the device's default dialer handler."

                    else ->
                        "Apply the Principle of Least Privilege: audit required permissions and remove unnecessary privilege combinations."
                }
            }

            // 2. Component exposure
            if (factorId == "EXP_ACT_SURFACE" || title.contains("Broad Activity Attack Surface", ignoreCase = true)) {
                return "Explicitly set android:exported=\"false\" on all internal activities in AndroidManifest.xml. Only export activities intended as external public entry points (e.g., MAIN/LAUNCHER or Deep Links), and protect sensitive entry points with signature permissions."
            }
            if (factorId.startsWith("EXP_SRV") || title.contains("Exported Service", ignoreCase = true)) {
                return "Declare android:exported=\"false\" on the service or protect it with a custom signature-level permission (android:permission) to prevent unauthorized invocation from third-party apps."
            }
            if (factorId.startsWith("EXP_RCV") || title.contains("Broadcast Receiver", ignoreCase = true)) {
                return "Declare android:exported=\"false\" or enforce a signature permission requirement on the receiver to prevent unsolicited external broadcasts."
            }
            if (factorId.startsWith("EXP_PRV") || title.contains("Content Provider", ignoreCase = true)) {
                return "Enforce custom signature-level readPermission and writePermission attributes on the content provider, or set android:exported=\"false\" if external data sharing is not required."
            }

            // 3. Configuration issues
            if (factorId == "CFG_DEBUGGABLE" || title.contains("Debuggable", ignoreCase = true)) {
                return "Set android:debuggable=\"false\" in AndroidManifest.xml and verify the release build type does not enable debug mode."
            }
            if (factorId == "CFG_ALLOW_BACKUP" || title.contains("Backup", ignoreCase = true)) {
                return "Set android:allowBackup=\"false\" in AndroidManifest.xml to protect sensitive SQLite databases and SharedPreferences from ADB backups."
            }
            if (factorId == "CFG_CLEARTEXT" || title.contains("Cleartext", ignoreCase = true)) {
                return "Configure a network_security_config.xml with cleartextTrafficPermitted=\"false\" to enforce HTTPS encryption for all network traffic."
            }
            if (factorId == "CFG_OUTDATED_TARGET_SDK" || title.contains("Target SDK", ignoreCase = true)) {
                return "Update targetSdkVersion to Android 14+ (API level 34+) to inherit modern platform security sandboxing, scoped storage, and intent-filter restrictions."
            }
            if (factorId == "CFG_OUTDATED_MIN_SDK" || title.contains("Min SDK", ignoreCase = true)) {
                return "Increase minSdkVersion to at least API 23 (Android 6.0) to ensure runtime permission enforcement on all target devices."
            }

            // 4. Special and dangerous permissions
            if (factorId.startsWith("SPEC_") || title.startsWith("Special Permission:", ignoreCase = true)) {
                val perm = evidence ?: title.substringAfter("Special Permission:").trim()
                return "Audit the business requirement for elevated system permission '$perm'. Replace with standard Android framework APIs or remove."
            }
            if (factorId.startsWith("DANG_") || title.startsWith("Dangerous Permission:", ignoreCase = true)) {
                val perm = evidence ?: title.substringAfter("Dangerous Permission:").trim()
                return "Audit necessity of '$perm'. Request permission only at the moment of use with clear in-app justification, and handle graceful fallback if denied."
            }

            // 5. DEX analysis
            if (title.contains("WebView", ignoreCase = true)) {
                return "Review WebView configuration: disable local file access (setAllowFileAccess(false)), disable content URL access, and enforce HTTPS URL loading."
            }
            if (title.contains("JavaScript Enabled", ignoreCase = true)) {
                return "Disable JavaScript (setJavaScriptEnabled(false)) unless required, and sanitize all input to prevent Cross-Site Scripting (XSS)."
            }
            if (title.contains("JavaScript Interface", ignoreCase = true)) {
                return "Ensure all methods exposed via @JavascriptInterface perform strict origin checks and input validation, and only expose them to trusted origins."
            }

            return "Review application configuration and apply the Principle of Least Privilege to minimize exposed attack surface."
        }

        /**
         * Builds a canonical, deduplicated list of findings from a RiskAssessment
         * and any scanner-specific findings.
         */
        fun buildUnifiedFindings(
            assessment: RiskAssessment,
            extraFindings: List<VulnerabilityFinding> = emptyList()
        ): List<VulnerabilityFinding> {
            val factorFindings = assessment.contributingFactors.map { factor ->
                VulnerabilityFinding(
                    name = factor.title,
                    severity = factor.severity,
                    affectedComponent = factor.category.displayName,
                    evidence = factor.evidence ?: factor.description,
                    impact = factor.description,
                    recommendation = resolveRecommendation(
                        factorId = factor.id,
                        title = factor.title,
                        category = factor.category,
                        evidence = factor.evidence
                    )
                )
            }

            // Filter out redundant raw component findings from extraFindings if already represented by factors
            val filteredExtras = extraFindings.filter { extra ->
                factorFindings.none { factor -> factor.name.equals(extra.name, ignoreCase = true) }
            }

            return (factorFindings + filteredExtras).sortedByDescending { it.severity.ordinal }
        }
    }

    /**
     * Creates a report from an APK analysis result and its risk assessment.
     */
    fun buildFromApkAnalysis(
        result: ApkAnalysisResult,
        risk: RiskAssessment
    ): SecurityReport {
        val apkInfo = result.apkInfo

        // Filter DEX string analysis and specialized scanner findings
        val dexFindings = result.findings.filter { finding ->
            finding.name.contains("WebView", ignoreCase = true) ||
            finding.name.contains("JavaScript", ignoreCase = true) ||
            finding.name.contains("DEX", ignoreCase = true) ||
            finding.name.contains("Dependency", ignoreCase = true) ||
            finding.name.contains("Vulnerability", ignoreCase = true)
        }

        val findings = buildUnifiedFindings(risk, extraFindings = dexFindings)

        val criticalCount = findings.count { it.severity == RiskLevel.CRITICAL }
        val highCount = findings.count { it.severity == RiskLevel.HIGH }
        val mediumCount = findings.count { it.severity == RiskLevel.MEDIUM }
        val lowCount = findings.count { it.severity == RiskLevel.LOW }

        // Extract dangerous permission entries from assessment contributing factors and findings
        val dangerousPermEntries = risk.contributingFactors
            .filter { it.id.startsWith("DANG_") }
            .map { factor ->
                val permName = factor.evidence ?: factor.title.substringAfterLast(":")
                PermissionEntry(
                    name = permName.substringAfterLast(".").trim(),
                    description = factor.description,
                    category = "DANGEROUS"
                )
            }.ifEmpty {
                result.findings
                    .filter { it.name.contains("Dangerous Permission", ignoreCase = true) }
                    .flatMap { finding ->
                        finding.evidence.lines()
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                    }
                    .distinct()
                    .map { permissionName ->
                        PermissionEntry(
                            name = permissionName.substringAfterLast("."),
                            description = "Dangerous permission reported by APK analysis.",
                            category = "DANGEROUS"
                        )
                    }
            }

        val executiveSummary = createExecutiveSummary(
            overallScore = risk.overallScore,
            overallLevel = risk.overallLevel,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            findingsCount = findings.size,
            dangerousPermCount = dangerousPermEntries.size,
            specialPermCount = 0,
            exportedComponentCount = risk.componentBreakdown.totalExported
        )

        val metadata = ReportAppMetadata(
            appName = apkInfo.appName
                ?.takeIf { it.isNotBlank() }
                ?: apkInfo.packageName,
            packageName = apkInfo.packageName,
            versionName = apkInfo.versionName,
            versionCode = apkInfo.versionCode,
            minSdk = apkInfo.minSdk,
            targetSdk = apkInfo.targetSdk,
            isSystemApp = false,
            scanType = ScanType.APK_FILE,
            scanTimestamp = System.currentTimeMillis()
        )

        return SecurityReport(
            metadata = metadata,
            summary = executiveSummary,
            riskAssessment = risk,
            vulnerabilityFindings = findings,
            dangerousPermissions = dangerousPermEntries,
            detectedCombinations = risk.detectedCombinations,
            componentBreakdown = risk.componentBreakdown,
            recommendations = risk.recommendations
        )
    }

    /**
     * Scans an APK supplied through a content URI and creates a report.
     */
    fun buildFromApkUri(uri: Uri): SecurityReport {
        val analysisResult = scannerRepo.scanApk(uri)
        val riskAssessment = riskRepo.assessApkResult(analysisResult)

        return buildFromApkAnalysis(
            result = analysisResult,
            risk = riskAssessment
        )
    }

    /**
     * Generates a report for an installed application.
     */
    fun buildFromInstalledApp(packageName: String): SecurityReport {
        val pm = context.packageManager

        val packageInfo = try {
            pm.getPackageInfo(packageName, 0)
        } catch (e: Exception) {
            val active = getActiveReport()
            if (active != null && active.metadata.packageName == packageName) {
                return active
            }
            throw IllegalArgumentException(
                "Unable to retrieve package information for $packageName.",
                e
            )
        }

        val appInfo = packageInfo.applicationInfo
        val appLabel = appInfo?.loadLabel(pm)?.toString()
            ?: packageName

        val isSystem = appInfo?.let {
            it.flags and ApplicationInfo.FLAG_SYSTEM != 0
        } ?: false

        val minSdk = if (android.os.Build.VERSION.SDK_INT >= 24) {
            appInfo?.minSdkVersion
        } else {
            21
        }

        val targetSdk = appInfo?.targetSdkVersion

        val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }

        val targetApp = AppInfo(
            name = appLabel,
            packageName = packageName,
            versionName = packageInfo.versionName,
            isSystemApp = isSystem
        )

        val permissions = permRepo
            .getAppPermissions(listOf(targetApp))
            .firstOrNull()
            ?.permissions
            ?: emptyList()

        val dangerousPerms = permissions.filter {
            it.category == "DANGEROUS"
        }

        val specialPerms = permissions.filter {
            it.category == "SPECIAL"
        }

        val riskAssessment = riskRepo.assessInstalledApp(packageName)

        val findings = buildUnifiedFindings(riskAssessment)

        val criticalCount = findings.count {
            it.severity == RiskLevel.CRITICAL
        }
        val highCount = findings.count {
            it.severity == RiskLevel.HIGH
        }
        val mediumCount = findings.count {
            it.severity == RiskLevel.MEDIUM
        }
        val lowCount = findings.count {
            it.severity == RiskLevel.LOW
        }

        val summary = createExecutiveSummary(
            overallScore = riskAssessment.overallScore,
            overallLevel = riskAssessment.overallLevel,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            findingsCount = findings.size,
            dangerousPermCount = dangerousPerms.size,
            specialPermCount = specialPerms.size,
            exportedComponentCount =
                riskAssessment.componentBreakdown.totalExported
        )

        val metadata = ReportAppMetadata(
            appName = appLabel,
            packageName = packageName,
            versionName = packageInfo.versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            isSystemApp = isSystem,
            scanType = ScanType.INSTALLED_APP,
            scanTimestamp = System.currentTimeMillis()
        )

        return SecurityReport(
            metadata = metadata,
            summary = summary,
            riskAssessment = riskAssessment,
            vulnerabilityFindings = findings,
            dangerousPermissions = dangerousPerms,
            specialPermissions = specialPerms,
            allPermissions = permissions,
            detectedCombinations = riskAssessment.detectedCombinations,
            componentBreakdown = riskAssessment.componentBreakdown,
            recommendations = riskAssessment.recommendations
        )
    }

    /**
     * Reconstructs a report from a persisted scan-history ID.
     */
    suspend fun buildFromHistoryId(
        historyId: Long
    ): SecurityReport? {
        val entry = historyRepo.getHistoryById(historyId)
            ?: return null

        return buildFromHistoryEntry(entry)
    }

    /**
     * Reconstructs a report from a persisted scan-history entry.
     *
     * The original history schema does not preserve individual risk-category
     * scores. Those category scores are therefore marked as unavailable
     * numerically rather than estimated from finding counts.
     */
    fun buildFromHistoryEntry(
        entry: ScanHistoryEntry
    ): SecurityReport {
        val dangerousPermEntries = entry.findings
            .filter {
                it.name.contains("Dangerous Permission", ignoreCase = true)
            }
            .flatMap { finding ->
                finding.evidence.lines()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            }
            .distinct()
            .map { permissionName ->
                PermissionEntry(
                    name = permissionName.substringAfterLast("."),
                    description = "Dangerous permission detected in scan audit.",
                    category = "DANGEROUS"
                )
            }

        val expAct = entry.findings.count {
            it.name.contains("Activity", ignoreCase = true) &&
                    it.name.contains("Exported", ignoreCase = true)
        }

        val expSrv = entry.findings.count {
            it.name.contains("Service", ignoreCase = true) &&
                    it.name.contains("Exported", ignoreCase = true)
        }

        val expRcv = entry.findings.count {
            it.name.contains("Receiver", ignoreCase = true) &&
                    it.name.contains("Exported", ignoreCase = true)
        }

        val expPrv = entry.findings.count {
            it.name.contains("Provider", ignoreCase = true) &&
                    it.name.contains("Exported", ignoreCase = true)
        }

        val compBreakdown = ComponentExposureBreakdown(
            totalExportedActivities = expAct,
            totalExportedServices = expSrv,
            totalExportedReceivers = expRcv,
            totalExportedProviders = expPrv
        )

        val recommendations = entry.findings.mapNotNull { finding ->
            if (finding.recommendation.isNotBlank()) {
                RiskRecommendation(
                    id = "REC_${finding.name.hashCode()}",
                    title = "Mitigate ${finding.name}",
                    action = finding.recommendation,
                    priority = finding.severity,
                    targetComponent = finding.affectedComponent
                )
            } else {
                null
            }
        }

        val riskAssessment = RiskAssessment(
            targetName = entry.targetName,
            packageName = entry.packageName,
            versionName = entry.versionName,
            overallScore = entry.riskScore,
            overallLevel = entry.riskLevel,

            // Historical records do not store individual category scores.
            // Represent them as unassessed (null) rather than fabricated zero/LOW risk.
            permissionRisk = null,
            permissionRiskLevel = null,

            vulnerabilityRisk = null,
            vulnerabilityRiskLevel = null,

            componentExposureRisk = null,
            componentExposureRiskLevel = null,

            configurationRisk = null,
            configurationRiskLevel = null,

            componentBreakdown = compBreakdown,
            recommendations = recommendations,
            minSdk = entry.minSdk.takeIf { it > 0 },
            targetSdk = entry.targetSdk.takeIf { it > 0 },
            versionCode = entry.versionCode.takeIf { it > 0 },
            timestamp = entry.timestamp
        )

        val summary = createExecutiveSummary(
            overallScore = entry.riskScore,
            overallLevel = entry.riskLevel,
            criticalCount = entry.criticalCount,
            highCount = entry.highCount,
            mediumCount = entry.mediumCount,
            lowCount = entry.lowCount,
            findingsCount = entry.findingsCount,
            dangerousPermCount = dangerousPermEntries.size,
            specialPermCount = 0,
            exportedComponentCount = compBreakdown.totalExported
        )

        val metadata = ReportAppMetadata(
            appName = entry.targetName,
            packageName = entry.packageName,
            versionName = entry.versionName,
            versionCode = entry.versionCode,
            minSdk = entry.minSdk,
            targetSdk = entry.targetSdk,
            isSystemApp = false,
            scanType = entry.scanType,
            scanTimestamp = entry.timestamp
        )

        return SecurityReport(
            metadata = metadata,
            summary = summary,
            riskAssessment = riskAssessment,
            vulnerabilityFindings = entry.findings,
            dangerousPermissions = dangerousPermEntries,
            componentBreakdown = compBreakdown,
            recommendations = recommendations
        )
    }

    /**
     * Creates the executive summary displayed in the report.
     */
    private fun createExecutiveSummary(
        overallScore: Int,
        overallLevel: RiskLevel,
        criticalCount: Int,
        highCount: Int,
        mediumCount: Int,
        lowCount: Int,
        findingsCount: Int,
        dangerousPermCount: Int,
        specialPermCount: Int,
        exportedComponentCount: Int
    ): ReportExecutiveSummary {
        val narrative = when (overallLevel) {
            RiskLevel.CRITICAL ->
                "Critical security threats detected. Immediate hardening required prior to deployment."

            RiskLevel.HIGH ->
                "High security risk identified. Multiple attack surfaces and sensitive privileges detected."

            RiskLevel.MEDIUM ->
                "Moderate security risk detected. Certain configurations and permissions warrant review."

            RiskLevel.LOW ->
                "Security posture meets standard baseline. Minimal elevated attack surface identified."
        }

        return ReportExecutiveSummary(
            overallScore = overallScore,
            overallLevel = overallLevel,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            totalFindingsCount = findingsCount,
            totalPermissionsCount = dangerousPermCount + specialPermCount,
            dangerousPermissionsCount = dangerousPermCount,
            specialPermissionsCount = specialPermCount,
            totalExportedComponents = exportedComponentCount,
            summaryNarrative = narrative
        )
    }

    /**
     * Formats a security report as Markdown for export or sharing.
     */
    fun formatReportForExport(
        report: SecurityReport
    ): String {
        val dateFormat = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        )

        val formattedDate = dateFormat.format(
            java.util.Date(report.metadata.scanTimestamp)
        )

        val sb = StringBuilder()

        sb.appendLine("# APVM Security Audit Report")
        sb.appendLine(
            "Target: ${report.metadata.appName} (${report.metadata.packageName})"
        )
        sb.appendLine(
            "Version: ${report.metadata.versionName ?: "N/A"} " +
                    "(Target SDK: ${report.metadata.targetSdk ?: "N/A"})"
        )
        sb.appendLine("Scan Type: ${report.metadata.scanType.displayName}")
        sb.appendLine("Date: $formattedDate")
        sb.appendLine()

        sb.appendLine("---")
        sb.appendLine("## Executive Summary")
        sb.appendLine(
            "Overall Risk Score: ${report.summary.overallScore}/100 " +
                    "[${report.summary.overallLevel.name}]"
        )
        sb.appendLine("Posture: ${report.summary.summaryNarrative}")
        sb.appendLine(
            "Findings Breakdown: ${report.summary.criticalCount} Critical, " +
                    "${report.summary.highCount} High, " +
                    "${report.summary.mediumCount} Medium, " +
                    "${report.summary.lowCount} Low"
        )
        sb.appendLine(
            "Dangerous Permissions: ${report.summary.dangerousPermissionsCount}"
        )
        sb.appendLine(
            "Exported Components: ${report.summary.totalExportedComponents}"
        )
        sb.appendLine()

        if (report.vulnerabilityFindings.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine(
                "## Security Findings (${report.vulnerabilityFindings.size})"
            )

            report.vulnerabilityFindings.forEachIndexed { index, finding ->
                sb.appendLine(
                    "${index + 1}. [${finding.severity.name}] ${finding.name}"
                )

                if (!finding.affectedComponent.isNullOrBlank()) {
                    sb.appendLine(
                        "   - Component: ${finding.affectedComponent}"
                    )
                }

                sb.appendLine(
                    "   - Evidence: ${finding.evidence.replace("\n", " ")}"
                )
                sb.appendLine("   - Impact: ${finding.impact}")
                sb.appendLine("   - Mitigation: ${finding.recommendation}")
                sb.appendLine()
            }
        }

        if (report.detectedCombinations.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine(
                "## Dangerous Permission Combinations " +
                        "(${report.detectedCombinations.size})"
            )

            report.detectedCombinations.forEach { combo ->
                sb.appendLine("- [${combo.severity.name}] ${combo.name}")
                sb.appendLine(
                    "  Permissions: ${combo.permissions.joinToString(", ")}"
                )
                sb.appendLine("  Risk: ${combo.dangerSummary}")
                sb.appendLine()
            }
        }

        if (report.recommendations.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine("## Actionable Mitigation Roadmap")

            report.recommendations.forEachIndexed { index, recommendation ->
                sb.appendLine(
                    "${index + 1}. [${recommendation.priority.name}] " +
                            recommendation.title
                )
                sb.appendLine("   Action: ${recommendation.action}")
            }
        }

        sb.appendLine()
        sb.appendLine(
            "Generated by APVM (Android Process, Permission & Vulnerability Manager)"
        )

        return sb.toString()
    }

    /**
     * Serializes the security report into pretty-printed structured JSON format.
     */
    fun formatReportAsJson(report: SecurityReport): String {
        return com.google.gson.GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create()
            .toJson(report)
    }
}

