package com.example.android_manager.data.history

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanStatus
import com.example.android_manager.model.ScanType
import com.example.android_manager.model.VulnerabilityFinding

/**
 * Room database entity representing a persisted scan record.
 */
@Entity(
    tableName = "scan_history",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["riskLevel"]),
        Index(value = ["packageName"])
    ]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanType: ScanType = ScanType.APK_FILE,
    val targetName: String,
    val packageName: String,
    val versionName: String = "",
    val versionCode: Long = 0L,
    val minSdk: Int = 0,
    val targetSdk: Int = 0,
    val riskScore: Int,
    val riskLevel: RiskLevel,
    val status: ScanStatus = ScanStatus.CLEAN,
    val findingsCount: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val findings: List<VulnerabilityFinding> = emptyList(),
    val timestamp: Long = System.currentTimeMillis(),
    val scanDurationMs: Long = 0L
)
