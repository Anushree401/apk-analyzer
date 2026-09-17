package com.example.android_manager.data.risk

import android.content.pm.ApplicationInfo
import com.example.android_manager.model.RiskCategory
import com.example.android_manager.model.RiskFactor
import com.example.android_manager.model.RiskLevel

/**
 * Result of manifest configuration and hardening evaluation.
 */
data class ConfigurationRiskResult(
    val score: Int,
    val level: RiskLevel,
    val isDebuggable: Boolean,
    val allowBackup: Boolean,
    val usesCleartextTraffic: Boolean,
    val targetSdk: Int?,
    val minSdk: Int?,
    val contributingFactors: List<RiskFactor>
)

/**
 * Evaluates manifest hardening settings, flags, and SDK targets.
 */
class ConfigurationRiskEvaluator {

    fun evaluate(
        applicationInfo: ApplicationInfo,
        targetSdk: Int? = null,
        minSdk: Int? = null
    ): ConfigurationRiskResult {
        val factors = mutableListOf<RiskFactor>()
        var penalty = 0

        // 1. Debuggable flag (FLAG_DEBUGGABLE)
        val isDebuggable = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (isDebuggable) {
            val debugPenalty = 30
            penalty += debugPenalty
            factors.add(
                RiskFactor(
                    id = "CFG_DEBUGGABLE",
                    title = "Application is Debuggable",
                    description = "android:debuggable is enabled. Attackers can attach JDWP debuggers to extract memory and execute arbitrary code.",
                    severity = RiskLevel.CRITICAL,
                    category = RiskCategory.CONFIGURATION,
                    scorePenalty = debugPenalty,
                    evidence = "FLAG_DEBUGGABLE == true"
                )
            )
        }

        // 2. AllowBackup flag (FLAG_ALLOW_BACKUP)
        val allowBackup = (applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        if (allowBackup) {
            val backupPenalty = 12
            penalty += backupPenalty
            factors.add(
                RiskFactor(
                    id = "CFG_ALLOW_BACKUP",
                    title = "ADB Backup Enabled",
                    description = "android:allowBackup is enabled. Application sandbox databases and shared preferences can be dumped via ADB.",
                    severity = RiskLevel.MEDIUM,
                    category = RiskCategory.CONFIGURATION,
                    scorePenalty = backupPenalty,
                    evidence = "FLAG_ALLOW_BACKUP == true"
                )
            )
        }

        // 3. Cleartext Network Traffic
        val effectiveTargetSdk = targetSdk ?: applicationInfo.targetSdkVersion
        val usesCleartext = if (android.os.Build.VERSION.SDK_INT >= 24) {
            (applicationInfo.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        } else {
            true
        }

        if (usesCleartext && effectiveTargetSdk < 28) {
            val cleartextPenalty = 16
            penalty += cleartextPenalty
            factors.add(
                RiskFactor(
                    id = "CFG_CLEARTEXT",
                    title = "Unencrypted Cleartext HTTP Permitted",
                    description = "Application allows unencrypted cleartext HTTP communications, enabling eavesdropping and MITM attacks.",
                    severity = RiskLevel.HIGH,
                    category = RiskCategory.CONFIGURATION,
                    scorePenalty = cleartextPenalty,
                    evidence = "FLAG_USES_CLEARTEXT_TRAFFIC == true"
                )
            )
        }

        // 4. Target SDK Obsoleteness
        if (effectiveTargetSdk < 31) {
            val sdkPenalty = if (effectiveTargetSdk < 28) 20 else 12
            penalty += sdkPenalty
            factors.add(
                RiskFactor(
                    id = "CFG_OUTDATED_TARGET_SDK",
                    title = "Outdated Target SDK ($effectiveTargetSdk)",
                    description = "Target SDK $effectiveTargetSdk misses vital platform security mitigations introduced in modern Android versions.",
                    severity = if (effectiveTargetSdk < 28) RiskLevel.HIGH else RiskLevel.MEDIUM,
                    category = RiskCategory.CONFIGURATION,
                    scorePenalty = sdkPenalty,
                    evidence = "targetSdkVersion = $effectiveTargetSdk"
                )
            )
        }

        // 5. Min SDK Obsoleteness
        val effectiveMinSdk = minSdk ?: if (android.os.Build.VERSION.SDK_INT >= 24) applicationInfo.minSdkVersion else 21
        if (effectiveMinSdk < 23) {
            val minSdkPenalty = 10
            penalty += minSdkPenalty
            factors.add(
                RiskFactor(
                    id = "CFG_OBSOLETE_MIN_SDK",
                    title = "Legacy Min SDK ($effectiveMinSdk)",
                    description = "Supporting Android versions prior to 23 bypasses runtime permission consent mechanisms.",
                    severity = RiskLevel.LOW,
                    category = RiskCategory.CONFIGURATION,
                    scorePenalty = minSdkPenalty,
                    evidence = "minSdkVersion = $effectiveMinSdk"
                )
            )
        }

        val clampedScore = penalty.coerceIn(0, 100)
        val level = RiskLevel.fromScore(clampedScore)

        return ConfigurationRiskResult(
            score = clampedScore,
            level = level,
            isDebuggable = isDebuggable,
            allowBackup = allowBackup,
            usesCleartextTraffic = usesCleartext,
            targetSdk = effectiveTargetSdk,
            minSdk = effectiveMinSdk,
            contributingFactors = factors
        )
    }
}
