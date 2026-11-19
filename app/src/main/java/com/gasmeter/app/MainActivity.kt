package com.gasmeter.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gasmeter.app.ui.GasMeterNavHost
import com.gasmeter.app.ui.GasMeterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as GasMeterApp).repository
        setContent {
            GasMeterTheme {
                GasMeterNavHost(repository)
            }
        }
    }
}
