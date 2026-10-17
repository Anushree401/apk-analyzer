
package com.example.android_manager.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText

private val AppsCyan = Color(0xFF00DCC0)
private val AppsCard = Color(0xFF101010)
private val AppsIconBg = Color(0xFF192522)
private val AppsBorder = Color(0xFF292929)
private val AppsMuted = Color(0xFF777777)

private data class SecurityModule(
    val number: String,
    val title: String,
    val description: String,
    val category: String,
    val symbol: String,
    val onClick: () -> Unit
)

@Composable
fun AppsDirectoryScreen(
    onPermissionAuditor: () -> Unit,
    onProcessManager: () -> Unit,
    onRiskAssessment: () -> Unit,
    onSecurityReport: () -> Unit
) {
    val modules = listOf(
        SecurityModule(
            number = "01",
            title = "Permission Auditor",
            description = "Inspect app permissions and access levels.",
            category = "PERMISSIONS",
            symbol = "PA",
            onClick = onPermissionAuditor
        ),
        SecurityModule(
            number = "02",
            title = "Process Manager",
            description = "Review foreground and recent app activity.",
            category = "PROCESSES",
            symbol = "PM",
            onClick = onProcessManager
        ),
        SecurityModule(
            number = "03",
            title = "Security Risk Assessment",
            description = "Explore risk scores and exposed components.",
            category = "RISK ANALYSIS",
            symbol = "RA",
            onClick = onRiskAssessment
        ),
        SecurityModule(
            number = "04",
            title = "Security Report & Explanation",
            description = "Review findings and mitigation guidance.",
            category = "REPORTING",
            symbol = "SR",
            onClick = onSecurityReport
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = "APVM / MODULES",
                color = AppsCyan,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.7.sp
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Security Centre",
                color = PrimaryText,
                fontSize = 27.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = "Device analysis and security tools.",
                color = SecondaryText,
                fontSize = 13.sp
            )

            Spacer(Modifier.height(22.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = modules,
                    key = { it.number }
                ) { module ->
                    ModuleDirectoryCard(module)
                }
            }
        }
    }
}

@Composable
private fun ModuleDirectoryCard(
    module: SecurityModule
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.88f)
            .clip(RoundedCornerShape(15.dp))
            .background(AppsCard)
            .border(
                width = 1.dp,
                color = AppsBorder,
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(onClick = module.onClick)
            .padding(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(AppsIconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = module.symbol,
                    color = AppsCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = module.number,
                color = AppsMuted,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = module.title,
            color = PrimaryText,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(7.dp))

        Text(
            text = module.description,
            color = SecondaryText,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AppsBorder)
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = module.category,
            color = AppsMuted,
            fontSize = 8.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.4.sp,
            maxLines = 1
        )

        Spacer(Modifier.height(7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "OPEN",
                color = AppsCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )

            Text(
                text = "↗",
                color = AppsCyan,
                fontSize = 17.sp
            )
        }
    }
}
