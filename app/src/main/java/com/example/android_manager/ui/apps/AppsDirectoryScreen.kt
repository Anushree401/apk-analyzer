package com.example.android_manager.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.ui.components.APVMTopBar
import com.example.android_manager.ui.components.ModuleCard
import com.example.android_manager.ui.theme.Background
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText

private data class AppModule(
    val title: String,
    val description: String
)

@Composable
fun AppsDirectoryScreen(
    onPermissionAuditor: () -> Unit,
    onProcessManager: () -> Unit,
    onRiskAssessment: () -> Unit,
    onSecurityReport: () -> Unit
) {
    val modules = listOf(
        AppModule(
            "Permission Auditor",
            "Audit installed application permissions"
        ),
        AppModule(
            "Process Manager",
            "Monitor foreground, background and recent activity"
        ),
        AppModule(
            "Security Risk Assessment",
            "Review risk scores, permissions and component exposure"
        ),
        AppModule(
            "Security Report & Explanation",
            "Review findings and mitigation recommendations"
        )
    )

    val actions = listOf(
        onPermissionAuditor,
        onProcessManager,
        onRiskAssessment,
        onSecurityReport
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        APVMTopBar()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Apps & Security Modules",
                    color = PrimaryText,
                    fontSize = 22.sp
                )

                Text(
                    text = "Select a module to explore its details.",
                    color = SecondaryText,
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(8.dp))
            }

            items(modules.indices.toList()) { index ->
                ModuleCard(
                    title = modules[index].title,
                    description = modules[index].description,
                    onClick = actions[index],
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}