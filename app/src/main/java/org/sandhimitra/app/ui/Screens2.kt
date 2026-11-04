package org.sandhimitra.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.sandhimitra.app.SandhimitraApp
import org.sandhimitra.app.data.*
import org.sandhimitra.app.engine.Script

private fun tx(s: String, lang: String) = Script.render(s, lang)

@Composable
fun IntroScreen(app: SandhimitraApp, onMenu: () -> Unit, onBack: () -> Unit) {
    val lang = LocalLang.current
    val topics = remember { app.content.topics("intro") }
    var open by rememberSaveable { mutableIntStateOf(0) }
    Page(onMenu, onBack, tr("introduction")) {
        topics.forEachIndexed { i, ref ->
            CardBox(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { open = if (open == i) -1 else i }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sx(ref.title(lang), size = 17.sp, weight = FontWeight.SemiBold, color = LocalTokens.current.primary, modifier = Modifier.weight(1f))
                    Sx(if (open == i) "▾" else "▸", size = 18.sp, color = LocalTokens.current.accent)
                }
                if (open == i) {
                    val topic = remember(ref.id) { app.content.topic(ref.id) }
                    Spacer(Modifier.height(6.dp))
                    LessonBody(topic, lang)
                }
            }
        }
    }
}

@Composable
fun CategoryScreen(app: SandhimitraApp, group: String, onMenu: () -> Unit, onBack: () -> Unit, go: (String) -> Unit) {
    val lang = LocalLang.current
    val topics = remember(group) { app.content.topics(group) }
    val title = tr(group)
    Page(onMenu, onBack, title) {
        val done = app.progress.version.let { app.progress.doneCount(topics.map { it.id }) }
        Sx("${lab("progress")}$done / ${topics.size}", size = 13.sp, color = LocalTokens.current.muted, modifier = Modifier.padding(bottom = 8.dp))
        topics.forEach { ref ->
            val isDone = app.progress.version.let { app.progress.isLessonDone(ref.id) }
            CardBox(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { go(Routes.topic(ref.id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Sx(ref.title(lang), size = 17.sp, weight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    if (isDone) Sx("✓", size = 18.sp, weight = FontWeight.Bold, color = LocalTokens.current.ok)
                }
            }
        }
    }
}

@Composable
fun TopicScreen(app: SandhimitraApp, id: String, onMenu: () -> Unit, onBack: () -> Unit, practiceOnly: Boolean) {
    val lang = LocalLang.current
    val topic = remember(id) { app.content.topic(id) }
    val hasLp = topic.group in listOf("vowel", "consonant", "visarga")
    var mode by rememberSaveable(id) { mutableIntStateOf(if (practiceOnly) 1 else 0) }
    val title = when (lang) { "en" -> topic.en.title; "kn" -> topic.kn.title; else -> topic.sa.title }
    Page(onMenu, onBack, title) {
        if (hasLp) Capsule(listOf(tr("lesson"), tr("practice")), mode, { mode = it }, Modifier.padding(bottom = 12.dp))
        if (mode == 0 && !practiceOnly) {
            LessonBody(topic, lang)
            LessonActions(app, topic.id)
        } else PracticeBody(app, topic, lang)
    }
}

@Composable
private fun LessonActions(app: SandhimitraApp, id: String) {
    val done = app.progress.version.let { app.progress.isLessonDone(id) }
    val bm = app.progress.version.let { app.progress.isBookmarked(id) }
    Column(Modifier.fillMaxWidth().padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PrimaryButton(if (done) tr("unmark_done") else tr("mark_done"), { app.progress.setLessonDone(id, !done) }, Modifier.fillMaxWidth(), filled = !done)
        PrimaryButton(if (bm) tr("bookmarked") else tr("bookmark"), { app.progress.setBookmark(id, !bm) }, Modifier.fillMaxWidth(), filled = false)
    }
}

private val labelsCache = HashMap<Int, Triple<String, String, String>>()
private fun secLabel(n: Int, lang: String): String {
    val l = labelsCache.getOrPut(n) {
        when (n) {
            1 -> Triple("प्रकरणनाम", "ಪ್ರಕರಣದ ಹೆಸರು", "Topic"); 2 -> Triple("प्रस्तावना", "ಪ್ರಸ್ತಾವನೆ", "Introduction"); 3 -> Triple("स्वरूपम्", "ಸ್ವರೂಪ", "Nature of the rule")
            4 -> Triple("सूत्राणि", "ಸೂತ್ರಗಳು", "Sūtras"); 5 -> Triple("पदच्छेदः अर्थश्च", "ಪದಚ್ಛೇದ ಮತ್ತು ಅರ್ಥ", "Word analysis"); 6 -> Triple("नियमविवरणम्", "ನಿಯಮ ವಿವರಣೆ", "Explanation")
            7 -> Triple("प्रक्रियादर्शनम्", "ಪ್ರಕ್ರಿಯಾ ದರ್ಶನ", "Derivations"); 8 -> Triple("उदाहरणानि", "ಉದಾಹರಣೆಗಳು", "Examples"); 9 -> Triple("विकल्पः अपवादाश्च", "ವಿಕಲ್ಪ ಮತ್ತು ಅಪವಾದ", "Options and exceptions")
            10 -> Triple("विशेषावधानानि", "ವಿಶೇಷ ಸೂಚನೆಗಳು", "Special notes"); 11 -> Triple("स्मरणीयांशाः", "ನೆನಪಿಡಬೇಕಾದವು", "To remember"); 12 -> Triple("अभ्यासः", "ಅಭ್ಯಾಸ", "Exercises")
            13 -> Triple("स्वाध्यायप्रश्नाः", "ಸ್ವಾಧ್ಯಾಯ ಪ್ರಶ್ನೋತ್ತರಗಳು", "Self-study questions"); else -> Triple("प्रकरणसारः", "ಪ್ರಕರಣಸಾರ", "Summary")
        }
    }
    return when (lang) { "kn" -> l.second; "en" -> l.third; else -> l.first }
}

/** Lesson content. Sanskrit/Kannada modes show the full source sections; English shows the independently composed layer plus source derivations and examples in IAST. */
@Composable
fun LessonBody(topic: Topic, lang: String) {
    val t = LocalTokens.current
    if (lang == "en") {
        val en = topic.en
        SectionTitle(tr("overview"))
        Sx(en.overview)
        if (en.rules.isNotEmpty()) {
            SectionTitle(tr("sutras"))
            en.rules.forEach { r ->
                CardBox(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    Sx(tx(r.sutra, "en"), weight = FontWeight.SemiBold, color = t.primary)
                    Sx("(${r.ref})", size = 13.sp, color = t.accent)
                    Sx(r.gloss, size = 15.sp)
                }
            }
        }
        if (en.notes.isNotEmpty()) {
            SectionTitle(tr("notes"))
            en.notes.forEach { n -> Bullet(n) }
        }
        val sa = topic.sa
        sa.sections.firstOrNull { it.n == 7 }?.let { s -> if (s.paras.isNotEmpty()) { SectionTitle(tr("derivations")); s.paras.forEach { Sx(tx(it, "en"), size = 15.sp, modifier = Modifier.padding(bottom = 8.dp)) } } }
        if (sa.ex.size > 1) { SectionTitle(tr("examples")); ExampleRows(sa.ex, "en", showComment = false) }
        Sx(tr("source_note_en"), size = 12.sp, color = t.muted, italic = true, modifier = Modifier.padding(top = 12.dp))
        return
    }
    val lc = topic.lc(lang)
    lc.sections.filter { it.n != 12 && it.n != 13 }.forEach { s ->
        if (s.paras.isEmpty() && s.n != 8) return@forEach
        SectionTitle("${s.n}. ${secLabel(s.n, lang)}")
        s.paras.forEach { p -> RichPara(p) }
        if (s.n == 8 && lc.ex.size > 1) ExampleRows(lc.ex, lang, showComment = true)
    }
}

@Composable
private fun RichPara(p: String) {
    val parts = p.split(" • ").map { it.trim().removePrefix("•").trim() }.filter { it.isNotEmpty() }
    if (parts.size > 1 || p.trim().startsWith("•")) parts.forEach { Bullet(it) } else Sx(p, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun Bullet(text: String) {
    Row(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        if (LocalAlign.current == null) Sx("•", color = LocalTokens.current.accent, modifier = Modifier.padding(end = 8.dp))
        Sx(text, modifier = Modifier.weight(1f))
    }
}

@Composable
fun ExampleRows(rows: List<List<String>>, lang: String, showComment: Boolean) {
    val t = LocalTokens.current
    rows.drop(1).forEach { r ->
        if (r.size < 3) return@forEach
        CardBox(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Sx("${tx(r[0], lang)}  →  ${tx(r[1], lang)}", weight = FontWeight.SemiBold, color = t.primary)
            Sx(tx(r[2], lang), size = 15.sp)
            if (showComment && r.size > 3 && r[3].isNotBlank()) Sx(tx(r[3], lang), size = 13.sp, color = t.muted, italic = true)
        }
    }
}

// ----------------------------------------------------------------------- practice
private data class Item(val id: String, val q: String, val a: String)

private fun parseJoin(row: List<String>): Pair<List<String>, String>? {
    if (row.size < 3) return null
    val left = row[0].replace(Regex("\\(.*?\\)"), "").trim()
    val parts = left.split("+").map { it.trim() }
    if (parts.size != 2 || parts.any { it.isEmpty() || it.contains(' ') || it.contains(',') }) return null
    val res = row[1].trim()
    if (res.contains(' ') || res.contains('(') || res.contains(',')) return null
    return parts to res
}

@Composable
fun PracticeBody(app: SandhimitraApp, topic: Topic, lang: String) {
    val t = LocalTokens.current
    val id = topic.id
    val lc = if (lang == "kn") topic.kn else topic.sa
    val gen = remember(id, lang) {
        val rows = lc.ex.drop(1).mapIndexedNotNull { i, r -> parseJoin(r)?.let { (p, res) -> Triple(i, p, res to r[2]) } }.take(5)
        buildList {
            rows.forEach { (i, p, resOp) ->
                add(Item("j$i", "${S.t("q_join", lang)} ${tx(p[0], lang)} + ${tx(p[1], lang)}", "${tx(resOp.first, lang)}   (${tx(resOp.second, lang)})"))
                add(Item("s$i", "${S.t("q_split", lang)} ${tx(resOp.first, lang)}", "${tx(p[0], lang)} + ${tx(p[1], lang)}   (${tx(resOp.second, lang)})"))
            }
        }
    }
    val src = remember(id, lang) { lc.pr.drop(1).filter { it.size >= 3 }.mapIndexed { i, r -> Item("p$i", tx(r[1], lang), tx(r[2], lang)) } }
    val qa = remember(id, lang) { lc.sections.firstOrNull { it.n == 13 }?.paras.orEmpty().mapIndexedNotNull { i, p -> splitQa(p)?.let { (q, a) -> Item("q$i", tx(q, lang), tx(a, lang)) } } }
    val recall = remember(id, lang) { if (lang == "en") topic.en.rules.mapIndexed { i, r -> Item("r$i", S.t("q_recall", "en") + r.gloss, "${tx(r.sutra, "en")} (${r.ref})") } else emptyList() }
    val all = src + qa + gen + recall
    if (all.isEmpty()) { Sx(tr("no_practice")); return }
    @Composable fun block(title: String, items: List<Item>) { if (items.isNotEmpty()) { SectionTitle(title); items.forEach { QuestionCard(app, id, it) } } }
    block(tr("source_exercises"), src)
    block(tr("selfstudy"), qa)
    block(tr("generated"), gen)
    block(tr("recall"), recall)
}

@Composable
private fun QuestionCard(app: SandhimitraApp, topic: String, item: Item) {
    val t = LocalTokens.current
    var shown by rememberSaveable(topic, item.id) { mutableStateOf(false) }
    CardBox(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Sx(item.q, weight = FontWeight.Medium)
        if (shown) {
            Spacer(Modifier.height(6.dp)); Sx(item.a, color = t.primary)
        }
        PrimaryButton(if (shown) tr("hide_answer") else tr("show_answer"), { shown = !shown }, Modifier.padding(top = 10.dp), filled = false)
    }
}
