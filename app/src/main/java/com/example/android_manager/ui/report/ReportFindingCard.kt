package com.example.android_manager.ui.report

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
import com.example.android_manager.model.VulnerabilityFinding
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor

/**
 * Vulnerability Finding Card presenting finding severity badge,
 * vulnerability title, affected component tag, and structured metadata.
 */
@Composable
fun ReportFindingCard(
    finding: VulnerabilityFinding,
    modifier: Modifier = Modifier
) {
    val severityColor = finding.severity.color()
    val severityContainer = finding.severity.containerColor()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(12.dp)
            )
            .background(CardBackground, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Top Row: Severity Badge and Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
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
    }
}