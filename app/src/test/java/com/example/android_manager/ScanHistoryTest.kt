package com.example.android_manager

import com.example.android_manager.data.history.ScanHistoryConverters
import com.example.android_manager.data.history.ScanHistorySampleData
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanStatus
import com.example.android_manager.model.ScanType
import com.example.android_manager.model.VulnerabilityFinding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Scan History serialization, converters, and sample model consistency.
 */
class ScanHistoryTest {

    private val converters = ScanHistoryConverters()

    @Test
    fun testVulnerabilityFindingsSerializationRoundTrip() {
        val originalFindings = listOf(
            VulnerabilityFinding(
                name = "Insecure Data Storage",
                severity = RiskLevel.HIGH,
                affectedComponent = "com.example.SharedPrefs",
                evidence = "World readable shared preferences",
                impact = "Credentials readable by any app",
                recommendation = "Use EncryptedSharedPreferences"
            ),
            VulnerabilityFinding(
                name = "Exported Provider",
                severity = RiskLevel.CRITICAL,
                affectedComponent = "com.example.DataProvider",
                evidence = "android:exported=\"true\"",
                impact = "Data leak",
                recommendation = "Set exported false"
            )
        )

        val json = converters.fromVulnerabilityFindingList(originalFindings)
        assertNotNull(json)
        assertTrue(json.contains("Insecure Data Storage"))
        assertTrue(json.contains("CRITICAL"))

        val restored = converters.toVulnerabilityFindingList(json)
        assertEquals(2, restored.size)
        assertEquals(originalFindings[0].name, restored[0].name)
        assertEquals(originalFindings[0].severity, restored[0].severity)
        assertEquals(originalFindings[1].affectedComponent, restored[1].affectedComponent)
    }

    @Test
    fun testEmptyFindingsSerialization() {
        val emptyJson = converters.fromVulnerabilityFindingList(emptyList())
        assertEquals("[]", emptyJson)

        val restored = converters.toVulnerabilityFindingList(emptyJson)
        assertTrue(restored.isEmpty())

        val nullRestored = converters.toVulnerabilityFindingList(null)
        assertTrue(nullRestored.isEmpty())
    }

    @Test
    fun testEnumConverters() {
        // ScanType
        assertEquals(ScanType.APK_FILE, converters.toScanType(converters.fromScanType(ScanType.APK_FILE)))
        assertEquals(ScanType.SECURITY_CHECK, converters.toScanType(converters.fromScanType(ScanType.SECURITY_CHECK)))
        assertEquals(ScanType.APK_FILE, converters.toScanType("UNKNOWN_VALUE"))

        // RiskLevel
        assertEquals(RiskLevel.CRITICAL, converters.toRiskLevel(converters.fromRiskLevel(RiskLevel.CRITICAL)))
        assertEquals(RiskLevel.LOW, converters.toRiskLevel("INVALID"))

        // ScanStatus
        assertEquals(ScanStatus.THREAT_DETECTED, converters.toScanStatus(converters.fromScanStatus(ScanStatus.THREAT_DETECTED)))
        assertEquals(ScanStatus.CLEAN, converters.toScanStatus(null))
    }

    @Test
    fun testSampleDataIntegrity() {
        val samples = ScanHistorySampleData.sampleEntries
        assertTrue(samples.isNotEmpty())

        val criticalEntry = samples.first { it.riskLevel == RiskLevel.CRITICAL }
        assertEquals(ScanStatus.THREAT_DETECTED, criticalEntry.status)
        assertTrue(criticalEntry.criticalCount > 0)
        assertEquals(criticalEntry.findings.size, criticalEntry.findingsCount)

        val cleanEntry = samples.first { it.riskLevel == RiskLevel.LOW }
        assertEquals(ScanStatus.CLEAN, cleanEntry.status)
        assertEquals(0, cleanEntry.findingsCount)
    }
}
