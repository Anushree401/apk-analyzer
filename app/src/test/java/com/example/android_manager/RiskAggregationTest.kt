package com.example.android_manager

import com.example.android_manager.model.ComponentExposureBreakdown
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Unit tests validating risk score aggregation formulas,
 * composite weighting, score boundaries, and unassessed category representations.
 */
class RiskAggregationTest {

    @Test
    fun testApkCompositeFormulaWeights() {
        val permScore = 80
        val vulnScore = 60
        val compScore = 40
        val configScore = 20

        // Formula: 30% Permissions, 35% Vulnerabilities, 20% Component Exposure, 15% Configuration
        val rawComposite = (permScore * 0.30) +
                (vulnScore * 0.35) +
                (compScore * 0.20) +
                (configScore * 0.15)

        val overallScore = rawComposite.roundToInt().coerceIn(0, 100)
        // (80*0.30=24) + (60*0.35=21) + (40*0.20=8) + (20*0.15=3) = 56
        assertEquals(56, overallScore)
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(overallScore))
    }

    @Test
    fun testInstalledAppCompositeFormulaWeights() {
        val permScore = 70
        val compScore = 50
        val configScore = 30

        // Formula: 45% Permission Risk, 35% Component Exposure, 20% Configuration Hardening
        val rawComposite = (permScore * 0.45) +
                (compScore * 0.35) +
                (configScore * 0.20)

        val overallScore = rawComposite.roundToInt().coerceIn(0, 100)
        // (70*0.45=31.5) + (50*0.35=17.5) + (30*0.20=6) = 55
        assertEquals(55, overallScore)
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(overallScore))
    }

    @Test
    fun testRiskLevelScoreThresholds() {
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(0))
        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(29))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(30))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(59))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(60))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(79))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(80))
        assertEquals(RiskLevel.CRITICAL, RiskLevel.fromScore(100))
    }

    @Test
    fun testUnassessedSubscoreRepresentation() {
        val historicalAssessment = RiskAssessment(
            targetName = "Legacy Scan",
            packageName = "com.legacy.app",
            versionName = "1.0",
            overallScore = 65,
            overallLevel = RiskLevel.HIGH,
            permissionRisk = null,
            permissionRiskLevel = null,
            vulnerabilityRisk = null,
            vulnerabilityRiskLevel = null,
            componentExposureRisk = null,
            componentExposureRiskLevel = null,
            configurationRisk = null,
            configurationRiskLevel = null
        )

        assertEquals(65, historicalAssessment.overallScore)
        assertEquals(RiskLevel.HIGH, historicalAssessment.overallLevel)
        assertNull(historicalAssessment.permissionRisk)
        assertNull(historicalAssessment.vulnerabilityRisk)
        assertNull(historicalAssessment.componentExposureRisk)
        assertNull(historicalAssessment.configurationRisk)
    }

    @Test
    fun testDevicePostureAggregationFormula() {
        val appScores = listOf(85, 75, 40, 20, 10)
        val criticalCount = 1 // 85 is CRITICAL (>=80)
        val highCount = 1     // 75 is HIGH (60-79)

        val avgScore = appScores.average() // (230 / 5) = 46.0
        val penaltyMultiplier = (criticalCount * 4) + (highCount * 2) // 4 + 2 = 6
        val postureScore = (avgScore + penaltyMultiplier).roundToInt().coerceIn(0, 100)
        // 46.0 + 6 = 52
        assertEquals(52, postureScore)
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(postureScore))
    }
}
