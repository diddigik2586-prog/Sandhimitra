package org.sandhimitra.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import org.sandhimitra.app.ui.MainApp
import org.sandhimitra.app.ui.SandhimitraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as SandhimitraApp
        setContent {
            val dark = app.prefs.dark
            WindowCompat.getInsetsController(window, window.decorView).apply { isAppearanceLightStatusBars = !dark; isAppearanceLightNavigationBars = !dark }
            SandhimitraTheme(dark = dark, lang = app.prefs.lang, fontScale = app.prefs.fontScale) { MainApp(app) }
        }
    }
}
