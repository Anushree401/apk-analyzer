package com.example.android_manager.ui.risk

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.example.android_manager.model.ComponentExposureBreakdown
import com.example.android_manager.model.RiskCombination
import com.example.android_manager.model.RiskRecommendation
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskCritical
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor

/**
 * Card displaying detected dangerous multi-permission combinations.
 */
@Composable
fun RiskCombinationsCard(
    combinations: List<RiskCombination>,
    modifier: Modifier = Modifier
) {
    if (combinations.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CRITICAL PERMISSION COMBINATIONS",
                color = RiskCritical,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "${combinations.size} DETECTED",
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        combinations.forEach { combo ->
            val color = combo.severity.color()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .background(CardBackground, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = combo.name,
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Box(
                        modifier = Modifier
                            .background(combo.severity.containerColor(), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = combo.severity.name,
                            color = color,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = combo.dangerSummary,
                    color = SecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                // Permissions badges row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    combo.permissions.forEach { perm ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, BorderSubtle, RoundedCornerShape(4.dp))
                                .background(Surface, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = perm,
                                color = PrimaryText,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card detailing exported Android components breakdown and attack surface.
 */
@Composable
fun ComponentExposureDetailCard(
    breakdown: ComponentExposureBreakdown,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COMPONENT EXPOSURE SURFACE",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "${breakdown.totalExported} EXPORTED",
                color = PrimaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Component count grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ComponentStatTile(
                count = breakdown.totalExportedActivities,
                label = "Activities",
                modifier = Modifier.weight(1f)
            )
            ComponentStatTile(
                count = breakdown.totalExportedServices,
                label = "Services",
                modifier = Modifier.weight(1f)
            )
            ComponentStatTile(
                count = breakdown.totalExportedReceivers,
                label = "Receivers",
                modifier = Modifier.weight(1f)
            )
            ComponentStatTile(
                count = breakdown.totalExportedProviders,
                label = "Providers",
                modifier = Modifier.weight(1f)
            )
        }

        if (breakdown.unprotectedComponents.isNotEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Unprotected Entry Points:",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                breakdown.unprotectedComponents.take(5).forEach { item ->
                    Text(
                        text = "• $item",
                        color = PrimaryText,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
private fun ComponentStatTile(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
            .background(CardBackground, RoundedCornerShape(8.dp))
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "$count",
            color = if (count > 0) PrimaryText else MutedText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = SecondaryText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

/**
 * Card displaying actionable security recommendations.
 */
@Composable
fun RiskRecommendationsCard(
    recommendations: List<RiskRecommendation>,
    modifier: Modifier = Modifier
) {
    if (recommendations.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Border, RoundedCornerShape(16.dp))
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MITIGATION RECOMMENDATIONS",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "${recommendations.size} ACTIONS",
                color = SecondaryText,
                fontSize = 11.sp
            )
        }

        recommendations.forEach { rec ->
            val priorityColor = rec.priority.color()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .background(CardBackground, RoundedCornerShape(10.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rec.title,
                        color = PrimaryText,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )

                    Box(
                        modifier = Modifier
                            .background(rec.priority.containerColor(), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = rec.priority.name,
                            color = priorityColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = rec.action,
                    color = SecondaryText,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                if (!rec.targetComponent.isNullOrBlank()) {
                    Text(
                        text = "Target: ${rec.targetComponent}",
                        color = Graphite,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
