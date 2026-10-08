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
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.RiskRecommendation
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.model.ScanType
import com.example.android_manager.model.SecurityReport
import com.example.android_manager.model.VulnerabilityFinding

/**
 * Repository orchestrating the generation, caching, and retrieval of comprehensive
 * security reports across APK scans, installed applications, and historical audit logs.
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
        private var activeReport: SecurityReport? = null

        /**
         * Sets the active report in memory for immediate access by the report screen.
         */
        fun setActiveReport(report: SecurityReport) {
            activeReport = report
        }

        /**
         * Gets the active report currently loaded in memory.
         */
        fun getActiveReport(): SecurityReport? = activeReport

        /**
         * Clears the active report from memory.
         */
        fun clearActiveReport() {
            activeReport = null
        }
    }

    /**
     * Synthesizes an APK analysis result and risk assessment into a comprehensive SecurityReport.
     */
    fun buildFromApkAnalysis(
        result: ApkAnalysisResult,
        risk: RiskAssessment
    ): SecurityReport {
        val apkInfo = result.apkInfo
        val findings = result.findings

        val criticalCount = findings.count { it.severity == RiskLevel.CRITICAL }
        val highCount = findings.count { it.severity == RiskLevel.HIGH }
        val mediumCount = findings.count { it.severity == RiskLevel.MEDIUM }
        val lowCount = findings.count { it.severity == RiskLevel.LOW }

        // Extract dangerous permissions from findings evidence where present
        val dangerousPermEntries = findings
            .filter { it.name.contains("Dangerous Permission", ignoreCase = true) }
            .flatMap { it.evidence.lines() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { permName ->
                PermissionEntry(
                    name = permName.substringAfterLast("."),
                    description = "Dangerous runtime permission requested by APK manifest.",
                    category = "DANGEROUS"
                )
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
            appName = apkInfo.appName?.takeIf { it.isNotBlank() } ?: apkInfo.packageName,
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
     * Directly analyzes an APK from a content URI and produces a complete SecurityReport.
     */
    fun buildFromApkUri(uri: Uri): SecurityReport {
        val analysisResult = scannerRepo.scanApk(uri)
        val riskAssessment = riskRepo.assessApkResult(analysisResult)
        return buildFromApkAnalysis(analysisResult, riskAssessment)
    }

    /**
     * Generates a complete SecurityReport for an installed package on the device.
     */
    fun buildFromInstalledApp(packageName: String): SecurityReport {
        val pm = context.packageManager
        val packageInfo = try {
            pm.getPackageInfo(packageName, 0)
        } catch (_: Exception) {
            null
        }

        val appInfo = packageInfo?.applicationInfo
        val appLabel = appInfo?.loadLabel(pm)?.toString() ?: packageName
        val isSystem = (appInfo?.flags?.let { it and ApplicationInfo.FLAG_SYSTEM != 0 }) ?: false
        val minSdk = if (android.os.Build.VERSION.SDK_INT >= 24) appInfo?.minSdkVersion else 21
        val targetSdk = appInfo?.targetSdkVersion
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= 28) {
            packageInfo?.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong()
        }

        val targetApp = AppInfo(
            name = appLabel,
            packageName = packageName,
            versionName = packageInfo?.versionName,
            isSystemApp = isSystem
        )

        val permissions = permRepo.getAppPermissions(listOf(targetApp)).firstOrNull()?.permissions ?: emptyList()
        val dangerousPerms = permissions.filter { it.category == "DANGEROUS" }
        val specialPerms = permissions.filter { it.category == "SPECIAL" }
        val riskAssessment = riskRepo.assessInstalledApp(packageName)

        val findings = riskAssessment.contributingFactors.map { factor ->
            VulnerabilityFinding(
                name = factor.title,
                severity = factor.severity,
                affectedComponent = factor.category.displayName,
                evidence = factor.evidence ?: factor.description,
                impact = factor.description,
                recommendation = riskAssessment.recommendations.find { it.title.contains(factor.title, ignoreCase = true) }?.action
                    ?: "Review application privileges and minimize attack surface."
            )
        }

        val criticalCount = findings.count { it.severity == RiskLevel.CRITICAL }
        val highCount = findings.count { it.severity == RiskLevel.HIGH }
        val mediumCount = findings.count { it.severity == RiskLevel.MEDIUM }
        val lowCount = findings.count { it.severity == RiskLevel.LOW }

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
            exportedComponentCount = riskAssessment.componentBreakdown.totalExported
        )

        val metadata = ReportAppMetadata(
            appName = appLabel,
            packageName = packageName,
            versionName = packageInfo?.versionName,
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
     * Reconstructs a complete SecurityReport from a persisted ScanHistory entry by ID.
     */
    suspend fun buildFromHistoryId(historyId: Long): SecurityReport? {
        val entry = historyRepo.getHistoryById(historyId) ?: return null
        return buildFromHistoryEntry(entry)
    }

    /**
     * Reconstructs a complete SecurityReport from a persisted ScanHistoryEntry domain object.
     */
    fun buildFromHistoryEntry(entry: ScanHistoryEntry): SecurityReport {
        val dangerousPermEntries = entry.findings
            .filter { it.name.contains("Dangerous Permission", ignoreCase = true) }
            .flatMap { it.evidence.lines() }
            .filter { it.isNotBlank() }
            .distinct()
            .map { permName ->
                PermissionEntry(
                    name = permName.substringAfterLast("."),
                    description = "Dangerous permission detected in scan audit.",
                    category = "DANGEROUS"
                )
            }

        val expAct = entry.findings.count { it.name.contains("Activity", ignoreCase = true) && it.name.contains("Exported", ignoreCase = true) }
        val expSrv = entry.findings.count { it.name.contains("Service", ignoreCase = true) && it.name.contains("Exported", ignoreCase = true) }
        val expRcv = entry.findings.count { it.name.contains("Receiver", ignoreCase = true) && it.name.contains("Exported", ignoreCase = true) }
        val expPrv = entry.findings.count { it.name.contains("Provider", ignoreCase = true) && it.name.contains("Exported", ignoreCase = true) }

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
            } else null
        }

        val permRiskScore = if (entry.findingsCount > 0) (dangerousPermEntries.size * 10).coerceIn(0, 100) else 0
        val compRiskScore = (compBreakdown.totalExported * 15).coerceIn(0, 100)

        val riskAssessment = RiskAssessment(
            targetName = entry.targetName,
            packageName = entry.packageName,
            versionName = entry.versionName,
            overallScore = entry.riskScore,
            overallLevel = entry.riskLevel,
            permissionRisk = permRiskScore,
            permissionRiskLevel = RiskLevel.fromScore(permRiskScore),
            vulnerabilityRisk = entry.riskScore,
            vulnerabilityRiskLevel = entry.riskLevel,
            componentExposureRisk = compRiskScore,
            componentExposureRiskLevel = RiskLevel.fromScore(compRiskScore),
            configurationRisk = 0,
            configurationRiskLevel = RiskLevel.LOW,
            componentBreakdown = compBreakdown,
            recommendations = recommendations,
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
            RiskLevel.CRITICAL -> "Critical security threats detected. Immediate hardening required prior to deployment."
            RiskLevel.HIGH -> "High security risk identified. Multiple attack surfaces and sensitive privileges detected."
            RiskLevel.MEDIUM -> "Moderate security risk detected. Certain configurations and permissions warrant review."
            RiskLevel.LOW -> "Security posture meets standard baseline. Minimal elevated attack surface identified."
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
     * Formats a SecurityReport into a human-readable Markdown summary suitable for export and sharing.
     */
    fun formatReportForExport(report: SecurityReport): String {
        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val formattedDate = dateFormat.format(java.util.Date(report.metadata.scanTimestamp))

        val sb = StringBuilder()
        sb.appendLine("# APVM Security Audit Report")
        sb.appendLine("Target: ${report.metadata.appName} (${report.metadata.packageName})")
        sb.appendLine("Version: ${report.metadata.versionName ?: "N/A"} (Target SDK: ${report.metadata.targetSdk ?: "N/A"})")
        sb.appendLine("Scan Type: ${report.metadata.scanType.displayName}")
        sb.appendLine("Date: $formattedDate")
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("## Executive Summary")
        sb.appendLine("Overall Risk Score: ${report.summary.overallScore}/100 [${report.summary.overallLevel.name}]")
        sb.appendLine("Posture: ${report.summary.summaryNarrative}")
        sb.appendLine("Findings Breakdown: ${report.summary.criticalCount} Critical, ${report.summary.highCount} High, ${report.summary.mediumCount} Medium, ${report.summary.lowCount} Low")
        sb.appendLine("Dangerous Permissions: ${report.summary.dangerousPermissionsCount}")
        sb.appendLine("Exported Components: ${report.summary.totalExportedComponents}")
        sb.appendLine()

        if (report.vulnerabilityFindings.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine("## Vulnerability Findings (${report.vulnerabilityFindings.size})")
            report.vulnerabilityFindings.forEachIndexed { index, vuln ->
                sb.appendLine("${index + 1}. [${vuln.severity.name}] ${vuln.name}")
                if (!vuln.affectedComponent.isNullOrBlank()) {
                    sb.appendLine("   - Component: ${vuln.affectedComponent}")
                }
                sb.appendLine("   - Evidence: ${vuln.evidence.replace("\n", " ")}")
                sb.appendLine("   - Impact: ${vuln.impact}")
                sb.appendLine("   - Mitigation: ${vuln.recommendation}")
                sb.appendLine()
            }
        }

        if (report.detectedCombinations.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine("## Dangerous Permission Combinations (${report.detectedCombinations.size})")
            report.detectedCombinations.forEach { combo ->
                sb.appendLine("- [${combo.severity.name}] ${combo.name}")
                sb.appendLine("  Permissions: ${combo.permissions.joinToString(", ")}")
                sb.appendLine("  Risk: ${combo.dangerSummary}")
                sb.appendLine()
            }
        }

        if (report.recommendations.isNotEmpty()) {
            sb.appendLine("---")
            sb.appendLine("## Actionable Mitigation Roadmap")
            report.recommendations.forEachIndexed { index, rec ->
                sb.appendLine("${index + 1}. [${rec.priority.name}] ${rec.title}")
                sb.appendLine("   Action: ${rec.action}")
            }
        }

        sb.appendLine()
        sb.appendLine("Generated by APVM (Android Process, Permission & Vulnerability Manager)")
        return sb.toString()
    }
}