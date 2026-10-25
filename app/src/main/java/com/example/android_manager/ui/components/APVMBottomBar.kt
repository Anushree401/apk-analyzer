
package com.example.android_manager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
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
import com.example.android_manager.navigation.Screen

private val NavBackground = Color(0xFF080808)
private val NavSelected = Color(0xFF00DCC0)
private val NavUnselected = Color(0xFF888888)

@Composable
fun APVMBottomBar(
    modifier: Modifier = Modifier,
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(NavBackground)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 3.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomNavItem(
            label = "Home",
            selected = currentScreen == Screen.Dashboard,
            onClick = { onNavigate(Screen.Dashboard) }
        )

        BottomNavItem(
            label = "Apps",
            selected = currentScreen == Screen.Apps ||
                    currentScreen == Screen.PermissionAuditor ||
                    currentScreen == Screen.ProcessManager ||
                    currentScreen == Screen.RiskAssessment,
            onClick = { onNavigate(Screen.Apps) }
        )

        BottomNavItem(
            label = "Scan",
            selected = currentScreen == Screen.ApkScanner,
            onClick = { onNavigate(Screen.ApkScanner) }
        )

        BottomNavItem(
            label = "Reports",
            selected = currentScreen == Screen.SecurityReport,
            onClick = { onNavigate(Screen.SecurityReport) }
        )

        BottomNavItem(
            label = "History",
            selected = currentScreen == Screen.ScanHistory,
            onClick = { onNavigate(Screen.ScanHistory) }
        )
}
}

@Composable
private fun BottomNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 52.dp, minHeight = 48.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) NavSelected else NavUnselected,
            fontSize = 11.sp,
            fontWeight = if (selected) {
                FontWeight.SemiBold
            } else {
                FontWeight.Normal
            }
        )
    }
}
