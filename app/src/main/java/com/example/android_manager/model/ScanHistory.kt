package com.example.android_manager.model

/**
 * Represents a historical record of an APK analysis or security scan.
 */
data class ScanHistoryEntry(
    val id: Long = 0,
    val scanType: ScanType = ScanType.APK_FILE,
    val targetName: String,
    val packageName: String,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val status: ScanStatus = ScanStatus.CLEAN,
    val findingsCount: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val findings: List<VulnerabilityFinding> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val scanDurationMs: Long = 0L
)