package com.example.android_manager.model

import com.example.android_manager.data.permissions.PermissionEntry

/**
 * Metadata identifying the analyzed application or APK.
 */
data class ReportAppMetadata(
    val appName: String,
    val packageName: String,
    val versionName: String? = null,
    val versionCode: Long? = null,
    val minSdk: Int? = null,
    val targetSdk: Int? = null,
    val isSystemApp: Boolean = false,
    val scanType: ScanType = ScanType.APK_FILE,
    val scanTimestamp: Long = System.currentTimeMillis()
)

/**
 * Core security report model representing an application or APK security evaluation.
 */
data class SecurityReport(
    val id: String = java.util.UUID.randomUUID().toString(),
    val metadata: ReportAppMetadata,
    val riskAssessment: RiskAssessment,
    val vulnerabilityFindings: List<VulnerabilityFinding> = emptyList(),
    val dangerousPermissions: List<PermissionEntry> = emptyList(),
    val specialPermissions: List<PermissionEntry> = emptyList(),
    val allPermissions: List<PermissionEntry> = emptyList(),
    val detectedCombinations: List<RiskCombination> = emptyList(),
    val componentBreakdown: ComponentExposureBreakdown = ComponentExposureBreakdown(0, 0, 0, 0),
    val recommendations: List<RiskRecommendation> = emptyList()
)