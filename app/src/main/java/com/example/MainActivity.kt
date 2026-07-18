package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.RadioDatabase
import com.example.data.RadioRepository
import com.example.ui.RadioApp
import com.example.ui.RadioViewModel
import com.example.ui.RadioViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Support edge-to-edge status & nav bar coloring natively
        enableEdgeToEdge()
        
        // Hide system bars for immersive mode
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Instantiate SQLite persistence using singletons or standard builder patterns
        val database = RadioDatabase.getDatabase(applicationContext)
        val repository = RadioRepository(database.radioDao())
        val factory = RadioViewModelFactory(application, repository)
        
        setContent {
            MyApplicationTheme {
                // Initialize the audio playback and state viewmodel
                val viewModel: RadioViewModel = viewModel(factory = factory)
                RadioApp(viewModel = viewModel)
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
    androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}

