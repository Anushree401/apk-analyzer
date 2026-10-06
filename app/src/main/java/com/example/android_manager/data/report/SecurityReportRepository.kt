package com.example.android_manager.data.report

import android.content.Context
import android.net.Uri
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.data.permissions.PermissionEntry
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.data.risk.RiskAssessmentRepository
import com.example.android_manager.data.scanner.ApkAnalysisResult
import com.example.android_manager.data.scanner.ApkScannerRepository
import com.example.android_manager.model.ReportAppMetadata
import com.example.android_manager.model.ReportExecutiveSummary
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.model.RiskLevel
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
}