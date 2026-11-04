package org.sandhimitra.app.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Persistent user preferences (SharedPreferences, local only). Language defaults to Sanskrit on first install. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("sandhimitra_prefs", Context.MODE_PRIVATE)
    var lang by mutableStateOf(sp.getString("lang", "sa") ?: "sa")
        private set
    var dark by mutableStateOf(sp.getBoolean("dark", false))
        private set
    var fontStep by mutableIntStateOf(sp.getInt("fontStep", 1))
        private set

    fun chooseLang(v: String) { lang = v; sp.edit().putString("lang", v).apply() }
    fun chooseDark(v: Boolean) { dark = v; sp.edit().putBoolean("dark", v).apply() }
    fun chooseFontStep(v: Int) { fontStep = v; sp.edit().putInt("fontStep", v).apply() }
    val fontScale get() = floatArrayOf(0.9f, 1.0f, 1.2f, 1.4f)[fontStep.coerceIn(0, 3)]
}
