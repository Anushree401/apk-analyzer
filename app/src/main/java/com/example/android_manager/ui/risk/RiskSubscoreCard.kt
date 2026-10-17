
package com.example.android_manager.ui.risk

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color

/**
 * Multi-dimensional sub-score breakdown card displaying individual risk vectors.
 */
@Composable
fun RiskSubscoreCard(
    assessment: RiskAssessment,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "RISK VECTOR BREAKDOWN",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "4 PILLARS",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }

        SubscoreItem(
            title = "Permission Risk",
            score = assessment.permissionRisk,
            level = assessment.permissionRiskLevel,
            description = "Dangerous runtime access & privileged APIs"
        )

        SubscoreItem(
            title = "Vulnerability Risk",
            score = assessment.vulnerabilityRisk,
            level = assessment.vulnerabilityRiskLevel,
            description = "Static code flaws & insecure implementation"
        )

        SubscoreItem(
            title = "Component Exposure",
            score = assessment.componentExposureRisk,
            level = assessment.componentExposureRiskLevel,
            description = "Exported activities, services & providers"
        )

        SubscoreItem(
            title = "Configuration Hardening",
            score = assessment.configurationRisk,
            level = assessment.configurationRiskLevel,
            description = "Manifest flags, backup, cleartext & SDK target"
        )
    }
}

@Composable
private fun SubscoreItem(
    title: String,
    score: Int?,
    level: RiskLevel?,
    description: String,
    modifier: Modifier = Modifier
) {
    val assessed = score != null && level != null
    val safeScore = score?.coerceIn(0, 100) ?: 0

    val animatedProgress by animateFloatAsState(
        targetValue = if (assessed) safeScore / 100f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "SubscoreBar"
    )

    val levelColor = level?.color() ?: SecondaryText

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = PrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (assessed) "$safeScore/100" else "N/A",
                    color = PrimaryText,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = if (assessed) {
                        level!!.name
                    } else {
                        "NOT ASSESSED"
                    },
                    color = levelColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(BorderSubtle)
        ) {
            if (assessed) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(3.dp))
                        .background(levelColor)
                )
            }
        }

        Text(
            text = if (assessed) {
                description
            } else {
                "$description. This category was not assessed for this scan."
            },
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    }
}
