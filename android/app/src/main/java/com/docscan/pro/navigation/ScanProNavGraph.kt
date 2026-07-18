package com.docscan.pro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docscan.pro.feature.editor.EditorScreen
import com.docscan.pro.feature.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val EDITOR = "editor/{documentId}"
    fun editor(documentId: String) = "editor/$documentId"
}

@Composable
fun ScanProNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
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
