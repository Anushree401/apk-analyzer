package com.example.android_manager.ui.report

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.ReportExecutiveSummary
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor

/**
 * Executive Summary Card presenting the primary security score gauge,
 * overall threat rating badge, and high-level posture narrative.
 */
@Composable
fun ReportSummaryCard(
    summary: ReportExecutiveSummary,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (summary.overallScore.coerceIn(0, 100) / 100f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "ReportScoreProgress"
    )

    val accentColor = summary.overallLevel.color()
    val badgeContainer = summary.overallLevel.containerColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(16.dp)
            )
            .background(Surface, RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EXECUTIVE POSTURE SUMMARY",
                color = SecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            // Threat Level Pill Badge
            Box(
                modifier = Modifier
                    .background(badgeContainer, RoundedCornerShape(20.dp))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = summary.overallLevel.name,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Circular Telemetry Gauge
        Box(
            modifier = Modifier.size(160.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val arcSize = Size(radius * 2, radius * 2)
                val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)

                // Background track arc
                drawArc(
                    color = BorderSubtle,
                    startAngle = 140f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // Active animated progress arc
                if (animatedProgress > 0f) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(CyberCyan, accentColor)
                        ),
                        startAngle = 140f,
                        sweepAngle = 260f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }

            // Gauge Center Content
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${summary.overallScore}",
                    color = PrimaryText,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "RISK SCORE",
                    color = SecondaryText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Posture Narrative Text
        Text(
            text = summary.summaryNarrative,
            color = PrimaryText,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}