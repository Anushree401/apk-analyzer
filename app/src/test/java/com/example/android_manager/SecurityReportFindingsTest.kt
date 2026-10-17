package com.example.android_manager

import com.example.android_manager.data.report.SecurityReportRepository
import com.example.android_manager.data.risk.ComponentExposureEvaluator
import com.example.android_manager.data.risk.ConfigurationRiskEvaluator
import com.example.android_manager.data.risk.PermissionRiskEvaluator
import com.example.android_manager.model.ComponentExposureBreakdown
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.model.RiskCategory
import com.example.android_manager.model.RiskFactor
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.VulnerabilityFinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit tests verifying the Security Report Findings pipeline:
 * - Finding provenance (exact rule, source data, trigger conditions)
 * - Technical evidence accuracy (e.g. "10 exported activities")
 * - Severity calculation and risk policy enforcement
 * - Mitigation specificity (eliminating generic fallback)
 * - Internal consistency across counts, severities, and report models
 */
class SecurityReportFindingsTest {

    private val permissionEvaluator = PermissionRiskEvaluator()
    private val componentEvaluator = ComponentExposureEvaluator()
    private val configEvaluator = ConfigurationRiskEvaluator()

    @Test
    fun testTestCase1_CameraAndAudioWithNetwork_CriticalProvenanceAndMitigation() {
        // Trigger condition: CAMERA + RECORD_AUDIO + INTERNET
        val requestedPermissions = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
        )
        val result = permissionEvaluator.evaluate(requestedPermissions)

        // 1. Finding Provenance
        val comboFactor = result.contributingFactors.find {
            it.title.contains("Camera", ignoreCase = true) && it.title.contains("Audio", ignoreCase = true)
        }
        assertNotNull("Camera & Audio capture combo factor must be present", comboFactor)
        assertEquals(RiskLevel.CRITICAL, comboFactor!!.severity)
        assertEquals(RiskCategory.COMBINATION, comboFactor.category)

        // 2. Technical Evidence
        assertTrue(
            "Evidence must explicitly list the required trigger permissions",
            comboFactor.evidence?.contains("CAMERA") == true &&
                    comboFactor.evidence?.contains("RECORD_AUDIO") == true &&
                    comboFactor.evidence?.contains("INTERNET") == true
        )

        // 3. Security Impact
        assertTrue(
            "Impact must describe capability without claiming confirmed spyware exfiltration",
            comboFactor.description.contains("Simultaneous camera and microphone", ignoreCase = true) ||
                    comboFactor.description.contains("capture", ignoreCase = true)
        )
        assertFalse(
            "Must not claim proven exfiltration without behavioral malware evidence",
            comboFactor.title.contains("exfiltration", ignoreCase = true)
        )

