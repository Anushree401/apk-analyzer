package com.example.android_manager.model

/**
 * Category of security risks evaluated in APVM.
 */
enum class RiskCategory(val displayName: String) {
    PERMISSION("Permissions"),
    VULNERABILITY("Vulnerabilities"),
    COMPONENT_EXPOSURE("Component Exposure"),
    CONFIGURATION("App Hardening"),
    COMBINATION("Risky Combinations")
}

/**
 * Represents a single contributing factor that impacted the overall risk score.
 */
data class RiskFactor(
    val id: String,
    val title: String,
    val description: String,
    val severity: RiskLevel,
    val category: RiskCategory,
    val scorePenalty: Int,
    val evidence: String? = null
)

/**
 * Represents a dangerous combination of permissions detected together.
 * For example: RECORD_AUDIO + INTERNET (eavesdropping risk)
 * or READ_SMS + RECEIVE_BOOT_COMPLETED (background intercept risk).
 */
data class RiskCombination(
    val name: String,
    val permissions: List<String>,
    val dangerSummary: String,
    val severity: RiskLevel
)

/**
 * Counts and details of exported and unprotected Android components.
 */
data class ComponentExposureBreakdown(
    val totalExportedActivities: Int,
    val totalExportedServices: Int,
    val totalExportedReceivers: Int,
    val totalExportedProviders: Int,
    val unprotectedComponents: List<String> = emptyList()
) {
    val totalExported: Int
        get() = totalExportedActivities + totalExportedServices + totalExportedReceivers + totalExportedProviders
}

/**
 * Actionable security mitigation recommendation.
 */
data class RiskRecommendation(
    val id: String,
    val title: String,
    val action: String,
    val priority: RiskLevel,
    val targetComponent: String? = null
)

/**
 * Complete security risk assessment evaluation result for an application or APK.
 */
data class RiskAssessment(
    val targetName: String,
    val packageName: String,
    val versionName: String?,
    val overallScore: Int,
    val overallLevel: RiskLevel,
    val permissionRisk: Int,
    val permissionRiskLevel: RiskLevel,
    val vulnerabilityRisk: Int,
    val vulnerabilityRiskLevel: RiskLevel,
    val componentExposureRisk: Int,
    val componentExposureRiskLevel: RiskLevel,
    val configurationRisk: Int,
    val configurationRiskLevel: RiskLevel,
    val contributingFactors: List<RiskFactor> = emptyList(),
    val detectedCombinations: List<RiskCombination> = emptyList(),
    val componentBreakdown: ComponentExposureBreakdown = ComponentExposureBreakdown(0, 0, 0, 0),
    val recommendations: List<RiskRecommendation> = emptyList(),
    val isSystemApp: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Device-wide security posture aggregating security risks across installed applications.
 */
data class DeviceRiskPosture(
    val overallScore: Int,
    val overallLevel: RiskLevel,
    val totalAppsAudited: Int,
    val criticalAppsCount: Int,
    val highRiskAppsCount: Int,
    val mediumRiskAppsCount: Int,
    val lowRiskAppsCount: Int,
    val topRiskApps: List<RiskAssessment> = emptyList(),
    val mostCommonRiskyPermissions: List<Pair<String, Int>> = emptyList(),
    val timestamp: Long = System.currentTimeMillis()
)