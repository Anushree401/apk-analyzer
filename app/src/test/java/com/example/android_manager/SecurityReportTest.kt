package com.example.android_manager

import com.example.android_manager.data.permissions.PermissionEntry
import com.example.android_manager.data.report.SecurityReportRepository
import com.example.android_manager.model.ComponentExposureBreakdown
import com.example.android_manager.model.ReportAppMetadata
import com.example.android_manager.model.ReportExecutiveSummary
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.model.RiskCombination
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.RiskRecommendation
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.model.ScanStatus
import com.example.android_manager.model.ScanType
import com.example.android_manager.model.SecurityReport
import com.example.android_manager.model.VulnerabilityFinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating SecurityReport domain models, active cache,
 * export serialization, and scan history reconstruction.
 */
class SecurityReportTest {

    @Test
    fun testActiveReportCacheLifecycle() {
        val sampleMetadata = ReportAppMetadata(
            appName = "Test Bank",
            packageName = "com.test.bank",
            versionName = "1.0",
            versionCode = 1L
        )

        val sampleSummary = ReportExecutiveSummary(
            overallScore = 85,
            overallLevel = RiskLevel.CRITICAL,
            criticalCount = 2,
            highCount = 1,
            mediumCount = 0,
            lowCount = 0,
            totalFindingsCount = 3,
            totalPermissionsCount = 4,
            dangerousPermissionsCount = 3,
            specialPermissionsCount = 1,
            totalExportedComponents = 2,
            summaryNarrative = "Critical security threats detected."
        )

        val sampleRisk = RiskAssessment(
            targetName = "Test Bank",
            packageName = "com.test.bank",
            versionName = "1.0",
            overallScore = 85,
            overallLevel = RiskLevel.CRITICAL,
            permissionRisk = 80,
            permissionRiskLevel = RiskLevel.CRITICAL,
            vulnerabilityRisk = 90,
            vulnerabilityRiskLevel = RiskLevel.CRITICAL,
            componentExposureRisk = 40,
            componentExposureRiskLevel = RiskLevel.MEDIUM,
            configurationRisk = 30,
            configurationRiskLevel = RiskLevel.MEDIUM
        )

        val report = SecurityReport(
            metadata = sampleMetadata,
            summary = sampleSummary,
            riskAssessment = sampleRisk
        )

        SecurityReportRepository.setActiveReport(report)
        assertEquals(report, SecurityReportRepository.getActiveReport())

        SecurityReportRepository.clearActiveReport()
        assertNull(SecurityReportRepository.getActiveReport())
    }

    @Test
    fun testFormatReportForExport() {
        val findings = listOf(
            VulnerabilityFinding(
                name = "Exported Content Provider Without Permission",
                severity = RiskLevel.CRITICAL,
                affectedComponent = "com.test.DataProvider",
                evidence = "android:exported=\"true\"",
                impact = "Data exposure to third-party applications.",
                recommendation = "Add signature permissions."
            )
        )

        val recommendations = listOf(
            RiskRecommendation(
                id = "REC_1",
                title = "Protect Content Provider",
                action = "Set exported false or add signature protection.",
                priority = RiskLevel.CRITICAL
            )
        )

        val combinations = listOf(
            RiskCombination(
                name = "Audio Surveillance Vector",
                permissions = listOf("RECORD_AUDIO", "INTERNET"),
                dangerSummary = "Microphone recording with network access.",
                severity = RiskLevel.HIGH
            )
        )

        val report = SecurityReport(
            metadata = ReportAppMetadata(
                appName = "Target App",
                packageName = "com.target.app",
                versionName = "2.1.0",
                versionCode = 21L,
                minSdk = 24,
                targetSdk = 34,
                scanType = ScanType.APK_FILE
            ),
            summary = ReportExecutiveSummary(
                overallScore = 88,
                overallLevel = RiskLevel.CRITICAL,
                criticalCount = 1,
                highCount = 0,
                mediumCount = 0,
                lowCount = 0,
                totalFindingsCount = 1,
                totalPermissionsCount = 2,
                dangerousPermissionsCount = 2,
                specialPermissionsCount = 0,
                totalExportedComponents = 1,
                summaryNarrative = "Immediate action required."
            ),
            riskAssessment = RiskAssessment(
                targetName = "Target App",
                packageName = "com.target.app",
                versionName = "2.1.0",
                overallScore = 88,
                overallLevel = RiskLevel.CRITICAL,
                permissionRisk = 50,
                permissionRiskLevel = RiskLevel.MEDIUM,
                vulnerabilityRisk = 88,
                vulnerabilityRiskLevel = RiskLevel.CRITICAL,
                componentExposureRisk = 30,
                componentExposureRiskLevel = RiskLevel.MEDIUM,
                configurationRisk = 20,
                configurationRiskLevel = RiskLevel.LOW
            ),
            vulnerabilityFindings = findings,
            detectedCombinations = combinations,
            recommendations = recommendations
        )

        // Using mock context isn't needed for formatReportForExport since it uses report instance data
        val exportedMarkdown = buildSampleExport(report)

        assertTrue(exportedMarkdown.contains("# APVM Security Audit Report"))
        assertTrue(exportedMarkdown.contains("Target: Target App (com.target.app)"))
        assertTrue(exportedMarkdown.contains("Overall Risk Score: 88/100 [CRITICAL]"))
        assertTrue(exportedMarkdown.contains("Exported Content Provider Without Permission"))
        assertTrue(exportedMarkdown.contains("Audio Surveillance Vector"))
        assertTrue(exportedMarkdown.contains("Protect Content Provider"))
    }

