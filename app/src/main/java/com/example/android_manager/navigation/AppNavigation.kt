package com.example.android_manager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.android_manager.ui.dashboard.DashboardScreen
import com.example.android_manager.ui.history.ScanHistoryScreen
import com.example.android_manager.ui.permissions.PermissionAuditorScreen
import com.example.android_manager.ui.process.ProcessManagerScreen
import com.example.android_manager.ui.report.SecurityReportScreen
import com.example.android_manager.ui.risk.RiskAssessmentScreen
import com.example.android_manager.ui.scanner.ApkScannerScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Dashboard.route
    ) {

        composable(
            route = Screen.Dashboard.route
        ) {

            DashboardScreen(
                onPermissionAuditor = {
                    navController.navigate(Screen.PermissionAuditor.route)
                },
                onProcessManager = {
                    navController.navigate(Screen.ProcessManager.route)
                },
                onApkScanner = {
                    navController.navigate(Screen.ApkScanner.route)
                },
                onRiskAssessment = {
                    navController.navigate(Screen.RiskAssessment.route)
                },
                onScanHistory = {
                    navController.navigate(Screen.ScanHistory.route)
                }
            )
        }

        composable(
            route = Screen.PermissionAuditor.route
        ) {

            PermissionAuditorScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ProcessManager.route) {
            ProcessManagerScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ApkScanner.route) {

            ApkScannerScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToRiskAssessment = {
                    navController.navigate(Screen.RiskAssessment.route)
                },
                onNavigateToReport = {
                    navController.navigate(Screen.SecurityReport.route)
                }
            )
        }

        composable(Screen.RiskAssessment.route) {

            RiskAssessmentScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToReport = { pkg ->
                    navController.navigate(Screen.SecurityReport.createRoute(targetPkg = pkg))
                }
            )
        }

        composable(
            route = Screen.SecurityReport.route,
            arguments = listOf(
                navArgument("targetPkg") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("historyId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val targetPkg = backStackEntry.arguments?.getString("targetPkg")?.takeIf { it.isNotBlank() }
            val historyId = backStackEntry.arguments?.getLong("historyId")?.takeIf { it > 0 }

            SecurityReportScreen(
                targetPackageName = targetPkg,
                historyId = historyId,
                onBack = {
                    navController.popBackStack()
                },
                onNavigate = { screen ->
                    when (screen) {
                        Screen.Dashboard -> navController.navigate(Screen.Dashboard.route)
                        Screen.PermissionAuditor -> navController.navigate(Screen.PermissionAuditor.route)
                        Screen.ProcessManager -> navController.navigate(Screen.ProcessManager.route)
                        Screen.ApkScanner -> navController.navigate(Screen.ApkScanner.route)
                        Screen.RiskAssessment -> navController.navigate(Screen.RiskAssessment.route)
                        Screen.ScanHistory -> navController.navigate(Screen.ScanHistory.route)
                        else -> Unit
                    }
                }
            )
        }

        composable(Screen.ScanHistory.route) {
            ScanHistoryScreen(
                onBack = {
                    navController.popBackStack()
                },
                onNavigateToScan = {
                    navController.navigate(Screen.ApkScanner.route)
                }
            )
        }
    }
}