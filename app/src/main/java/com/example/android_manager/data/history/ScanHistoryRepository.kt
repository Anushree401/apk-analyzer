package com.example.android_manager.data.history

import android.content.Context
import com.example.android_manager.data.scanner.ApkAnalysisResult
import com.example.android_manager.model.RiskAssessment
import com.example.android_manager.model.RiskLevel
import com.example.android_manager.model.ScanHistoryEntry
import com.example.android_manager.model.ScanStatus
import com.example.android_manager.model.ScanType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository providing access to scan history records, handling mappings
 * between database entities and domain models.
 */
class ScanHistoryRepository(
    context: Context
) {
    private val dao = AppDatabase.getInstance(context).scanHistoryDao()

    /**
     * Flow emitting all scan history entries ordered by newest first.
     */
    fun getAllHistory(): Flow<List<ScanHistoryEntry>> {
        return dao.getAll().map { list -> list.map { it.toDomainModel() } }
    }

    /**
     * Retrieve all scan history entries snapshot.
     */
    suspend fun getAllHistorySnapshot(): List<ScanHistoryEntry> {
        return dao.getAllSnapshot().map { it.toDomainModel() }
    }

    /**
     * Retrieve a specific scan history entry by ID.
     */
    suspend fun getHistoryById(id: Long): ScanHistoryEntry? {
        return dao.getById(id)?.toDomainModel()
    }

    /**
     * Filter history by risk severity level.
     */
    fun filterByRiskLevel(riskLevel: RiskLevel): Flow<List<ScanHistoryEntry>> {
        return dao.getByRiskLevel(riskLevel.name).map { list -> list.map { it.toDomainModel() } }
    }

    /**
     * Search history records matching target or package name.
     */
    fun searchHistory(query: String): Flow<List<ScanHistoryEntry>> {
        return dao.search(query).map { list -> list.map { it.toDomainModel() } }
    }

    /**
     * Save a scan history entry to the database.
     */
    suspend fun saveScan(entry: ScanHistoryEntry): Long {
        return dao.insert(entry.toEntity())
    }

    /**
     * Convenience method to persist an APK scan result and its risk assessment.
     */
    suspend fun recordApkScan(
        result: ApkAnalysisResult,
        risk: RiskAssessment,
        durationMs: Long = 0L
    ): Long {
        val dexFindings = result.findings.filter { finding ->
            finding.name.contains("WebView", ignoreCase = true) ||
            finding.name.contains("JavaScript", ignoreCase = true) ||
            finding.name.contains("DEX", ignoreCase = true) ||
            finding.name.contains("Dependency", ignoreCase = true) ||
            finding.name.contains("Vulnerability", ignoreCase = true)
        }
        val unifiedFindings = com.example.android_manager.data.report.SecurityReportRepository.buildUnifiedFindings(
            assessment = risk,
            extraFindings = dexFindings
        )

        val criticalCount = unifiedFindings.count { it.severity == RiskLevel.CRITICAL }
        val highCount = unifiedFindings.count { it.severity == RiskLevel.HIGH }
        val mediumCount = unifiedFindings.count { it.severity == RiskLevel.MEDIUM }
        val lowCount = unifiedFindings.count { it.severity == RiskLevel.LOW }

        val status = when {
            criticalCount > 0 || highCount > 0 -> ScanStatus.THREAT_DETECTED
            mediumCount > 0 -> ScanStatus.SUSPICIOUS
            else -> ScanStatus.CLEAN
        }

        val appName = result.apkInfo.appName?.takeIf { it.isNotBlank() } ?: result.apkInfo.packageName

        val entry = ScanHistoryEntry(
            scanType = ScanType.APK_FILE,
            targetName = appName,
            packageName = result.apkInfo.packageName,
            versionName = result.apkInfo.versionName ?: "",
            versionCode = result.apkInfo.versionCode ?: 0L,
            minSdk = result.apkInfo.minSdk ?: 0,
            targetSdk = result.apkInfo.targetSdk ?: 0,
            riskScore = risk.overallScore,
            riskLevel = risk.overallLevel,
            status = status,
            findingsCount = unifiedFindings.size,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            findings = unifiedFindings,
            timestamp = System.currentTimeMillis(),
            scanDurationMs = durationMs
        )
        return saveScan(entry)
    }

    /**
     * Delete a single entry by its ID.
     */
    suspend fun deleteScan(id: Long): Boolean {
        return dao.deleteById(id) > 0
    }

    /**
     * Clear all recorded scan history.
     */
    suspend fun clearHistory(): Boolean {
        return dao.clearAll() >= 0
    }

    private fun ScanHistoryEntity.toDomainModel(): ScanHistoryEntry {
        return ScanHistoryEntry(
            id = id,
            scanType = scanType,
            targetName = targetName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            riskScore = riskScore,
            riskLevel = riskLevel,
            status = status,
            findingsCount = findingsCount,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            findings = findings,
            timestamp = timestamp,
            scanDurationMs = scanDurationMs
        )
    }

    private fun ScanHistoryEntry.toEntity(): ScanHistoryEntity {
        return ScanHistoryEntity(
            id = id,
            scanType = scanType,
            targetName = targetName,
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = minSdk,
            targetSdk = targetSdk,
            riskScore = riskScore,
            riskLevel = riskLevel,
            status = status,
            findingsCount = findingsCount,
            criticalCount = criticalCount,
            highCount = highCount,
            mediumCount = mediumCount,
            lowCount = lowCount,
            findings = findings,
            timestamp = timestamp,
            scanDurationMs = scanDurationMs
        )
    }
}