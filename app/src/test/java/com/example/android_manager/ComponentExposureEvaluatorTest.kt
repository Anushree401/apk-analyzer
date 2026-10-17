package com.example.android_manager

import com.example.android_manager.data.risk.ComponentExposureEvaluator
import com.example.android_manager.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests validating ComponentExposureEvaluator raw evaluation,
 * severity thresholds, and score clamping.
 */
class ComponentExposureEvaluatorTest {

    private val evaluator = ComponentExposureEvaluator()

    @Test
    fun testZeroExposedComponentsProduceZeroRisk() {
        val result = evaluator.evaluateRaw(
            exportedActivities = 0,
            exportedServices = 0,
            exportedReceivers = 0,
            exportedProviders = 0
        )
        assertEquals(0, result.score)
        assertEquals(RiskLevel.LOW, result.level)
        assertEquals(0, result.breakdown.totalExported)
        assertTrue(result.contributingFactors.isEmpty())
    }

    @Test
    fun testIndividualComponentPenalties() {
        // 1 exported Provider (penalty 20)
        val providerResult = evaluator.evaluateRaw(
            exportedActivities = 0,
            exportedServices = 0,
            exportedReceivers = 0,
            exportedProviders = 1
        )
        assertEquals(20, providerResult.score)
        assertEquals(RiskLevel.LOW, providerResult.level)
        assertTrue(providerResult.contributingFactors.any { it.id == "RAW_PRV" })

        // 1 exported Service (penalty 12)
        val serviceResult = evaluator.evaluateRaw(
            exportedActivities = 0,
            exportedServices = 1,
            exportedReceivers = 0,
            exportedProviders = 0
        )
        assertEquals(12, serviceResult.score)
        assertEquals(RiskLevel.LOW, serviceResult.level)
        assertTrue(serviceResult.contributingFactors.any { it.id == "RAW_SRV" })
    }

    @Test
    fun testMultipleComponentsAccumulationAndClamping() {
        // 10 activities (30) + 5 services (60) + 5 receivers (50) + 3 providers (60) = 200 -> clamped to 100
        val result = evaluator.evaluateRaw(
            exportedActivities = 10,
            exportedServices = 5,
            exportedReceivers = 5,
            exportedProviders = 3
        )
        assertEquals(100, result.score)
        assertEquals(RiskLevel.CRITICAL, result.level)
        assertEquals(23, result.breakdown.totalExported)
    }
}
