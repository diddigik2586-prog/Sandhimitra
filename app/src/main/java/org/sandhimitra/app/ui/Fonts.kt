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
import androidx.compose.ui.unit.sp
import org.sandhimitra.app.R

/**
 * Font mapping. The supplied Kohinoor family covers Latin only (no Devanagari, Kannada, nor the IAST dot-below letters),
 * so Kohinoor is applied to every Latin run and Noto Sans Devanagari / Kannada / Noto Sans (IAST letters) cover the rest.
 * See FONT_INTEGRATION_REPORT.md.
 */
object Fonts {
    val kohinoor = FontFamily(Font(R.font.kohinoor_book, FontWeight.Normal), Font(R.font.kohinoor_book_italic, FontWeight.Normal, FontStyle.Italic),
        Font(R.font.kohinoor_medium, FontWeight.Medium), Font(R.font.kohinoor_medium, FontWeight.SemiBold), Font(R.font.kohinoor_bold, FontWeight.Bold))
    val devanagari = FontFamily(Font(R.font.noto_devanagari_regular, FontWeight.Normal), Font(R.font.noto_devanagari_regular, FontWeight.Medium),
        Font(R.font.noto_devanagari_bold, FontWeight.SemiBold), Font(R.font.noto_devanagari_bold, FontWeight.Bold))
    val kannada = FontFamily(Font(R.font.noto_kannada_regular, FontWeight.Normal), Font(R.font.noto_kannada_regular, FontWeight.Medium),
        Font(R.font.noto_kannada_bold, FontWeight.SemiBold), Font(R.font.noto_kannada_bold, FontWeight.Bold))
    val latinExt = FontFamily(Font(R.font.noto_sans_regular, FontWeight.Normal), Font(R.font.noto_sans_regular, FontWeight.Medium),
        Font(R.font.noto_sans_bold, FontWeight.SemiBold), Font(R.font.noto_sans_bold, FontWeight.Bold))

    /** Latin letters absent from Kohinoor but present in Noto Sans (IAST dot-below, macron-less etc.). */
    private const val NOT_IN_KOHINOOR = "ṛṝḷḹṃṁḥṅṭḍṇṣṚṜḶḸṂḤṄṬḌṆṢ̣̃̄́"
    fun kindOf(c: Char): Int = when {
        c in 'ऀ'..'ॿ' || c == '‍' || c == '‌' -> 1
        c in 'ಀ'..'೿' -> 2
        NOT_IN_KOHINOOR.indexOf(c) >= 0 || c in '̀'..'ͯ' || c in 'Ḁ'..'ỿ' || c in 'Ā'..'ſ' && "āīūĀĪŪñśŚÑ".indexOf(c) < 0 -> 3
        else -> 0
    }

    /** Splits text into script runs and applies the matching FontFamily to each run. Neutral characters join the preceding run. */
    fun annotate(text: String, color: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
        append(text)
        var start = 0; var cur = -1
        fun close(end: Int) {
            if (cur >= 0 && end > start) addStyle(SpanStyle(fontFamily = when (cur) { 1 -> devanagari; 2 -> kannada; 3 -> latinExt; else -> kohinoor }), start, end)
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

/** The single text primitive of the app: every visible string goes through script-aware font mapping. */
@Composable
fun Sx(text: String, modifier: Modifier = Modifier, size: TextUnit = 16.sp, weight: FontWeight = FontWeight.Normal, color: Color = LocalTokens.current.text,
       align: TextAlign? = null, italic: Boolean = false, maxLines: Int = Int.MAX_VALUE, lineHeight: TextUnit = TextUnit.Unspecified) {
    val a = remember(text) { Fonts.annotate(text) }
    BasicText(a, modifier = modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
        style = TextStyle(fontSize = size, fontWeight = weight, color = color, textAlign = align ?: TextAlign.Unspecified,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            lineHeight = if (lineHeight == TextUnit.Unspecified) (size.value * 1.55f).sp else lineHeight, fontFamily = Fonts.kohinoor))
}

/** Applies script-aware fonts inside text fields (identity offset mapping). */
object ScriptTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString) = TransformedText(Fonts.annotate(text.text), OffsetMapping.Identity)
}
