package org.sandhimitra.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Cursor-aware editing helpers shared by the in-app keyboard and its host screens. */
object Edit {
    fun insert(v: TextFieldValue, s: String): TextFieldValue {
        val a = v.selection.min.coerceIn(0, v.text.length); val b = v.selection.max.coerceIn(0, v.text.length)
        return TextFieldValue(v.text.replaceRange(a, b, s), TextRange(a + s.length))
    }
    fun backspace(v: TextFieldValue): TextFieldValue {
        val a = v.selection.min; val b = v.selection.max
        return if (a != b) TextFieldValue(v.text.removeRange(a, b), TextRange(a))
        else if (a > 0) TextFieldValue(v.text.removeRange(a - 1, a), TextRange(a - 1)) else v
    }
    fun move(v: TextFieldValue, d: Int): TextFieldValue { val p = (v.selection.max + d).coerceIn(0, v.text.length); return TextFieldValue(v.text, TextRange(p)) }
}

private val devVowels = "अ आ इ ई उ ऊ ऋ ॠ ऌ ए ऐ ओ औ".split(" ")
private val devSigns = "ा ि ी ु ू ृ ॄ े ै ो ौ".split(" ")
private val devMarks = "ं ः ँ ऽ ् ३".split(" ")
private val devCons = listOf("क ख ग घ ङ", "च छ ज झ ञ", "ट ठ ड ढ ण", "त थ द ध न", "प फ ब भ म", "य र ल व श ष स ह ळ").map { it.split(" ") }
private val devConj = "क्ष त्र ज्ञ श्र त्त द्ध द्य द्व न्त न्द स्त ह्म".split(" ")
private val iastRows = listOf("ā ī ū ṛ ṝ ḷ ḹ ṃ ḥ", "ṅ ñ ṭ ḍ ṇ ś ṣ ' 3", "q w e r t y u i o p", "a s d f g h j k l", "z x c v b n m").map { it.split(" ") }

/** In-app hybrid keyboard: Devanagari varṇamālā with signs, virāma, anusvāra, visarga, avagraha and conjunct shortcuts; and an IAST layout. */
@Composable
fun SanskritKeyboard(onText: (String) -> Unit, onBackspace: () -> Unit, onMove: (Int) -> Unit, onClear: () -> Unit, onHide: () -> Unit) {
    val t = LocalTokens.current
    var layout by rememberSaveable { mutableIntStateOf(0) }
    Column(Modifier.fillMaxWidth().background(t.card).border(BorderStroke(1.dp, t.outline)).navigationBarsPadding().padding(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Capsule(listOf(tr("kb_deva"), tr("kb_iast")), layout, { layout = it }, Modifier.weight(1f), textSize = 13)
            Sx("▾", size = 22.sp, color = t.primary, modifier = Modifier.clickable { onHide() }.padding(horizontal = 14.dp, vertical = 4.dp))
        }
        Column(Modifier.heightIn(max = 236.dp).verticalScroll(rememberScrollState())) {
            if (layout == 0) {
                KRow(devVowels.take(7), onText); KRow(devVowels.drop(7), onText)
                KRow(devSigns.take(6), onText, base = "◌"); KRow(devSigns.drop(6), onText, base = "◌")
                KRow(devMarks, onText)
                devCons.forEach { KRow(it, onText) }
                Sx(tr("kb_conj"), size = 11.sp, color = t.muted, modifier = Modifier.padding(top = 4.dp))
                KRow(devConj.take(6), onText); KRow(devConj.drop(6), onText)
            } else iastRows.forEach { KRow(it, onText) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Key("←", Modifier.weight(1f)) { onMove(-1) }
            Key(tr("kb_space"), Modifier.weight(3f)) { onText(" ") }
            Key("+", Modifier.weight(1f)) { onText(" + ") }
            Key("→", Modifier.weight(1f)) { onMove(1) }
            Key("⌫", Modifier.weight(1f)) { onBackspace() }
            Key("✕", Modifier.weight(1f)) { onClear() }
        }
    }
}

@Composable
private fun KRow(keys: List<String>, onText: (String) -> Unit, base: String = "") {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        keys.forEach { k -> Key(base + k, Modifier.weight(1f)) { onText(k) } }
    }
}

@Composable
private fun Key(label: String, modifier: Modifier, onClick: () -> Unit) {
    val t = LocalTokens.current
    val shape = RoundedCornerShape(8.dp)
    Box(modifier.heightIn(min = 40.dp).clip(shape).background(t.bg).border(BorderStroke(1.dp, t.outline), shape).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Sx(label, size = 18.sp, weight = FontWeight.Medium, color = t.text, align = TextAlign.Center, lineHeight = 24.sp, maxLines = 1)
    }
}
