package com.docscan.pro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docscan.pro.feature.editor.EditorScreen
import com.docscan.pro.feature.home.HomeScreen
import com.docscan.pro.feature.splash.SplashScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val EDITOR = "editor/{documentId}"
    fun editor(documentId: String) = "editor/$documentId"
}

@Composable
fun ScanProNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                nav.navigate(Routes.HOME) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(onOpenDocument = { id -> nav.navigate(Routes.editor(id)) })
        }
        composable(
            route = Routes.EDITOR,
            arguments = listOf(navArgument("documentId") { type = NavType.StringType }),
        ) {
            EditorScreen(onBack = { nav.popBackStack() })
        }
    }
}
