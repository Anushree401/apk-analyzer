package com.example.android_manager.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.android_manager.ui.dashboard.DashboardScreen
import com.example.android_manager.ui.permissions.PermissionAuditorScreen
import com.example.android_manager.ui.process.ProcessManagerScreen

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
                onNavigateToPermissions = {
                    navController.navigate(Screen.PermissionAuditor.route)
                },
                onNavigateToProcessManager = {
                    navController.navigate(Screen.ProcessManager.route)
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
    }
}