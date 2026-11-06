package org.sandhimitra.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sandhimitra.app.SandhimitraApp
import org.sandhimitra.app.engine.*

/** Renders SLP1 in the interface script: Sanskrit → Devanagari, Kannada → Kannada script, English → IAST. */
fun sl(s: String, lang: String) = when (lang) { "kn" -> Script.slp1ToKannada(s); "en" -> Script.slp1ToIast(s); else -> Script.slp1ToDevanagari(s) }
private fun slTokens(s: String, lang: String) = s.split("+").joinToString(" + ") { sl(it, lang) }

@Composable
fun SandhatriHome(app: SandhimitraApp, onMenu: () -> Unit, onBack: () -> Unit, go: (String) -> Unit) {
    val lang = LocalLang.current; val t = LocalTokens.current
    val sets = remember { app.content.topics("practice") }
    Page(onMenu, onBack, tr("sandhatri")) {
        Sx(tr("engine_intro"), size = 15.sp, color = t.muted, modifier = Modifier.padding(bottom = 12.dp))
        listOf(Routes.YOJANA to "yojana", Routes.VICCHEDA to "viccheda", Routes.REGISTRY to "registry").forEach { (r, k) ->
            CardBox(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { go(r) }) { Sx(tr(k), size = 19.sp, weight = FontWeight.SemiBold, color = t.primary) }
        }
        SectionTitle(tr("practice_sets"))
        sets.forEach { ref -> CardBox(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { go(Routes.pset(ref.id)) }) { Sx(ref.title(lang), size = 16.sp, weight = FontWeight.Medium) } }
    }
}

@Composable
private fun EngineField(label: String, value: TextFieldValue, onChange: (TextFieldValue) -> Unit, readOnly: Boolean, onFocus: () -> Unit) {
    val t = LocalTokens.current
    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Sx(label, size = 13.sp, color = t.muted, modifier = Modifier.padding(bottom = 4.dp))
        BasicTextField(value, { nv ->
            val fixed = Script.fixTyped(nv.text)
            onChange(if (fixed == nv.text) nv else TextFieldValue(fixed, androidx.compose.ui.text.TextRange(Script.fixTyped(nv.text.substring(0, nv.selection.end.coerceIn(0, nv.text.length))).length)))
        }, readOnly = readOnly, singleLine = true, visualTransformation = ScriptTransformation,
            // tall line box so Devanagari matras above and below (ि ी ु ृ, conjuncts) are never clipped by the border
            textStyle = TextStyle(fontSize = 22.sp, color = t.text, lineHeight = 40.sp,
                lineHeightStyle = androidx.compose.ui.text.style.LineHeightStyle(androidx.compose.ui.text.style.LineHeightStyle.Alignment.Center, androidx.compose.ui.text.style.LineHeightStyle.Trim.None)),
            cursorBrush = SolidColor(t.accent),
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).border(BorderStroke(1.dp, t.primary), RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 12.dp).onFocusChanged { if (it.isFocused) onFocus() })
    }
}

private fun copyText(ctx: Context, text: String) {
    (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Sandhātrī", text))
    Toast.makeText(ctx, S.t("copied", (ctx.applicationContext as SandhimitraApp).prefs.lang), Toast.LENGTH_SHORT).show()
}
private fun shareText(ctx: Context, text: String) {
    ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null))
}
private fun sharePdf(ctx: Context, lines: List<String>) {
    try {
        val f = PdfExport.write(ctx, "Sandhātrī", lines)
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("application/pdf").putExtra(Intent.EXTRA_STREAM, uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
    } catch (e: Exception) { Toast.makeText(ctx, e.message ?: "PDF error", Toast.LENGTH_LONG).show() }
}

@Composable
private fun ExportRow(lines: List<String>) {
    val ctx = LocalContext.current
    val text = lines.joinToString("\n")
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PrimaryButton(tr("copy"), { copyText(ctx, text) }, filled = false)
        PrimaryButton(tr("share"), { shareText(ctx, text) }, filled = false)
        PrimaryButton(tr("pdf"), { sharePdf(ctx, lines) }, filled = false)
    }
}

