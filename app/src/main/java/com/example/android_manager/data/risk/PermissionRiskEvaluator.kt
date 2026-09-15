package com.example.android_manager.data.risk

import com.example.android_manager.model.RiskCategory
import com.example.android_manager.model.RiskCombination
import com.example.android_manager.model.RiskFactor
import com.example.android_manager.model.RiskLevel

/**
 * Result of evaluating permission risks for an application or APK.
 */
data class PermissionRiskResult(
    val score: Int,
    val level: RiskLevel,
    val dangerousCount: Int,
    val specialCount: Int,
    val totalCount: Int,
    val contributingFactors: List<RiskFactor>,
    val combinations: List<RiskCombination>
)

/**
 * Evaluates requested Android permissions, identifying high-risk permissions,
 * privilege abuse vectors, and malicious permission combinations.
 */
class PermissionRiskEvaluator {

    private data class CombinationRule(
        val name: String,
        val requiredPermissions: Set<String>,
        val dangerSummary: String,
        val severity: RiskLevel,
        val penalty: Int
    )

    private val riskyCombinationRules = listOf(
        CombinationRule(
            name = "Audio Surveillance Vector",
            requiredPermissions = setOf("RECORD_AUDIO", "INTERNET"),
            dangerSummary = "App can record microphone audio and exfiltrate data over the network without user awareness.",
            severity = RiskLevel.HIGH,
            penalty = 22
        ),
        CombinationRule(
            name = "Camera & Audio Exfiltration",
            requiredPermissions = setOf("CAMERA", "RECORD_AUDIO", "INTERNET"),
            dangerSummary = "Simultaneous camera and microphone capture capability combined with outbound internet access.",
            severity = RiskLevel.CRITICAL,
            penalty = 30
        ),
        CombinationRule(
            name = "Silent SMS Intercept & Fraud",
            requiredPermissions = setOf("RECEIVE_SMS", "RECEIVE_BOOT_COMPLETED"),
            dangerSummary = "App can monitor incoming two-factor authentication SMS codes immediately on device boot.",
            severity = RiskLevel.CRITICAL,
            penalty = 28
        ),
        CombinationRule(
            name = "Outgoing SMS Toll Fraud",
            requiredPermissions = setOf("SEND_SMS", "RECEIVE_BOOT_COMPLETED"),
            dangerSummary = "Can initiate premium SMS messages in the background without explicit real-time consent.",
            severity = RiskLevel.CRITICAL,
            penalty = 26
        ),
        CombinationRule(
            name = "Stealth Background Tracking",
            requiredPermissions = setOf("ACCESS_FINE_LOCATION", "ACCESS_BACKGROUND_LOCATION", "INTERNET"),
            dangerSummary = "Continuous covert GPS tracking even when application is closed, exfiltrating location to remote servers.",
            severity = RiskLevel.HIGH,
            penalty = 24
        ),
        CombinationRule(
            name = "Trojan Dropper Vector",
            requiredPermissions = setOf("REQUEST_INSTALL_PACKAGES", "INTERNET"),
            dangerSummary = "Can download arbitrary secondary APK payloads from internet and prompt for silent or coerced installation.",
            severity = RiskLevel.HIGH,
            penalty = 25
        ),
        CombinationRule(
            name = "Clickjacking & Overlay Attack",
            requiredPermissions = setOf("SYSTEM_ALERT_WINDOW", "BIND_ACCESSIBILITY_SERVICE"),
            dangerSummary = "Capable of drawing invisible screen overlays over banking/login apps and harvesting touch interactions.",
            severity = RiskLevel.CRITICAL,
            penalty = 32
        ),
        CombinationRule(
            name = "Contact Book & Identity Harvesting",
            requiredPermissions = setOf("READ_CONTACTS", "READ_PHONE_STATE", "INTERNET"),
            dangerSummary = "App can access complete address book and persistent device identifiers and transfer them externally.",
            severity = RiskLevel.HIGH,
            penalty = 20
        ),
        CombinationRule(
            name = "Call Log & Telephony Monitoring",
            requiredPermissions = setOf("READ_CALL_LOG", "READ_PHONE_STATE", "INTERNET"),
            dangerSummary = "Full history of dialed and received phone numbers can be tracked and synced with external servers.",
            severity = RiskLevel.HIGH,
            penalty = 18
        )
    )

    private val specialPermissionWeights = mapOf(
        "SYSTEM_ALERT_WINDOW" to Pair(RiskLevel.HIGH, 18),
        "REQUEST_INSTALL_PACKAGES" to Pair(RiskLevel.HIGH, 20),
        "WRITE_SETTINGS" to Pair(RiskLevel.HIGH, 15),
        "PACKAGE_USAGE_STATS" to Pair(RiskLevel.HIGH, 16),
        "MANAGE_EXTERNAL_STORAGE" to Pair(RiskLevel.HIGH, 18),
        "ACCESS_NOTIFICATION_POLICY" to Pair(RiskLevel.MEDIUM, 10),
        "BIND_ACCESSIBILITY_SERVICE" to Pair(RiskLevel.CRITICAL, 26),
        "BIND_DEVICE_ADMIN" to Pair(RiskLevel.CRITICAL, 28)
    )

