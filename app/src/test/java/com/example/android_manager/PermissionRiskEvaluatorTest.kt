package com.example.android_manager

import com.example.android_manager.data.risk.PermissionRiskEvaluator
import com.example.android_manager.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating PermissionRiskEvaluator behavior, privilege weighting,
 * risky combination detection, boundary score caps, and input normalization.
 */
class PermissionRiskEvaluatorTest {

    private val evaluator = PermissionRiskEvaluator()

    @Test
    fun testEmptyPermissionsProduceZeroRisk() {
        val result = evaluator.evaluate(emptyList())
        assertEquals(0, result.score)
        assertEquals(RiskLevel.LOW, result.level)
        assertEquals(0, result.dangerousCount)
        assertEquals(0, result.specialCount)
        assertEquals(0, result.totalCount)
        assertTrue(result.contributingFactors.isEmpty())
        assertTrue(result.combinations.isEmpty())
    }

    @Test
    fun testInputNormalizationAndPackagePrefixStripping() {
        val perms = listOf(
            "android.permission.RECORD_AUDIO",
            "com.android.permission.CAMERA"
        )
        val result = evaluator.evaluate(perms)
        assertEquals(2, result.dangerousCount)
        assertEquals(2, result.totalCount)
        assertTrue(result.contributingFactors.any { it.id == "DANG_RECORD_AUDIO" })
        assertTrue(result.contributingFactors.any { it.id == "DANG_CAMERA" })
    }

    @Test
    fun testSpecialPermissionWeighting() {
        val perms = listOf("android.permission.SYSTEM_ALERT_WINDOW")
        val result = evaluator.evaluate(perms)
        assertEquals(1, result.specialCount)
        assertEquals(0, result.dangerousCount)
        assertEquals(18, result.score)
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.contributingFactors.any { it.id == "SPEC_SYSTEM_ALERT_WINDOW" })
    }

    @Test
    fun testAudioCaptureAndNetworkCombination() {
        val perms = listOf(
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET"
        )
        val result = evaluator.evaluate(perms)
        assertEquals(1, result.dangerousCount)
        assertEquals(1, result.combinations.size)
        assertEquals("Audio Capture & Network Capability", result.combinations[0].name)
        assertEquals(RiskLevel.HIGH, result.combinations[0].severity)

        // RECORD_AUDIO (15) + Combo (22) = 37
        assertEquals(37, result.score)
        assertEquals(RiskLevel.MEDIUM, result.level)
    }

    @Test
    fun testDynamicPackageInstallationCombination() {
        val perms = listOf(
            "android.permission.REQUEST_INSTALL_PACKAGES",
            "android.permission.INTERNET"
        )
        val result = evaluator.evaluate(perms)
        assertEquals(1, result.specialCount)
        assertEquals(1, result.combinations.size)
        assertEquals("Dynamic Package Installation Risk", result.combinations[0].name)
        assertEquals(RiskLevel.HIGH, result.combinations[0].severity)
        // REQUEST_INSTALL_PACKAGES (20) + Combo (25) = 45
        assertEquals(45, result.score)
        assertEquals(RiskLevel.MEDIUM, result.level)
    }

    @Test
    fun testScoreClampingAtBoundary() {
        // Request an overwhelming set of dangerous permissions and special permissions
        val massivePerms = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.INTERNET",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_BACKGROUND_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_SMS",
            "android.permission.SEND_SMS",
            "android.permission.RECEIVE_SMS",
            "android.permission.RECEIVE_BOOT_COMPLETED",
            "android.permission.SYSTEM_ALERT_WINDOW",
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "android.permission.REQUEST_INSTALL_PACKAGES"
        )
        val result = evaluator.evaluate(massivePerms)
        assertEquals(100, result.score)
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertTrue(result.combinations.size >= 4)
    }

    @Test
    fun testExcessiveDangerousPermissionsPenalty() {
        val perms = listOf(
            "android.permission.CAMERA",
            "android.permission.RECORD_AUDIO",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_CALENDAR",
            "android.permission.BODY_SENSORS"
        )
        val result = evaluator.evaluate(perms)
        assertEquals(6, result.dangerousCount)
        assertTrue(result.contributingFactors.any { it.id == "EXCESSIVE_DANGEROUS" })
    }
}
