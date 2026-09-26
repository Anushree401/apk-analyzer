package com.example.android_manager.data.history

import androidx.room.TypeConverter
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanStatus
import com.example.android_manager.model.ScanType
import com.example.android_manager.model.VulnerabilityFinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Room TypeConverters for serializing complex objects like vulnerability findings
 * and enums for persistence in SQLite.
 */
class ScanHistoryConverters {
    private val gson = Gson()

    @TypeConverter
    fun fromVulnerabilityFindingList(findings: List<VulnerabilityFinding>?): String {
        return if (findings.isNullOrEmpty()) "[]" else gson.toJson(findings)
    }

    @TypeConverter
    fun toVulnerabilityFindingList(data: String?): List<VulnerabilityFinding> {
        if (data.isNullOrBlank() || data == "[]") return emptyList()
        val listType = object : TypeToken<List<VulnerabilityFinding>>() {}.type
        return try {
            gson.fromJson(data, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromScanType(type: ScanType?): String = type?.name ?: ScanType.APK_FILE.name

    @TypeConverter
    fun toScanType(value: String?): ScanType {
        return try {
            if (value != null) ScanType.valueOf(value) else ScanType.APK_FILE
        } catch (e: Exception) {
            ScanType.APK_FILE
        }
    }

    @TypeConverter
    fun fromRiskLevel(level: RiskLevel?): String = level?.name ?: RiskLevel.LOW.name

    @TypeConverter
    fun toRiskLevel(value: String?): RiskLevel {
        return try {
            if (value != null) RiskLevel.valueOf(value) else RiskLevel.LOW
        } catch (e: Exception) {
            RiskLevel.LOW
        }
    }

    @TypeConverter
    fun fromScanStatus(status: ScanStatus?): String = status?.name ?: ScanStatus.CLEAN.name

    @TypeConverter
    fun toScanStatus(value: String?): ScanStatus {
        return try {
            if (value != null) ScanStatus.valueOf(value) else ScanStatus.CLEAN
        } catch (e: Exception) {
            ScanStatus.CLEAN
        }
    }
}
