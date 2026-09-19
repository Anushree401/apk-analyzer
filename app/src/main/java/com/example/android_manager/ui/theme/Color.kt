package com.example.android_manager.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.android_manager.model.RiskLevel

val Background = Color(0xFF000000)
val Surface = Color(0xFF111111)
val SurfaceVariant = Color(0xFF151515)
val CardBackground = Color(0xFF141414)

val PrimaryText = Color(0xFFFFFFFF)
val SecondaryText = Color(0xFF888888)
val MutedText = Color(0xFF555555)

val Border = Color(0xFF252525)
val BorderSubtle = Color(0xFF1C1C1C)
val Graphite = Color(0xFF6B6B6B)

// Cybersecurity telemetry and risk status indicators
val RiskLow = Color(0xFF00E676)       // Vibrant Emerald
val RiskMedium = Color(0xFFFFD600)    // High-visibility Amber
val RiskHigh = Color(0xFFFF9100)      // Neon Orange
val RiskCritical = Color(0xFFFF1744)  // Vivid Crimson

val CyberCyan = Color(0xFF00E5FF)
val CyberPurple = Color(0xFFB388FF)

/**
 * Maps a RiskLevel directly to its associated accent status color.
 */
fun RiskLevel.color(): Color = when (this) {
    RiskLevel.LOW -> RiskLow
    RiskLevel.MEDIUM -> RiskMedium
    RiskLevel.HIGH -> RiskHigh
    RiskLevel.CRITICAL -> RiskCritical
}

/**
 * Maps a RiskLevel to a muted 15% opacity container background color.
 */
fun RiskLevel.containerColor(): Color = when (this) {
    RiskLevel.LOW -> Color(0x2200E676)
    RiskLevel.MEDIUM -> Color(0x22FFD600)
    RiskLevel.HIGH -> Color(0x22FF9100)
    RiskLevel.CRITICAL -> Color(0x22FF1744)
}