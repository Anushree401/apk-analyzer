package com.example.android_manager.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.android_manager.ui.apps.AppsDirectoryScreen
import com.example.android_manager.ui.components.APVMBottomBar
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
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()

    val currentScreen = when {
        route == Screen.Dashboard.route -> Screen.Dashboard
        route == Screen.Apps.route -> Screen.Apps
        route == Screen.PermissionAuditor.route -> Screen.PermissionAuditor
        route == Screen.ProcessManager.route -> Screen.ProcessManager
        route == Screen.ApkScanner.route -> Screen.ApkScanner
        route == Screen.RiskAssessment.route -> Screen.RiskAssessment
        route.startsWith("security_report") -> Screen.SecurityReport
        route == Screen.ScanHistory.route -> Screen.ScanHistory
        else -> Screen.Dashboard
    }

    fun navigateToTab(screen: Screen) {
        val destination = when (screen) {
            Screen.Dashboard -> Screen.Dashboard.route
            Screen.Apps -> Screen.Apps.route
            Screen.ApkScanner -> Screen.ApkScanner.route
            Screen.SecurityReport -> Screen.SecurityReport.createRoute()
            Screen.ScanHistory -> Screen.ScanHistory.route
            else -> screen.route
        }

        navController.navigate(destination) {
            popUpTo(Screen.Dashboard.route) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.weight(1f)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onApps = {
                        navigateToTab(Screen.Apps)
                    },
                    onApkScanner = {
                        navigateToTab(Screen.ApkScanner)
                    },
                    onNavigateToReport = { historyId ->
                        navController.navigate(
                            Screen.SecurityReport.createRoute(historyId = historyId)
                        )
                    }
                )
            }

            composable(Screen.Apps.route) {
                AppsDirectoryScreen(
                    onPermissionAuditor = {
                        navController.navigate(Screen.PermissionAuditor.route)
                    },
                    onProcessManager = {
                        navController.navigate(Screen.ProcessManager.route)
                    },
                    onRiskAssessment = {
                        navController.navigate(Screen.RiskAssessment.route)
                    },
                    onSecurityReport = {
                        navigateToTab(Screen.SecurityReport)
                    }
                )
            }

            composable(Screen.PermissionAuditor.route) {
                PermissionAuditorScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ProcessManager.route) {
                ProcessManagerScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ApkScanner.route) {
                ApkScannerScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToRiskAssessment = {
                        navController.navigate(Screen.RiskAssessment.route)
                    },
                    onNavigateToReport = {
                        navigateToTab(Screen.SecurityReport)
                    }
                )
            }

            composable(Screen.RiskAssessment.route) {
                RiskAssessmentScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToReport = { pkg ->
                        navController.navigate(
                            Screen.SecurityReport.createRoute(targetPkg = pkg)
                        )
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
            ) { entry ->
                val targetPkg = entry.arguments
                    ?.getString("targetPkg")
                    ?.takeIf { it.isNotBlank() }

                val historyId = entry.arguments
                    ?.getLong("historyId")
                    ?.takeIf { it > 0L }

                SecurityReportScreen(
                    targetPackageName = targetPkg,
                    historyId = historyId,
                    onBack = { navController.popBackStack() },
                    onNavigate = { screen ->
                        when (screen) {
                            Screen.Dashboard -> navigateToTab(Screen.Dashboard)
                            Screen.Apps -> navigateToTab(Screen.Apps)
                            Screen.PermissionAuditor ->
                                navController.navigate(Screen.PermissionAuditor.route)
                            Screen.ProcessManager ->
                                navController.navigate(Screen.ProcessManager.route)
                            Screen.ApkScanner -> navigateToTab(Screen.ApkScanner)
                            Screen.RiskAssessment ->
                                navController.navigate(Screen.RiskAssessment.route)
                            Screen.ScanHistory -> navigateToTab(Screen.ScanHistory)
                            else -> Unit
                        }
                    }
                )
            }

            composable(Screen.ScanHistory.route) {
                ScanHistoryScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToScan = {
                        navigateToTab(Screen.ApkScanner)
                    },
                    onNavigateToReport = { historyId ->
                        navController.navigate(
                            Screen.SecurityReport.createRoute(historyId = historyId)
                        )
                    }
                )
            }
        }
        APVMBottomBar(
            currentScreen = currentScreen,
            onNavigate = ::navigateToTab
        )
    }
}