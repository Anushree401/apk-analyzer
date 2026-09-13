package com.example.android_manager.ui.scanner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.android_manager.model.VulnerabilityFinding
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText
import com.example.android_manager.ui.theme.Surface

@Composable
fun ApkFindingCard(
    finding: VulnerabilityFinding,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Surface,
                RoundedCornerShape(12.dp)
            )
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {

        Text(
            text = finding.severity.name,
            color = PrimaryText
        )

        Spacer(
            modifier = Modifier.height(6.dp)
        )

        Text(
            text = finding.name,
            color = PrimaryText
        )

        finding.affectedComponent?.let {

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            Text(
                text = "component: $it",
                color = SecondaryText
            )
        }

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "evidence",
            color = SecondaryText
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = finding.evidence,
            color = PrimaryText
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "impact",
            color = SecondaryText
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = finding.impact,
            color = PrimaryText
        )

        Spacer(
            modifier = Modifier.height(12.dp)
        )

        Text(
            text = "recommendation",
            color = SecondaryText
        )

        Spacer(
            modifier = Modifier.height(4.dp)
        )

        Text(
            text = finding.recommendation,
            color = PrimaryText
        )
    }
}