    private val dangerousPermissionWeights = mapOf(
        "READ_SMS" to Pair(RiskLevel.HIGH, 16),
        "RECEIVE_SMS" to Pair(RiskLevel.HIGH, 16),
        "SEND_SMS" to Pair(RiskLevel.HIGH, 18),
        "READ_CALL_LOG" to Pair(RiskLevel.HIGH, 14),
        "WRITE_CALL_LOG" to Pair(RiskLevel.HIGH, 14),
        "RECORD_AUDIO" to Pair(RiskLevel.HIGH, 15),
        "CAMERA" to Pair(RiskLevel.HIGH, 14),
        "ACCESS_FINE_LOCATION" to Pair(RiskLevel.MEDIUM, 12),
        "ACCESS_BACKGROUND_LOCATION" to Pair(RiskLevel.HIGH, 16),
        "READ_CONTACTS" to Pair(RiskLevel.MEDIUM, 12),
        "WRITE_CONTACTS" to Pair(RiskLevel.MEDIUM, 12),
        "READ_CALENDAR" to Pair(RiskLevel.LOW, 8),
        "READ_PHONE_STATE" to Pair(RiskLevel.MEDIUM, 10),
        "CALL_PHONE" to Pair(RiskLevel.HIGH, 14),
        "USE_SIP" to Pair(RiskLevel.MEDIUM, 10),
        "BODY_SENSORS" to Pair(RiskLevel.LOW, 8)
    )

    fun evaluate(rawPermissionNames: Collection<String>): PermissionRiskResult {
        val normalized = rawPermissionNames.map { it.substringAfterLast(".").uppercase() }.toSet()
        val factors = mutableListOf<RiskFactor>()
        val combinations = mutableListOf<RiskCombination>()

        var totalPenalty = 0
        var dangerousCount = 0
        var specialCount = 0

        // 1. Evaluate Special / High Privilege Permissions
        specialPermissionWeights.forEach { (perm, weight) ->
            if (normalized.contains(perm)) {
                specialCount++
                val (severity, penalty) = weight
                totalPenalty += penalty
                factors.add(
                    RiskFactor(
                        id = "SPEC_$perm",
                        title = "Special Permission: $perm",
                        description = "Requested system-level permission granting elevated capabilities that bypass standard sandbox restrictions.",
                        severity = severity,
                        category = RiskCategory.PERMISSION,
                        scorePenalty = penalty,
                        evidence = perm
                    )
                )
            }
        }

        // 2. Evaluate Dangerous Runtime Permissions
        dangerousPermissionWeights.forEach { (perm, weight) ->
            if (normalized.contains(perm)) {
                dangerousCount++
                val (severity, penalty) = weight
                totalPenalty += penalty
                factors.add(
                    RiskFactor(
                        id = "DANG_$perm",
                        title = "Dangerous Permission: $perm",
                        description = "Direct access to private user data or sensitive hardware capabilities.",
                        severity = severity,
                        category = RiskCategory.PERMISSION,
                        scorePenalty = penalty,
                        evidence = perm
                    )
                )
            }
        }

        // 3. Evaluate Risky Permission Combinations
        riskyCombinationRules.forEach { rule ->
            val hasAll = rule.requiredPermissions.all { perm -> normalized.contains(perm) }
            if (hasAll) {
                totalPenalty += rule.penalty
                combinations.add(
                    RiskCombination(
                        name = rule.name,
                        permissions = rule.requiredPermissions.toList(),
                        dangerSummary = rule.dangerSummary,
                        severity = rule.severity
                    )
                )
                factors.add(
                    RiskFactor(
                        id = "COMB_${rule.name.replace(" ", "_")}",
                        title = "Risky Combo: ${rule.name}",
                        description = rule.dangerSummary,
                        severity = rule.severity,
                        category = RiskCategory.COMBINATION,
                        scorePenalty = rule.penalty,
                        evidence = rule.requiredPermissions.joinToString(" + ")
                    )
                )
            }
        }

        // 4. Excessive permission count penalty
        if (dangerousCount >= 6) {
            val excessivePenalty = 15
            totalPenalty += excessivePenalty
            factors.add(
                RiskFactor(
                    id = "EXCESSIVE_DANGEROUS",
                    title = "Excessive Sensitive Permissions",
                    description = "Application requests an unusually large surface of $dangerousCount dangerous permissions.",
                    severity = RiskLevel.HIGH,
                    category = RiskCategory.PERMISSION,
                    scorePenalty = excessivePenalty,
                    evidence = "$dangerousCount dangerous permissions requested"
                )
            )
        }

        val clampedScore = totalPenalty.coerceIn(0, 100)
        val level = RiskLevel.fromScore(clampedScore)

        return PermissionRiskResult(
            score = clampedScore,
            level = level,
            dangerousCount = dangerousCount,
            specialCount = specialCount,
            totalCount = rawPermissionNames.size,
            contributingFactors = factors,
            combinations = combinations
        )
    }
}
