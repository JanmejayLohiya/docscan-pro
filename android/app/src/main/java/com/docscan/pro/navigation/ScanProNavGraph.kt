package com.docscan.pro.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.docscan.pro.feature.home.HomeScreen

object Routes {
    const val HOME = "home"
}

@Composable
fun ScanProNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen() }
    }
}