private fun noteText(n: String, lang: String): String = when {
    n == "need_two_parts" -> S.t("need_two", lang)
    n.startsWith("invalid_char:") -> S.t("invalid_chars", lang) + n.removePrefix("invalid_char:")
    n == "unsupported_final" -> S.t("unsupported_final", lang)
    n.startsWith("component_unverified:") -> S.t("component_unverified", lang) + sl(n.removePrefix("component_unverified:"), lang)
    n == "padanta_conjunct" -> S.t("padanta_conjunct", lang)
    n == "too_short" || n == "too_long" -> S.t("no_supported", lang)
    n == "no_supported_analysis" -> S.t("no_supported", lang)
    else -> ""
}

@Composable
private fun StepsView(steps: List<Step>) {
    val lang = LocalLang.current; val t = LocalTokens.current
    if (steps.isEmpty()) { Sx(tr("no_change"), size = 14.sp, color = t.muted); return }
    steps.forEachIndexed { i, st ->
        val r = Rules.find(st.ruleId)
        Column(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Sx("${i + 1}. ${Script.render(r?.name ?: st.ruleId, lang)} (${r?.ref ?: ""})", size = 14.sp, weight = FontWeight.SemiBold, color = t.primary)
            val sutra = r?.sutraDeva ?: ""
            if (sutra.isNotEmpty()) Sx(Script.render(sutra, lang), size = 13.sp, color = t.muted)
            if (st.from.isNotEmpty() || st.to.isNotEmpty()) Sx("${slTokens(st.from, lang)}  →  ${if (st.to.isEmpty()) "∅" else slTokens(st.to, lang)}${if (st.note in setOf("vartika", "stem", "dual")) "   [${S.t(st.note, lang)}]" else if (st.note.isNotEmpty() && st.note != "unchanged") "   [${st.note}]" else ""}", size = 14.sp)
        }
    }
}

private fun categoriesOf(ids: List<String>) = ids.mapNotNull { Rules.find(it)?.category }.distinct()
private fun catKey(c: Category) = when (c) { Category.VOWEL -> "cat_vowel"; Category.CONSONANT -> "cat_consonant"; Category.VISARGA -> "cat_visarga"; Category.PRAKRITIBHAVA -> "cat_prakriti" }

@Composable
private fun SurfaceBlock(surface: String, optional: Boolean) {
    val lang = LocalLang.current; val t = LocalTokens.current
    Sx(Script.slp1ToDevanagari(surface), size = 26.sp, weight = FontWeight.SemiBold, color = t.text)
    if (lang != "sa") Sx(sl(surface, lang), size = 18.sp, color = t.muted)
    if (optional) Sx(tr("optional_form"), size = 12.sp, color = t.accent)
}