        // 4. Specific Mitigation
        val recommendation = SecurityReportRepository.resolveRecommendation(
            factorId = comboFactor.id,
            title = comboFactor.title,
            category = comboFactor.category,
            evidence = comboFactor.evidence
        )
        assertTrue(
            "Recommendation must specifically mention camera/microphone or system intents",
            recommendation.contains("camera", ignoreCase = true) || recommendation.contains("microphone", ignoreCase = true)
        )
        assertFalse(
            "Must not use generic fallback",
            recommendation == "Review application privileges and minimize attack surface."
        )
    }

    @Test
    fun testTestCase2_DynamicPackageInstallation_HighProvenanceAndMitigation() {
        // Trigger condition: REQUEST_INSTALL_PACKAGES + INTERNET
        val requestedPermissions = listOf(
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.INTERNET"
        )
        val result = permissionEvaluator.evaluate(requestedPermissions)

        // 1. Finding Provenance
        val comboFactor = result.contributingFactors.find {
            it.title.contains("Dynamic Package Installation", ignoreCase = true) ||
                    it.title.contains("Package Installation", ignoreCase = true) ||
                    it.title.contains("Trojan Dropper", ignoreCase = true)
        }
        assertNotNull("Package installation combo factor must be present", comboFactor)
        assertEquals(RiskLevel.HIGH, comboFactor!!.severity)

        // 2. Technical Evidence
        assertEquals("REQUEST_INSTALL_PACKAGES + INTERNET", comboFactor.evidence)

        // 3. Security Impact
        assertTrue(
            "Impact must explain sideloading capability without branding the app as a verified Trojan",
            comboFactor.description.contains("secondary APK packages", ignoreCase = true) ||
                    comboFactor.description.contains("without official store verification", ignoreCase = true)
        )

        // 4. Specific Mitigation
        val recommendation = SecurityReportRepository.resolveRecommendation(
            factorId = comboFactor.id,
            title = comboFactor.title,
            category = comboFactor.category,
            evidence = comboFactor.evidence
        )
        assertTrue(
            "Recommendation must specifically mention REQUEST_INSTALL_PACKAGES or verified app stores",
            recommendation.contains("REQUEST_INSTALL_PACKAGES", ignoreCase = true) ||
                    recommendation.contains("app stores", ignoreCase = true)
        )
        assertFalse(
            "Must not use generic fallback",
            recommendation == "Review application privileges and minimize attack surface."
        )
    }

    @Test
    fun testTestCase3_BroadActivityAttackSurface_HighWithTenExportedActivities() {
        // Trigger condition: 10 exported activities (> 8 triggers HIGH severity)
        val packageInfo = android.content.pm.PackageInfo().apply {
            activities = (1..10).map { i ->
                android.content.pm.ActivityInfo().apply {
                    name = "com.example.app.Activity$i"
                    exported = true
                    permission = null // unprotected
                }
            }.toTypedArray()
        }

        val result = componentEvaluator.evaluate(packageInfo)

        // 1. Finding Provenance
        val surfaceFactor = result.contributingFactors.find { it.id == "EXP_ACT_SURFACE" }
        assertNotNull("Broad Activity Attack Surface factor must be generated when exportedActivities > 3", surfaceFactor)

        // 2. Severity Calculation Policy: > 8 activities is HIGH
        assertEquals("10 exported activities must result in HIGH severity", RiskLevel.HIGH, surfaceFactor!!.severity)

        // 3. Technical Evidence
        assertEquals("10 exported activities", surfaceFactor.evidence)

        // 4. Security Impact
        assertTrue(
            "Description must explain intent manipulation / attack surface exposure",
            surfaceFactor.description.contains("10 activities are exported", ignoreCase = true)
        )

        // 5. Specific Mitigation
        val recommendation = SecurityReportRepository.resolveRecommendation(
            factorId = surfaceFactor.id,
            title = surfaceFactor.title,
            category = surfaceFactor.category,
            evidence = surfaceFactor.evidence
        )
        assertTrue(
            "Recommendation must advise setting android:exported=\"false\"",
            recommendation.contains("android:exported=\"false\"", ignoreCase = true)
        )
        assertFalse(
            "Must not use generic privileges fallback for component exposure",
            recommendation == "Review application privileges and minimize attack surface."
        )
    }

    @Test
    fun testTestCase4_AudioSurveillanceVector_HighProvenanceAndMitigation() {
        // Trigger condition: RECORD_AUDIO + INTERNET
        val requestedPermissions = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
        )
        val result = permissionEvaluator.evaluate(requestedPermissions)

        // 1. Finding Provenance
        val comboFactor = result.contributingFactors.find {
            it.title.contains("Audio Capture", ignoreCase = true) ||
                    it.title.contains("Audio Surveillance", ignoreCase = true)
        }
        assertNotNull("Audio combo factor must be present", comboFactor)
        assertEquals(RiskLevel.HIGH, comboFactor!!.severity)

        // 2. Technical Evidence
        assertEquals("RECORD_AUDIO + INTERNET", comboFactor.evidence)

        // 3. Specific Mitigation
        val recommendation = SecurityReportRepository.resolveRecommendation(
            factorId = comboFactor.id,
            title = comboFactor.title,
            category = comboFactor.category,
            evidence = comboFactor.evidence
        )
        assertTrue(
            "Recommendation must specifically address audio recording permission and indicators",
            recommendation.contains("microphone", ignoreCase = true) || recommendation.contains("RECORD_AUDIO", ignoreCase = true)
        )
        assertFalse(
            "Must not use generic fallback",
            recommendation == "Review application privileges and minimize attack surface."
        )
    }

    @Test
    fun testUnifiedFindings_CountsAndSeveritiesMatchReport() {
        val dummyFactors = listOf(
            RiskFactor(
                id = "COMB_CAM_AUDIO",
                title = "Camera & Audio Capture with Network Access",
                description = "Hardware sensors access with outbound network.",
                severity = RiskLevel.CRITICAL,
                category = RiskCategory.COMBINATION,
                scorePenalty = 30,
                evidence = "CAMERA + RECORD_AUDIO + INTERNET"
            ),
            RiskFactor(
                id = "EXP_ACT_SURFACE",
                title = "Broad Activity Attack Surface",
                description = "10 exported activities.",
                severity = RiskLevel.HIGH,
                category = RiskCategory.COMPONENT_EXPOSURE,
                scorePenalty = 25,
                evidence = "10 exported activities"
            ),
            RiskFactor(
                id = "CFG_ALLOW_BACKUP",
                title = "ADB Backup Enabled",
                description = "Backup enabled.",
                severity = RiskLevel.MEDIUM,
                category = RiskCategory.CONFIGURATION,
                scorePenalty = 12,
                evidence = "FLAG_ALLOW_BACKUP == true"
            )
        )

        val assessment = RiskAssessment(
            targetName = "Sample Target",
            packageName = "com.sample.target",
            versionName = "1.0.0",
            overallScore = 75,
            overallLevel = RiskLevel.HIGH,
            componentBreakdown = ComponentExposureBreakdown(10, 0, 0, 0),
            contributingFactors = dummyFactors
        )

        val dexFindings = listOf(
            VulnerabilityFinding(
                name = "JavaScript Enabled in WebView",
                severity = RiskLevel.HIGH,
                affectedComponent = null,
                evidence = "setJavaScriptEnabled reference detected",
                impact = "Increases WebView attack surface",
                recommendation = "Disable JavaScript unless required"
            )
        )

        val unified = SecurityReportRepository.buildUnifiedFindings(assessment, extraFindings = dexFindings)

        // Assert 4 total findings (3 factors + 1 unique dex finding)
        assertEquals(4, unified.size)

        // Assert finding counts per severity
        val criticalCount = unified.count { it.severity == RiskLevel.CRITICAL }
        val highCount = unified.count { it.severity == RiskLevel.HIGH }
        val mediumCount = unified.count { it.severity == RiskLevel.MEDIUM }
        val lowCount = unified.count { it.severity == RiskLevel.LOW }

        assertEquals(1, criticalCount)
        assertEquals(2, highCount)
        assertEquals(1, mediumCount)
        assertEquals(0, lowCount)

        // Verify that every single finding has a specific non-empty recommendation
        unified.forEach { finding ->
            assertTrue(
                "Finding '${finding.name}' must have a specific, actionable recommendation",
                finding.recommendation.isNotBlank() &&
                        finding.recommendation != "Review application privileges and minimize attack surface."
            )
        }
    }
}
