package com.example.android_manager

import android.content.pm.ApplicationInfo
import com.example.android_manager.data.risk.ConfigurationRiskEvaluator
import com.example.android_manager.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating ConfigurationRiskEvaluator for debuggable flags,
 * backup settings, cleartext traffic allowance, and target/min SDK levels.
 */
class ConfigurationRiskEvaluatorTest {

    private val evaluator = ConfigurationRiskEvaluator()

    @Test
    fun testHardenedModernApplicationConfiguration() {
        val appInfo = ApplicationInfo().apply {
            flags = 0 // not debuggable, backup disabled, cleartext disabled
            targetSdkVersion = 34
            minSdkVersion = 26
        }

        val result = evaluator.evaluate(
            applicationInfo = appInfo,
            targetSdk = 34,
            minSdk = 26
        )

        assertEquals(0, result.score)
        assertEquals(RiskLevel.LOW, result.level)
        assertFalse(result.isDebuggable)
        assertFalse(result.allowBackup)
        assertTrue(result.contributingFactors.isEmpty())
    }

    @Test
    fun testDebuggableFlagTriggersCriticalPenalty() {
        val appInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_DEBUGGABLE
            targetSdkVersion = 34
            minSdkVersion = 26
        }

        val result = evaluator.evaluate(
            applicationInfo = appInfo,
            targetSdk = 34,
            minSdk = 26
        )

        assertEquals(30, result.score)
        assertEquals(RiskLevel.MEDIUM, result.level)
        assertTrue(result.isDebuggable)
        assertTrue(result.contributingFactors.any { it.id == "CFG_DEBUGGABLE" && it.severity == RiskLevel.CRITICAL })
    }

    @Test
    fun testAllowBackupFlagTriggersMediumPenalty() {
        val appInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_ALLOW_BACKUP
            targetSdkVersion = 34
            minSdkVersion = 26
        }

        val result = evaluator.evaluate(
            applicationInfo = appInfo,
            targetSdk = 34,
            minSdk = 26
        )

        assertEquals(12, result.score)
        assertEquals(RiskLevel.LOW, result.level)
        assertTrue(result.allowBackup)
        assertTrue(result.contributingFactors.any { it.id == "CFG_ALLOW_BACKUP" && it.severity == RiskLevel.MEDIUM })
    }

    @Test
    fun testCleartextTrafficAndLegacySdkPenalties() {
        val appInfo = ApplicationInfo().apply {
            flags = ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC
            targetSdkVersion = 26 // outdated < 28 (penalty 20)
            minSdkVersion = 21   // legacy < 23 (penalty 10)
        }

        val result = evaluator.evaluate(
            applicationInfo = appInfo,
            targetSdk = 26,
            minSdk = 21
        )

        // Cleartext (16) + TargetSDK < 28 (20) + MinSDK < 23 (10) = 46
        assertEquals(46, result.score)
        assertEquals(RiskLevel.MEDIUM, result.level)
        assertTrue(result.contributingFactors.any { it.id == "CFG_CLEARTEXT" })
        assertTrue(result.contributingFactors.any { it.id == "CFG_OUTDATED_TARGET_SDK" })
        assertTrue(result.contributingFactors.any { it.id == "CFG_OBSOLETE_MIN_SDK" })
    }
}
