package com.example.android_manager.navigation

sealed class Screen(val route: String) {

    data object Dashboard : Screen("dashboard")
    data object Apps : Screen("apps")

    data object PermissionAuditor : Screen("permission_auditor")
    data object ProcessManager : Screen("process_manager")
    data object ApkScanner : Screen("apk_scanner")
    data object RiskAssessment : Screen("risk_assessment")

    data object SecurityReport :
        Screen("security_report?targetPkg={targetPkg}&historyId={historyId}") {

        fun createRoute(
            targetPkg: String? = null,
            historyId: Long? = null
        ): String {
            val pkg = targetPkg ?: ""
            val id = historyId ?: -1L
            return "security_report?targetPkg=$pkg&historyId=$id"
        }
    }

    data object ScanHistory : Screen("scan_history")
}