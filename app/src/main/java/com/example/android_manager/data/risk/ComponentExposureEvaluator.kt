package com.example.android_manager.data.risk

import android.content.pm.PackageInfo
import com.example.android_manager.model.ComponentExposureBreakdown
import com.example.android_manager.model.RiskCategory
import com.example.android_manager.model.RiskFactor
import com.example.android_manager.model.RiskLevel

/**
 * Result of component exposure assessment.
 */
data class ComponentExposureResult(
    val score: Int,
    val level: RiskLevel,
    val breakdown: ComponentExposureBreakdown,
    val contributingFactors: List<RiskFactor>
)

/**
 * Evaluates the attack surface created by exposed Android application components
 * (Activities, Services, Broadcast Receivers, Content Providers).
 */
class ComponentExposureEvaluator {

    fun evaluate(packageInfo: PackageInfo): ComponentExposureResult {
        var exportedActivities = 0
        var exportedServices = 0
        var exportedReceivers = 0
        var exportedProviders = 0

        val unprotectedList = mutableListOf<String>()
        val factors = mutableListOf<RiskFactor>()
        var totalPenalty = 0

        // 1. Exported Activities
        packageInfo.activities?.forEach { activity ->
            if (activity.exported) {
                exportedActivities++
                val hasPermission = !activity.permission.isNullOrBlank()
                if (!hasPermission) {
                    val name = activity.name.substringAfterLast(".")
                    unprotectedList.add("Activity: $name")
                }
            }
        }

        // 2. Exported Services (Services carry higher risk than activities)
        packageInfo.services?.forEach { service ->
            if (service.exported) {
                exportedServices++
                val hasPermission = !service.permission.isNullOrBlank()
                if (!hasPermission) {
                    val name = service.name.substringAfterLast(".")
                    unprotectedList.add("Service: $name")
                    totalPenalty += 12
                    factors.add(
                        RiskFactor(
                            id = "EXP_SRV_${service.name}",
                            title = "Unprotected Exported Service: $name",
                            description = "Service is publicly accessible without permission protection, enabling third-party apps to trigger it.",
                            severity = RiskLevel.HIGH,
                            category = RiskCategory.COMPONENT_EXPOSURE,
                            scorePenalty = 12,
                            evidence = service.name
                        )
                    )
                } else {
                    totalPenalty += 4
                }
            }
        }

        // 3. Exported Broadcast Receivers
        packageInfo.receivers?.forEach { receiver ->
            if (receiver.exported) {
                exportedReceivers++
                val hasPermission = !receiver.permission.isNullOrBlank()
                if (!hasPermission) {
                    val name = receiver.name.substringAfterLast(".")
                    unprotectedList.add("Receiver: $name")
                    totalPenalty += 10
                    factors.add(
                        RiskFactor(
                            id = "EXP_RCV_${receiver.name}",
                            title = "Unprotected Broadcast Receiver: $name",
                            description = "Receiver can receive unsolicited intents from any malicious application on the device.",
                            severity = RiskLevel.MEDIUM,
                            category = RiskCategory.COMPONENT_EXPOSURE,
                            scorePenalty = 10,
                            evidence = receiver.name
                        )
                    )
                } else {
                    totalPenalty += 3
                }
            }
        }

        // 4. Exported Content Providers (Highest risk: directly leaks data or allows SQLi)
        packageInfo.providers?.forEach { provider ->
            if (provider.exported) {
                exportedProviders++
                val hasReadPerm = !provider.readPermission.isNullOrBlank()
                val hasWritePerm = !provider.writePermission.isNullOrBlank()
                if (!hasReadPerm || !hasWritePerm) {
                    val name = provider.name.substringAfterLast(".")
                    unprotectedList.add("Provider: $name")
                    totalPenalty += 18
                    factors.add(
                        RiskFactor(
                            id = "EXP_PRV_${provider.name}",
                            title = "Unprotected Content Provider: $name",
                            description = "Content Provider does not enforce read/write permission protection, risking database exfiltration.",
                            severity = RiskLevel.CRITICAL,
                            category = RiskCategory.COMPONENT_EXPOSURE,
                            scorePenalty = 18,
                            evidence = provider.name
                        )
                    )
                } else {
                    totalPenalty += 5
                }
            }
        }

        // Add activity exposure penalty if excessive
        if (exportedActivities > 3) {
            val actPenalty = (exportedActivities * 3).coerceAtMost(25)
            totalPenalty += actPenalty
            factors.add(
                RiskFactor(
                    id = "EXP_ACT_SURFACE",
                    title = "Broad Activity Attack Surface",
                    description = "$exportedActivities activities are exported to third-party intents.",
                    severity = if (exportedActivities > 8) RiskLevel.HIGH else RiskLevel.MEDIUM,
                    category = RiskCategory.COMPONENT_EXPOSURE,
                    scorePenalty = actPenalty,
                    evidence = "$exportedActivities exported activities"
                )
            )
        }

        val clampedScore = totalPenalty.coerceIn(0, 100)
        val level = RiskLevel.fromScore(clampedScore)

        val breakdown = ComponentExposureBreakdown(
            totalExportedActivities = exportedActivities,
            totalExportedServices = exportedServices,
            totalExportedReceivers = exportedReceivers,
            totalExportedProviders = exportedProviders,
            unprotectedComponents = unprotectedList
        )

        return ComponentExposureResult(
            score = clampedScore,
            level = level,
            breakdown = breakdown,
            contributingFactors = factors
        )
    }

    /**
     * Fallback estimator when only component counts or partial manifest is available.
     */
    fun evaluateRaw(
        exportedActivities: Int,
        exportedServices: Int,
        exportedReceivers: Int,
        exportedProviders: Int
    ): ComponentExposureResult {
        var penalty = 0
        penalty += (exportedActivities * 3)
        penalty += (exportedServices * 12)
        penalty += (exportedReceivers * 10)
        penalty += (exportedProviders * 20)

        val clampedScore = penalty.coerceIn(0, 100)
        val level = RiskLevel.fromScore(clampedScore)

        val factors = mutableListOf<RiskFactor>()
        if (exportedProviders > 0) {
            factors.add(
                RiskFactor(
                    id = "RAW_PRV",
                    title = "Exported Content Providers",
                    description = "$exportedProviders content provider(s) exposed to other apps.",
                    severity = RiskLevel.HIGH,
                    category = RiskCategory.COMPONENT_EXPOSURE,
                    scorePenalty = (exportedProviders * 20).coerceAtMost(40)
                )
            )
        }
        if (exportedServices > 0) {
            factors.add(
                RiskFactor(
                    id = "RAW_SRV",
                    title = "Exported Background Services",
                    description = "$exportedServices service(s) exposed across the system.",
                    severity = RiskLevel.MEDIUM,
                    category = RiskCategory.COMPONENT_EXPOSURE,
                    scorePenalty = (exportedServices * 12).coerceAtMost(30)
                )
            )
        }

        return ComponentExposureResult(
            score = clampedScore,
            level = level,
            breakdown = ComponentExposureBreakdown(
                totalExportedActivities = exportedActivities,
                totalExportedServices = exportedServices,
                totalExportedReceivers = exportedReceivers,
                totalExportedProviders = exportedProviders,
                unprotectedComponents = emptyList()
            ),
            contributingFactors = factors
        )
    }
}
