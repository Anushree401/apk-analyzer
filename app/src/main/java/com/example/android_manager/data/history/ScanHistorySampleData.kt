package com.example.android_manager.data.history

import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.model.ScanStatus
import com.example.android_manager.model.ScanType
import com.example.android_manager.model.VulnerabilityFinding

/**
 * Provides representative sample data for Compose previews and testing.
 */
object ScanHistorySampleData {

    val sampleFindings = listOf(
        VulnerabilityFinding(
            name = "Exported Activity Without Permission",
            severity = RiskLevel.CRITICAL,
            affectedComponent = "com.evilcorp.bank.PaymentActivity",
            evidence = "android:exported=\"true\" without android:permission restriction",
            impact = "Any third-party application can launch payment processing directly",
            recommendation = "Set android:exported=\"false\" or enforce a signature-level permission"
        ),
        VulnerabilityFinding(
            name = "Cleartext HTTP Traffic Allowed",
            severity = RiskLevel.HIGH,
            affectedComponent = "AndroidManifest.xml",
            evidence = "android:usesCleartextTraffic=\"true\"",
            impact = "Network traffic is vulnerable to Man-In-The-Middle interception",
            recommendation = "Configure a Network Security Config restricting traffic to HTTPS"
        ),
        VulnerabilityFinding(
            name = "Debuggable Flag Enabled",
            severity = RiskLevel.HIGH,
            affectedComponent = "AndroidManifest.xml",
            evidence = "android:debuggable=\"true\"",
            impact = "Attackers can attach JDWP debuggers to dump app memory and credentials",
            recommendation = "Ensure android:debuggable is false in release builds"
        ),
        VulnerabilityFinding(
            name = "Implicit Intent Receiver",
            severity = RiskLevel.MEDIUM,
            affectedComponent = "com.example.receiver.AuthReceiver",
            evidence = "BroadcastReceiver with broad intent-filter action",
            impact = "Susceptible to intent spoofing and unauthorized triggers",
            recommendation = "Protect the receiver with custom permission or require exact component names"
        ),
        VulnerabilityFinding(
            name = "Legacy Target SDK",
            severity = RiskLevel.LOW,
            affectedComponent = "build.gradle",
            evidence = "targetSdkVersion = 28",
            impact = "App misses modern Android platform privacy and sandbox protections",
            recommendation = "Upgrade targetSdkVersion to 34 or higher"
        )
    )

    val sampleEntries = listOf(
        ScanHistoryEntry(
            id = 1,
            scanType = ScanType.APK_FILE,
            targetName = "BankingApp_v2.4.apk",
            packageName = "com.sample.banking.app",
            versionName = "2.4.0",
            versionCode = 240,
            minSdk = 24,
            targetSdk = 34,
            riskScore = 88,
            riskLevel = RiskLevel.CRITICAL,
            status = ScanStatus.THREAT_DETECTED,
            findingsCount = 3,
            criticalCount = 1,
            highCount = 2,
            mediumCount = 0,
            lowCount = 0,
            findings = sampleFindings.take(3),
            timestamp = System.currentTimeMillis() - 1000 * 60 * 15, // 15 mins ago
            scanDurationMs = 520
        ),
        ScanHistoryEntry(
            id = 2,
            scanType = ScanType.APK_FILE,
            targetName = "SocialFeed_Mod.apk",
            packageName = "org.modded.social.feed",
            versionName = "1.0.5",
            versionCode = 105,
            minSdk = 26,
            targetSdk = 33,
            riskScore = 65,
            riskLevel = RiskLevel.HIGH,
            status = ScanStatus.THREAT_DETECTED,
            findingsCount = 2,
            criticalCount = 0,
            highCount = 1,
            mediumCount = 1,
            lowCount = 0,
            findings = listOf(sampleFindings[1], sampleFindings[3]),
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 3, // 3 hours ago
            scanDurationMs = 380
        ),
        ScanHistoryEntry(
            id = 3,
            scanType = ScanType.SECURITY_CHECK,
            targetName = "Messenger_Lite.apk",
            packageName = "com.chat.messenger.lite",
            versionName = "3.1.2",
            versionCode = 312,
            minSdk = 21,
            targetSdk = 34,
            riskScore = 35,
            riskLevel = RiskLevel.MEDIUM,
            status = ScanStatus.SUSPICIOUS,
            findingsCount = 1,
            criticalCount = 0,
            highCount = 0,
            mediumCount = 1,
            lowCount = 0,
            findings = listOf(sampleFindings[3]),
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 26, // 1 day ago
            scanDurationMs = 290
        ),
        ScanHistoryEntry(
            id = 4,
            scanType = ScanType.APK_FILE,
            targetName = "Authenticator_Pro.apk",
            packageName = "net.auth.totp.pro",
            versionName = "4.0.0",
            versionCode = 400,
            minSdk = 28,
            targetSdk = 35,
            riskScore = 5,
            riskLevel = RiskLevel.LOW,
            status = ScanStatus.CLEAN,
            findingsCount = 0,
            criticalCount = 0,
            highCount = 0,
            mediumCount = 0,
            lowCount = 0,
            findings = emptyList(),
            timestamp = System.currentTimeMillis() - 1000 * 60 * 60 * 72, // 3 days ago
            scanDurationMs = 210
        )
    )
}
