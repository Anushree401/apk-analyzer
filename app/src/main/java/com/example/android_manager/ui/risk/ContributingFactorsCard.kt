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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.model.RiskFactor
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.BorderSubtle
import com.example.android_manager.ui.theme.CardBackground
import com.example.android_manager.ui.theme.Graphite
import com.example.android_manager.ui.theme.MutedText
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface
import com.example.android_manager.ui.theme.color
import com.example.android_manager.ui.theme.containerColor

/**
 * Lists major security findings and permission risks that contribute to the composite score.
 */
@Composable
fun ContributingFactorsCard(
    factors: List<RiskFactor>,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf<RiskLevel?>(null) }

    val filteredFactors = remember(factors, selectedFilter) {
        if (selectedFilter == null) factors else factors.filter { it.severity == selectedFilter }
    }

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
                text = "CONTRIBUTING RISK FACTORS",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            Text(
                text = "${factors.size} TOTAL",
                color = SecondaryText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal
            )
        }

        // Severity filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                label = "ALL (${factors.size})",
                selected = selectedFilter == null,
                onClick = { selectedFilter = null }
            )

            val critCount = factors.count { it.severity == RiskLevel.CRITICAL }
            if (critCount > 0) {
                FilterChip(
                    label = "CRIT ($critCount)",
                    selected = selectedFilter == RiskLevel.CRITICAL,
                    accentColor = RiskLevel.CRITICAL.color(),
                    onClick = { selectedFilter = RiskLevel.CRITICAL }
                )
            }

            val highCount = factors.count { it.severity == RiskLevel.HIGH }
            if (highCount > 0) {
                FilterChip(
                    label = "HIGH ($highCount)",
                    selected = selectedFilter == RiskLevel.HIGH,
                    accentColor = RiskLevel.HIGH.color(),
                    onClick = { selectedFilter = RiskLevel.HIGH }
                )
            }

            val medCount = factors.count { it.severity == RiskLevel.MEDIUM }
            if (medCount > 0) {
                FilterChip(
                    label = "MED ($medCount)",
                    selected = selectedFilter == RiskLevel.MEDIUM,
                    accentColor = RiskLevel.MEDIUM.color(),
                    onClick = { selectedFilter = RiskLevel.MEDIUM }
                )
            }
        }

        if (filteredFactors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NO CONTRIBUTING FACTORS DETECTED",
                    color = MutedText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.sp
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                filteredFactors.take(12).forEach { factor ->
                    FactorItemRow(factor = factor)
                }

                if (filteredFactors.size > 12) {
                    Text(
                        text = "+ ${filteredFactors.size - 12} more findings logged",
                        color = SecondaryText,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    accentColor: androidx.compose.ui.graphics.Color = PrimaryText,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .border(
                1.dp,
                if (selected) accentColor else BorderSubtle,
                RoundedCornerShape(6.dp)
            )
            .background(
                if (selected) accentColor.copy(alpha = 0.15f) else CardBackground,
                RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = if (selected) accentColor else SecondaryText,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun FactorItemRow(
    factor: RiskFactor,
    modifier: Modifier = Modifier
) {
    val levelColor = factor.severity.color()

    Column(
        modifier = modifier
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(factor.severity.containerColor(), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = factor.severity.name,
                        color = levelColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .background(Graphite.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = factor.category.displayName.uppercase(),
                        color = SecondaryText,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = "+${factor.scorePenalty} pts",
                color = levelColor,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = factor.title,
            color = PrimaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            text = factor.description,
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        if (!factor.evidence.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = factor.evidence,
                color = Graphite,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                maxLines = 2
            )
        }
    }
}
