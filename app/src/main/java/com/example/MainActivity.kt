package com.example

import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.RadioDatabase
import com.example.data.RadioRepository
import com.example.ui.RadioApp
import com.example.ui.RadioViewModel
import com.example.ui.RadioViewModelFactory
import com.example.ui.SettingsScreen
import com.example.ui.SettingsViewModel
import com.example.ui.SettingsViewModelFactory
import com.example.data.AppTheme
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support edge-to-edge status & nav bar coloring natively
        enableEdgeToEdge()
        
        // Request Notification permission on Android 13+ (API 33) to prevent Foreground Service crashes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        
        // Hide system bars for immersive mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Instantiate SQLite persistence using singletons or standard builder patterns
        val database = RadioDatabase.getDatabase(applicationContext)
        val repository = RadioRepository(database.radioDao(), database.playbackHistoryDao())
        val radioFactory = RadioViewModelFactory(application, repository)
        val settingsFactory = SettingsViewModelFactory(application, repository)
        
        setContent {
            val settingsViewModel: SettingsViewModel = viewModel(factory = settingsFactory)
            val appTheme by settingsViewModel.appTheme.collectAsState()
            val appFontSize by settingsViewModel.appFontSize.collectAsState()
            val appIconSize by settingsViewModel.appIconSize.collectAsState()
            
            MyApplicationTheme(
                appTheme = appTheme,
                appFontSize = appFontSize,
                appIconSize = appIconSize
            ) {
                val navController = rememberNavController()
                
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        val radioViewModel: RadioViewModel = viewModel(factory = radioFactory)
                        RadioApp(
                            viewModel = radioViewModel,
                            onNavigateToSettings = { navController.navigate("settings") }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            viewModel = settingsViewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