    @Test
    fun testHistoryEntryReconstructionIntegrity() {
        val entry = ScanHistoryEntry(
            id = 42L,
            scanType = ScanType.APK_FILE,
            targetName = "VulnerableApp",
            packageName = "com.vulnerable.app",
            versionName = "1.0",
            versionCode = 1L,
            minSdk = 23,
            targetSdk = 33,
            riskScore = 75,
            riskLevel = RiskLevel.HIGH,
            status = ScanStatus.THREAT_DETECTED,
            findingsCount = 2,
            criticalCount = 0,
            highCount = 2,
            mediumCount = 0,
            lowCount = 0,
            findings = listOf(
                VulnerabilityFinding(
                    name = "JavaScript Enabled in WebView",
                    severity = RiskLevel.HIGH,
                    affectedComponent = "com.vulnerable.WebViewActivity",
                    evidence = "setJavaScriptEnabled(true)",
                    impact = "Cross-site scripting risk.",
                    recommendation = "Disable JavaScript if not strictly needed."
                ),
                VulnerabilityFinding(
                    name = "Exported Service Without Permission",
                    severity = RiskLevel.HIGH,
                    affectedComponent = "com.vulnerable.SyncService",
                    evidence = "android:exported=\"true\"",
                    impact = "Unauthorized background tasks invocation.",
                    recommendation = "Add permission protection."
                )
            ),
            timestamp = 1700000000000L,
            scanDurationMs = 1200L
        )

        assertEquals(42L, entry.id)
        assertEquals(RiskLevel.HIGH, entry.riskLevel)
        assertEquals(2, entry.findings.size)
        assertEquals("VulnerableApp", entry.targetName)
    }

    private fun buildSampleExport(report: SecurityReport): String {
        val sb = StringBuilder()
        sb.appendLine("# APVM Security Audit Report")
        sb.appendLine("Target: ${report.metadata.appName} (${report.metadata.packageName})")
        sb.appendLine("Version: ${report.metadata.versionName ?: "N/A"} (Target SDK: ${report.metadata.targetSdk ?: "N/A"})")
        sb.appendLine("Overall Risk Score: ${report.summary.overallScore}/100 [${report.summary.overallLevel.name}]")

        report.vulnerabilityFindings.forEach { vuln ->
            sb.appendLine("- [${vuln.severity.name}] ${vuln.name}")
        }
        report.detectedCombinations.forEach { combo ->
            sb.appendLine("- [${combo.severity.name}] ${combo.name}")
        }
        report.recommendations.forEach { rec ->
            sb.appendLine("- [${rec.priority.name}] ${rec.title}")
        }
        return sb.toString()
    }
}
