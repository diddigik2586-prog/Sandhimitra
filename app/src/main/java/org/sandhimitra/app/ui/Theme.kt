package org.sandhimitra.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/** Indigo & Copper (approved palette). Derived tokens (muted text, outline, onPrimary) are computed from the approved values. */
object Palette {
    val LBg = Color(0xFFF0F2F8); val LCard = Color(0xFFFFFFFF); val LPrimary = Color(0xFF394B78); val LAccent = Color(0xFFC17B55); val LText = Color(0xFF28334D)
    val DBg = Color(0xFF171E30); val DCard = Color(0xFF262F49); val DPrimary = Color(0xFFD18B63); val DAccent = Color(0xFFE6AE88); val DText = Color(0xFFF0F2F8)
}

class Tokens(val dark: Boolean, val bg: Color, val card: Color, val primary: Color, val accent: Color, val text: Color, val muted: Color, val outline: Color, val onPrimary: Color, val ok: Color, val bad: Color)

val LocalTokens = staticCompositionLocalOf { lightTokens }
val LocalLang = compositionLocalOf { "sa" }

val lightTokens = Tokens(false, Palette.LBg, Palette.LCard, Palette.LPrimary, Palette.LAccent, Palette.LText,
    Palette.LText.copy(alpha = 0.68f), Palette.LPrimary.copy(alpha = 0.22f), Color.White, Color(0xFF2E7D4F), Color(0xFFB3382C))
val darkTokens = Tokens(true, Palette.DBg, Palette.DCard, Palette.DPrimary, Palette.DAccent, Palette.DText,
    Palette.DText.copy(alpha = 0.72f), Palette.DText.copy(alpha = 0.22f), Color(0xFF171E30), Color(0xFF7FD3A0), Color(0xFFF2998F))

@Composable
fun SandhimitraTheme(dark: Boolean, lang: String, fontScale: Float, content: @Composable () -> Unit) {
    val t = if (dark) darkTokens else lightTokens
    val scheme = if (dark) darkColorScheme(primary = t.primary, onPrimary = t.onPrimary, secondary = t.accent, background = t.bg, onBackground = t.text,
        surface = t.card, onSurface = t.text, surfaceVariant = t.card, onSurfaceVariant = t.muted, outline = t.outline, tertiary = t.accent)
    else lightColorScheme(primary = t.primary, onPrimary = t.onPrimary, secondary = t.accent, background = t.bg, onBackground = t.text,
        surface = t.card, onSurface = t.text, surfaceVariant = t.card, onSurfaceVariant = t.muted, outline = t.outline, tertiary = t.accent)
    val d = LocalDensity.current
    CompositionLocalProvider(LocalTokens provides t, LocalLang provides lang, LocalDensity provides Density(d.density, d.fontScale * fontScale)) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
