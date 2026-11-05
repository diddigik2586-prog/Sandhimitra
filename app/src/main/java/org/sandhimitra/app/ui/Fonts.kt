package org.sandhimitra.app.ui

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sandhimitra.app.R

/** Fonts: Kohinoor Devanagari (Sanskrit), Kannada MN (Kannada), Andika (English/IAST). See FONT_INTEGRATION_REPORT.md. */
object Fonts {
    /** Latin text and IAST (English): Andika. */
    val latin = FontFamily(Font(R.font.andika_regular, FontWeight.Normal), Font(R.font.andika_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.andika_medium, FontWeight.Medium), Font(R.font.andika_semibold, FontWeight.SemiBold), Font(R.font.andika_bold, FontWeight.Bold))
    /** Sanskrit (Devanagari): Kohinoor Devanagari. */
    val devanagari = FontFamily(Font(R.font.kohinoor_deva_regular, FontWeight.Normal), Font(R.font.kohinoor_deva_medium, FontWeight.Medium),
        Font(R.font.kohinoor_deva_semibold, FontWeight.SemiBold), Font(R.font.kohinoor_deva_bold, FontWeight.Bold))
    /** Kannada: Kannada MN (single weight; bold is synthesised). */
    val kannada = FontFamily(Font(R.font.kannada_mn, FontWeight.Normal))

    fun kindOf(c: Char): Int = when {
        c in '\u0900'..'\u097F' || c == '\u200D' || c == '\u200C' -> 1
        c in '\u0C80'..'\u0CFF' -> 2
        else -> 0
    }

    /** Splits text into script runs and applies the matching FontFamily to each run. Neutral characters join the preceding run. */
    fun annotate(text: String, color: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
        append(text)
        var start = 0; var cur = -1
        fun close(end: Int) {
            if (cur >= 0 && end > start) addStyle(SpanStyle(fontFamily = when (cur) { 1 -> devanagari; 2 -> kannada; else -> latin }), start, end)
        }
        for (i in text.indices) {
            val c = text[i]
            val k = kindOf(c)
            val neutral = k == 0 && (c == ' ' || c.isDigit() || c in ",.;:!?()[]{}'\"‘’“”–—-+=→/|•·…*%&") && cur >= 1
            val effective = if (neutral) cur else k
            if (effective != cur) { close(i); start = i; cur = effective }
        }
        close(text.length)
    }
}

val LocalAlign = androidx.compose.runtime.compositionLocalOf<TextAlign?> { null }

/** The single text primitive of the app: every visible string goes through script-aware font mapping. */
@Composable
fun Sx(text: String, modifier: Modifier = Modifier, size: TextUnit = 16.sp, weight: FontWeight = FontWeight.Normal, color: Color = LocalTokens.current.text,
       align: TextAlign? = null, italic: Boolean = false, maxLines: Int = Int.MAX_VALUE, lineHeight: TextUnit = TextUnit.Unspecified,
       justify: Boolean = false, indent: Boolean = false) {
    val a = remember(text) { Fonts.annotate(text) }
    // paragraph style: justified, first line indented by 3 mm (3 / 25.4 in × 160 dp/in ≈ 18.9 dp, kept physical under font scaling)
    val ind = if (indent) with(androidx.compose.ui.platform.LocalDensity.current) { 18.9f.dp.toSp() } else 0.sp
    BasicText(a, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = size, fontWeight = weight, color = color, textAlign = if (justify) TextAlign.Justify else align ?: LocalAlign.current ?: TextAlign.Unspecified,
            textIndent = if (indent) androidx.compose.ui.text.style.TextIndent(firstLine = ind) else null,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            lineHeight = if (lineHeight == TextUnit.Unspecified) (size.value * 1.55f).sp else lineHeight, fontFamily = Fonts.latin))
}

/** Applies script-aware fonts inside text fields (identity offset mapping). */
object ScriptTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(Fonts.annotate(text.text), OffsetMapping.Identity)
}
