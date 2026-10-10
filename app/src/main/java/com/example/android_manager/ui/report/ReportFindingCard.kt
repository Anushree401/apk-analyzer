package com.example.android_manager.ui.report

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.VulnerabilityFinding
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.RiskLow
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor

/**
 * Expandable Vulnerability Finding Card presenting finding severity badge,
 * vulnerability title, affected component tag, and detailed accordion sections for
 * Technical Evidence, Threat Impact, and Developer Mitigation.
 */
@Composable
fun ReportFindingCard(
    finding: VulnerabilityFinding,
    initialExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    val severityColor = finding.severity.color()
    val severityContainer = finding.severity.containerColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isExpanded) severityColor.copy(alpha = 0.4f) else Border,
                shape = RoundedCornerShape(12.dp)
            )
            .background(CardBackground, RoundedCornerShape(12.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(16.dp)
    ) {
        // Top Row: Severity Badge, Component, and Expand Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(severityContainer, RoundedCornerShape(4.dp))
                        .border(1.dp, severityColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = finding.severity.name,
                        color = severityColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }

                if (!finding.affectedComponent.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .background(BorderSubtle, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = finding.affectedComponent.substringAfterLast("."),
                            color = SecondaryText,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    }
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = SecondaryText
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Vulnerability Title
        Text(
            text = finding.name,
            color = PrimaryText,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        // Full component identifier if present
        if (!finding.affectedComponent.isNullOrBlank() && finding.affectedComponent.contains(".")) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = finding.affectedComponent,
                color = SecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Expandable Detailed Evidence, Impact, and Mitigation Sections
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
            ) {
                // Evidence Section
                if (finding.evidence.isNotBlank()) {
                    ExplanationSection(
                        label = "TECHNICAL EVIDENCE",
                        labelColor = CyberCyan
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Surface, RoundedCornerShape(8.dp))
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = finding.evidence,
                                color = PrimaryText,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 16.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Security Impact Section
                if (finding.impact.isNotBlank()) {
                    ExplanationSection(
                        label = "SECURITY IMPACT",
                        labelColor = severityColor
                    ) {
                        Text(
                            text = finding.impact,
                            color = PrimaryText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Mitigation Recommendation Section
                if (finding.recommendation.isNotBlank()) {
                    ExplanationSection(
                        label = "RECOMMENDED MITIGATION",
                        labelColor = RiskLow
                    ) {
                        Text(
                            text = finding.recommendation,
                            color = PrimaryText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExplanationSection(
    label: String,
    labelColor: androidx.compose.ui.graphics.Color,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = labelColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}