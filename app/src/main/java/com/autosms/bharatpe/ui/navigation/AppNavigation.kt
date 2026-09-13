package com.autosms.bharatpe.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.autosms.bharatpe.ui.screens.HistoryScreen
import com.autosms.bharatpe.ui.screens.MainScreen
import com.autosms.bharatpe.ui.screens.SettingsScreen

/**
 * Navigation routes for the app.
 */
object Routes {
    const val MAIN = "main"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

/**
 * App navigation graph with three screens:
 * - Main: Dashboard with status, controls, and last payment
 * - History: Transaction history list
 * - Settings: Configuration, permissions, and setup guide
 */
@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN
    ) {
        composable(Routes.MAIN) {
            MainScreen(
                onNavigateToHistory = { navController.navigate(Routes.HISTORY) },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.HISTORY) {
            HistoryScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
