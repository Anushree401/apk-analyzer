package com.example.android_manager.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.DeviceRiskPosture
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskCritical
import com.example.android_manager.ui.theme.RiskHigh
import com.example.android_manager.ui.theme.RiskLow
import com.example.android_manager.ui.theme.RiskMedium
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor

/**
 * Tab content displaying overall device-wide risk posture.
 */
@Composable
fun DevicePostureTabContent(
    posture: DeviceRiskPosture?,
    onSelectApp: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (posture == null) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .background(Surface, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "CALCULATING DEVICE POSTURE...",
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Auditing installed apps across security metrics.",
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Overall Device Gauge Card
        RiskScoreCard(
            score = posture.overallScore,
            riskLevel = posture.overallLevel,
            label = "DEVICE SECURITY POSTURE",
            targetName = "${posture.totalAppsAudited} Installed Apps Audited"
        )

        // Risk Distribution Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .background(Surface, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "RISK SEVERITY DISTRIBUTION",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PostureDistributionTile(
                    count = posture.criticalAppsCount,
                    label = "CRITICAL",
                    color = RiskCritical,
                    modifier = Modifier.weight(1f)
                )
                PostureDistributionTile(
                    count = posture.highRiskAppsCount,
                    label = "HIGH",
                    color = RiskHigh,
                    modifier = Modifier.weight(1f)
                )
                PostureDistributionTile(
                    count = posture.mediumRiskAppsCount,
                    label = "MEDIUM",
                    color = RiskMedium,
                    modifier = Modifier.weight(1f)
                )
                PostureDistributionTile(
                    count = posture.lowRiskAppsCount,
                    label = "LOW",
                    color = RiskLow,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Top Risk Apps List
        if (posture.topRiskApps.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .background(Surface, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "HIGHEST RISK APPLICATIONS",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )

                posture.topRiskApps.forEach { app ->
                    val color = app.overallLevel.color()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                            .background(CardBackground, RoundedCornerShape(10.dp))
                            .clickable { onSelectApp(app.packageName) }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = app.targetName,
                                color = PrimaryText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = app.packageName,
                                color = SecondaryText,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${app.overallScore}/100",
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .background(app.overallLevel.containerColor(), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = app.overallLevel.name,
                                    color = color,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Frequent Sensitive Permissions
        if (posture.mostCommonRiskyPermissions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Border, RoundedCornerShape(16.dp))
                    .background(Surface, RoundedCornerShape(16.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "TOP SENSITIVE PERMISSION FREQUENCIES",
                    color = SecondaryText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp
                )

                posture.mostCommonRiskyPermissions.forEach { (perm, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = perm,
                            color = PrimaryText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "$count apps",
                            color = CyberCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostureDistributionTile(
    count: Int,
    label: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .background(CardBackground, RoundedCornerShape(8.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "$count",
            color = if (count > 0) color else MutedText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Tab explaining the APVM Risk Matrix, weighting formula, and severity classifications.
 */
@Composable
fun RiskMatrixTabContent(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Scoring Formula Explanation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .background(Surface, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "APVM RISK SCORING METHODOLOGY",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "The overall security risk score (0-100) is evaluated using a weighted multi-dimensional formula combining permission vectors, static vulnerability findings, exposed components, and configuration hardening.",
                color = PrimaryText,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            MatrixWeightRow(label = "Static Vulnerabilities", weight = "35%", color = CyberCyan)
            MatrixWeightRow(label = "Permission Exposure & Combos", weight = "30%", color = RiskHigh)
            MatrixWeightRow(label = "Component Attack Surface", weight = "20%", color = RiskMedium)
            MatrixWeightRow(label = "Manifest Hardening & SDK", weight = "15%", color = RiskLow)
        }

        // Risk Level Classifications
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Border, RoundedCornerShape(16.dp))
                .background(Surface, RoundedCornerShape(16.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SEVERITY CLASSIFICATION TIERS",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            RiskTierRow(
                level = RiskLevel.CRITICAL,
                range = "80 - 100",
                desc = "Urgent vulnerabilities, dangerous permission combinations, or unprotected components enabling arbitrary code execution or data theft."
            )

            RiskTierRow(
                level = RiskLevel.HIGH,
                range = "60 - 79",
                desc = "Elevated exposure such as unencrypted HTTP, excessive sensitive permissions, or exported services without permission controls."
            )

            RiskTierRow(
                level = RiskLevel.MEDIUM,
                range = "30 - 59",
                desc = "Moderate risk factors including ADB backup enabled, outdated target SDK, or standard dangerous runtime permissions."
            )

            RiskTierRow(
                level = RiskLevel.LOW,
                range = "0 - 29",
                desc = "Nominal attack surface with standard protected components and appropriate permission requests."
            )
        }
    }
}

@Composable
private fun MatrixWeightRow(
    label: String,
    weight: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = SecondaryText, fontSize = 12.sp)
        Text(
            text = weight,
            color = color,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RiskTierRow(
    level: RiskLevel,
    range: String,
    desc: String
) {
    val color = level.color()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
            .background(CardBackground, RoundedCornerShape(10.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .background(level.containerColor(), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = level.name,
                    color = color,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "Score $range",
                color = PrimaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = desc,
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )
    }
}
