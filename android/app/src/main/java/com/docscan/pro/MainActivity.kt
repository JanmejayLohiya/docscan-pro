package com.docscan.pro

import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.docscan.pro.navigation.ScanProNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Hold the splash briefly so the scan-line animation is visible.
        val start = SystemClock.uptimeMillis()
        splashScreen.setKeepOnScreenCondition { SystemClock.uptimeMillis() - start < 1200L }
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier) {
                    ScanProNavGraph()
                }
            }
        }
    }
}
