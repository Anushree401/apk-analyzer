package com.example.android_manager.data.report

import android.content.Context
import com.example.android_manager.data.apps.AppRepository
import com.example.android_manager.data.history.ScanHistoryRepository
import com.example.android_manager.data.permissions.PermissionRepository
import com.example.android_manager.data.risk.RiskAssessmentRepository
import com.example.android_manager.data.scanner.ApkScannerRepository
import com.example.android_manager.model.SecurityReport

/**
 * Repository orchestrating the generation, caching, and retrieval of comprehensive
 * security reports across APK scans, installed applications, and historical audit logs.
 */
class SecurityReportRepository(
    private val context: Context
) {
    private val appRepo = AppRepository(context)
    private val permRepo = PermissionRepository(context)
    private val riskRepo = RiskAssessmentRepository(context)
    private val scannerRepo = ApkScannerRepository(context)
    private val historyRepo = ScanHistoryRepository(context)

    companion object {
        private var activeReport: SecurityReport? = null

        /**
         * Sets the active report in memory for immediate access by the report screen.
         */
        fun setActiveReport(report: SecurityReport) {
            activeReport = report
        }

        /**
         * Gets the active report currently loaded in memory.
         */
        fun getActiveReport(): SecurityReport? = activeReport

        /**
         * Clears the active report from memory.
         */
        fun clearActiveReport() {
            activeReport = null
        }
    }
}