// =================================================================== Yojana (forward)
@Composable
fun YojanaScreen(app: SandhimitraApp, onMenu: () -> Unit, onBack: () -> Unit) {
    val lang = LocalLang.current; val t = LocalTokens.current
    val parts: SnapshotStateList<TextFieldValue> = remember { mutableStateListOf(TextFieldValue(""), TextFieldValue("")) }
        var pragrhya by rememberSaveable { mutableStateOf(false) }
    var pada by rememberSaveable { mutableStateOf(true) }
    var result by remember { mutableStateOf<JoinResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hist = app.progress.version.let { app.progress.history("join") }

    fun run() {
        val inputs = parts.map { it.text.trim() }.filter { it.isNotEmpty() }
        busy = true
        scope.launch {
            val r = withContext(Dispatchers.Default) { app.engine.joinAll(inputs, Ctx(leftPragrhya = pragrhya, leftPada = pada)) }
            result = r; busy = false
            if (r.status != Status.NO_RESULT) app.progress.addHistory("join", inputs.joinToString(" + "))
        }
    }

    Page(onMenu, onBack, tr("yojana")) {
        Row(verticalAlignment = Alignment.Bottom) {
            Box(Modifier.weight(1f)) { EngineField(tr("part1"), parts[0], { parts[0] = it }, readOnly = false, onFocus = {}) }
            Sx("+", size = 26.sp, weight = FontWeight.Bold, color = t.accent, modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 22.dp))
            Box(Modifier.weight(1f)) { EngineField(tr("part2"), parts[1], { parts[1] = it }, readOnly = false, onFocus = {}) }
        }
        for (i in 2 until parts.size) {
            Row(verticalAlignment = Alignment.Bottom) {
                Sx("+", size = 26.sp, weight = FontWeight.Bold, color = t.accent, modifier = Modifier.padding(end = 8.dp, bottom = 22.dp))
                Box(Modifier.weight(1f)) { EngineField("${tr("part_n")} ${i + 1}", parts[i], { parts[i] = it }, readOnly = false, onFocus = {}) }
                Sx("✕", size = 20.sp, color = t.bad, modifier = Modifier.padding(start = 8.dp, bottom = 22.dp).clickable { parts.removeAt(i) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 6.dp)) {
            if (parts.size < 5) PrimaryButton("+ ${tr("add_part")}", { parts.add(TextFieldValue("")) }, filled = false)
            PrimaryButton(tr("clear"), { parts.indices.forEach { parts[it] = TextFieldValue("") }; result = null }, filled = false)
        }
        ToggleRow(tr("opt_pragrhya"), pragrhya) { pragrhya = it }
        ToggleRow(tr("opt_pada"), pada) { pada = it }
        PrimaryButton(tr("run"), { run() }, Modifier.fillMaxWidth().padding(vertical = 10.dp))
        if (busy) Sx("…", color = t.muted)
        result?.let { r -> JoinResultView(r, parts.map { it.text }) }
        if (hist.isNotEmpty()) {
            SectionTitle(tr("history"))
            hist.forEach { h -> Sx(h, size = 15.sp, color = t.primary, modifier = Modifier.fillMaxWidth().clickable {
                val ps = h.split(" + "); parts.clear(); ps.forEach { parts.add(TextFieldValue(it)) }; while (parts.size < 2) parts.add(TextFieldValue(""))
            }.padding(vertical = 6.dp)) }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    val t = LocalTokens.current
    Row(Modifier.fillMaxWidth().clickable { onChange(!value) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(22.dp).border(BorderStroke(2.dp, t.primary), RoundedCornerShape(5.dp)).then(if (value) Modifier.background(t.primary, RoundedCornerShape(5.dp)) else Modifier), contentAlignment = Alignment.Center) {
            if (value) Sx("✓", size = 14.sp, color = t.onPrimary, lineHeight = 16.sp)
        }
        Sx(label, size = 15.sp, modifier = Modifier.padding(start = 12.dp).weight(1f))
    }
}

@Composable
private fun JoinResultView(r: JoinResult, rawInputs: List<String>) {
    val lang = LocalLang.current; val t = LocalTokens.current
    SectionTitle(tr("results"))
    Row(verticalAlignment = Alignment.CenterVertically) { Sx("${lab("status")}", size = 14.sp, color = t.muted); StatusChip(r.status) }
    r.notes.map { noteText(it, lang) }.filter { it.isNotEmpty() }.forEach { Sx(it, size = 13.sp, color = t.accent, modifier = Modifier.padding(top = 4.dp)) }
    if (r.alts.isEmpty()) return
    val lines = mutableListOf<String>()
    r.alts.forEachIndexed { idx, a ->
        CardBox(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            SurfaceBlock(a.surface, a.optional)
            val cats = categoriesOf(a.steps.map { it.ruleId })
            if (cats.isNotEmpty()) Sx("${lab("category")}${cats.joinToString(" · ") { S.t(catKey(it), lang) }}", size = 13.sp, color = t.accent, modifier = Modifier.padding(top = 4.dp))
            Sx(tr("steps"), size = 13.sp, color = t.muted, modifier = Modifier.padding(top = 8.dp))
            StepsView(a.steps)
        }
        val ruleLine = a.steps.joinToString("; ") { s -> Rules.find(s.ruleId)?.let { "${it.ref}" } ?: "" }.ifBlank { "—" }
        lines += "${rawInputs.filter { it.isNotBlank() }.joinToString(" + ")} → ${Script.slp1ToDevanagari(a.surface)} (${sl(a.surface, "en")}) [$ruleLine]"
    }
    ExportRow(lines)
}

// =================================================================== Viccheda (reverse)
@Composable
fun VicchedaScreen(app: SandhimitraApp, onMenu: () -> Unit, onBack: () -> Unit) {
    val lang = LocalLang.current; val t = LocalTokens.current
    var value by remember { mutableStateOf(TextFieldValue("")) }
    var result by remember { mutableStateOf<SplitResult?>(null) }
    var busy by remember { mutableStateOf(false) }
    var showUnsup by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val hist = app.progress.version.let { app.progress.history("split") }
    fun run() {
        val inp = value.text.trim(); busy = true
        scope.launch {
            val r = withContext(Dispatchers.Default) { app.engine.split(inp) }
            result = r; busy = false; showUnsup = false
            if (r.status != Status.NO_RESULT || r.unsupported.isNotEmpty()) app.progress.addHistory("split", inp)
        }
    }
    Page(onMenu, onBack, tr("viccheda")) {
        EngineField(tr("joined_input"), value, { value = it }, readOnly = false, onFocus = {})
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrimaryButton(tr("run"), { run() }, Modifier.weight(1f))
            PrimaryButton(tr("clear"), { value = TextFieldValue(""); result = null }, filled = false)
        }
        if (busy) Sx("…", color = t.muted, modifier = Modifier.padding(top = 8.dp))
        result?.let { r ->
            SectionTitle(tr("results"))
            Row(verticalAlignment = Alignment.CenterVertically) { Sx("${lab("status")}", size = 14.sp, color = t.muted); StatusChip(r.status) }
            r.notes.map { noteText(it, lang) }.filter { it.isNotEmpty() }.forEach { Sx(it, size = 14.sp, color = t.accent, modifier = Modifier.padding(top = 6.dp)) }
            val lines = mutableListOf<String>()
            // the best-supported analysis is shown; any further possible readings are listed under a collapsed heading
            r.analyses.firstOrNull()?.let { s -> SplitCard(s); lines += splitLine(s, r.input) }
            val others = r.analyses.drop(1)
            if (others.isNotEmpty()) {
                PrimaryButton((if (showUnsup) "▾ " else "▸ ") + tr("other_analyses") + " (${others.size})", { showUnsup = !showUnsup }, Modifier.padding(top = 12.dp), filled = false)
                if (showUnsup) others.forEach { s -> SplitCard(s); lines += splitLine(s, r.input) }
            }
            if (r.analyses.isNotEmpty()) ExportRow(lines)
        }
        if (hist.isNotEmpty()) {
            SectionTitle(tr("history"))
            hist.forEach { h -> Sx(h, size = 15.sp, color = t.primary, modifier = Modifier.fillMaxWidth().clickable { value = TextFieldValue(h) }.padding(vertical = 6.dp)) }
        }
    }
}

private fun splitLine(s: Split, input: String) = "$input → ${Script.slp1ToDevanagari(s.left)} + ${Script.slp1ToDevanagari(s.right)} (${sl(s.left, "en")} + ${sl(s.right, "en")}) [${s.alt.steps.mapNotNull { Rules.find(it.ruleId)?.ref }.distinct().joinToString("; ")}]"

@Composable
private fun SplitCard(s: Split) {
    val lang = LocalLang.current; val t = LocalTokens.current
    CardBox(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Sx("${Script.slp1ToDevanagari(s.left)} + ${Script.slp1ToDevanagari(s.right)}", size = 24.sp, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            StatusChip(s.status)
        }
        if (lang != "sa") Sx("${sl(s.left, lang)} + ${sl(s.right, lang)}", size = 17.sp, color = t.muted)
        if (!s.leftPada) Sx(tr("not_pada"), size = 12.sp, color = t.accent)
        val cats = categoriesOf(s.ruleIds)
        if (cats.isNotEmpty()) Sx("${lab("category")}${cats.joinToString(" · ") { S.t(catKey(it), lang) }}", size = 13.sp, color = t.accent, modifier = Modifier.padding(top = 4.dp))
        Sx(tr("steps"), size = 13.sp, color = t.muted, modifier = Modifier.padding(top = 8.dp))
        StepsView(s.alt.steps)
    }
}

// =================================================================== Registry & lexicon
@Composable
fun RegistryScreen(onMenu: () -> Unit, onBack: () -> Unit) {
    val lang = LocalLang.current; val t = LocalTokens.current
    Page(onMenu, onBack, tr("registry")) {
        Category.values().forEach { c ->
            SectionTitle(S.t(catKey(c), lang))
            Rules.all.filter { it.category == c }.forEach { r ->
                CardBox(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Sx(Script.render(r.name, lang), weight = FontWeight.SemiBold, color = t.primary)
                    Sx("${Script.render(r.sutraDeva, lang)} (${r.ref})", size = 14.sp)
                }
            }
        }
    }
}
