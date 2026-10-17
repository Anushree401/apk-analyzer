
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
        get() = totalExportedActivities +
                totalExportedServices +
                totalExportedReceivers +
                totalExportedProviders
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
 * Complete security risk assessment evaluation result.
 *
 * A null category score and null category level mean that category was
 * not assessed. They must not be interpreted as zero risk.
 */
data class RiskAssessment(
    val targetName: String,
    val packageName: String,
    val versionName: String?,
    val overallScore: Int,
    val overallLevel: RiskLevel,
    val permissionRisk: Int? = null,
    val permissionRiskLevel: RiskLevel? = null,
    val vulnerabilityRisk: Int? = null,
    val vulnerabilityRiskLevel: RiskLevel? = null,
    val componentExposureRisk: Int? = null,
    val componentExposureRiskLevel: RiskLevel? = null,
    val configurationRisk: Int? = null,
    val configurationRiskLevel: RiskLevel? = null,
    val contributingFactors: List<RiskFactor> = emptyList(),
    val detectedCombinations: List<RiskCombination> = emptyList(),
    val componentBreakdown: ComponentExposureBreakdown =
        ComponentExposureBreakdown(0, 0, 0, 0),
    val recommendations: List<RiskRecommendation> = emptyList(),
    val isSystemApp: Boolean = false,
    val minSdk: Int? = null,
    val targetSdk: Int? = null,
    val versionCode: Long? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Device-wide security posture across installed applications.
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
