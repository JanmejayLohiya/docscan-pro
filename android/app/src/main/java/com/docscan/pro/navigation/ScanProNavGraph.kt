package com.docscan.pro.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.docscan.pro.feature.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val SCAN = "scan"
}

@Composable
fun ScanProNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onScan = { nav.navigate(Routes.SCAN) })
        }
        composable(Routes.SCAN) {
            // Placeholder — the camera/scan feature module lands here (FR-3.*).
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text("Scan screen — to be implemented (CameraX + ML Kit).")
            }
        }
    }
}
