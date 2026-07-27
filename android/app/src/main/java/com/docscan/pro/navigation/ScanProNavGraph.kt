package com.docscan.pro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.docscan.pro.feature.account.AccountScreen
import com.docscan.pro.feature.account.AuthScreen
import com.docscan.pro.feature.editor.EditorScreen
import com.docscan.pro.feature.home.HomeScreen
import com.docscan.pro.feature.splash.SplashScreen
import com.docscan.pro.feature.tools.EditPickerScreen
import com.docscan.pro.feature.tools.ToolsScreen
import com.docscan.pro.feature.viewer.PdfViewerScreen

object Routes {
    const val SPLASH = "splash"
    const val HOME = "home"
    const val ACCOUNT = "account"
    const val AUTH = "auth"
    const val TOOLS = "tools"
    const val EDIT_PICK = "editpick"
    const val VIEWER = "viewer/{documentId}"
    const val EDITOR = "editor/{documentId}"
    fun viewer(documentId: String) = "viewer/$documentId"
    fun editor(documentId: String) = "editor/$documentId"
}

@Composable
fun ScanProNavGraph() {
    val nav = rememberNavController()
    val docArgs = listOf(navArgument("documentId") { type = NavType.StringType })

    NavHost(navController = nav, startDestination = Routes.SPLASH) {
        composable(Routes.SPLASH) {
            SplashScreen(onFinished = {
                nav.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenDocument = { id -> nav.navigate(Routes.viewer(id)) },
                onAccount = { nav.navigate(Routes.ACCOUNT) },
                onTools = { nav.navigate(Routes.TOOLS) },
            )
        }
        composable(Routes.VIEWER, arguments = docArgs) {
            PdfViewerScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.EDITOR, arguments = docArgs) {
            EditorScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.ACCOUNT) {
            AccountScreen(
                onBack = { nav.popBackStack() },
                onSignIn = { nav.navigate(Routes.AUTH) },
            )
        }
        composable(Routes.AUTH) {
            AuthScreen(onDone = { nav.popBackStack() })
        }
        composable(Routes.TOOLS) {
            ToolsScreen(
                onBack = { nav.popBackStack() },
                onEditPdf = { nav.navigate(Routes.EDIT_PICK) },
            )
        }
        composable(Routes.EDIT_PICK) {
            EditPickerScreen(
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate(Routes.editor(id)) },
            )
        }
    }
}
