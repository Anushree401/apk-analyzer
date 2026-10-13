package com.example.android_manager.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.android_manager.navigation.Screen
import com.example.android_manager.ui.theme.Border
import com.example.android_manager.ui.theme.CyberCyan
import com.example.android_manager.ui.theme.PrimaryText
import com.example.android_manager.ui.theme.SecondaryText

@Composable
fun APVMBottomBar(
    modifier: Modifier = Modifier,
    currentScreen: Screen? = null,
    onNavigate: ((Screen) -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Border,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        BottomNavItem(
            label = "Home",
            isSelected = currentScreen == Screen.Dashboard,
            onClick = { onNavigate?.invoke(Screen.Dashboard) }
        )
        BottomNavItem(
            label = "Apps",
            isSelected = currentScreen == Screen.PermissionAuditor,
            onClick = { onNavigate?.invoke(Screen.PermissionAuditor) }
        )
        BottomNavItem(
            label = "Scan",
            isSelected = currentScreen == Screen.ApkScanner,
            onClick = { onNavigate?.invoke(Screen.ApkScanner) }
        )
        BottomNavItem(
            label = "Reports",
            isSelected = currentScreen == Screen.SecurityReport || currentScreen == Screen.RiskAssessment,
            onClick = { onNavigate?.invoke(Screen.SecurityReport) }
        )
        BottomNavItem(
            label = "History",
            isSelected = currentScreen == Screen.ScanHistory,
            onClick = { onNavigate?.invoke(Screen.ScanHistory) }
        )
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Text(
        text = label,
        color = if (isSelected) CyberCyan else SecondaryText,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        fontSize = 12.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}