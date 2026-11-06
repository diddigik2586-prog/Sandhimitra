package org.sandhimitra.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import org.sandhimitra.app.R
import org.sandhimitra.app.SandhimitraApp
import org.sandhimitra.app.data.QuizQuestion
import org.sandhimitra.app.engine.Script

// ---------------------------------------------------------------- Sandhicakram landing: four square category cards, no scrolling

@Composable
fun GamesScreen(app: SandhimitraApp, onMenu: () -> Unit, onBack: () -> Unit, go: (String) -> Unit) {
    val t = LocalTokens.current
    val cats = listOf("vowel", "consonant", "visarga", "prakriti")
    Column(Modifier.fillMaxSize().background(t.bg)) {
        GlobalHeader(onMenu)
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.semantics { contentDescription = "Back" }) { Icon(Icons.Default.ArrowBack, null, tint = t.primary) }
            Icon(painterResource(R.drawable.ic_gamepad), contentDescription = null, tint = t.accent, modifier = Modifier.size(26.dp).padding(end = 4.dp))
            Sx(tr("games"), size = 20.sp, weight = FontWeight.SemiBold, color = t.primary, modifier = Modifier.weight(1f))
        }
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
            val gap = 12.dp
            val side = minOf((maxWidth - gap) / 2, (maxHeight - gap) / 2)
            Column(Modifier.align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(gap)) {
                for (row in 0..1) Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    for (col in 0..1) { val c = cats[row * 2 + col]; GameTile(app, c, side, catColor(t, c)) { go(Routes.quiz(c)) } }
                }
            }
        }
    }
}

private fun catColor(t: Tokens, cat: String): Color = when (cat) {
    "vowel" -> t.primary
    "consonant" -> t.accent
    "visarga" -> if (t.dark) Color(0xFF8FA3D8) else Color(0xFF5B6EA6)
    else -> if (t.dark) Color(0xFFE8C3A0) else Color(0xFF9C5F3E)
}

@Composable
private fun GameTile(app: SandhimitraApp, cat: String, side: androidx.compose.ui.unit.Dp, accent: Color, onClick: () -> Unit) {
    val t = LocalTokens.current
    val total = remember(cat) { app.quiz.count(cat) }
    val done = app.progress.version.let { app.progress.quizDoneCount(cat) }
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.testTag("game_$cat").size(side).clip(shape).background(t.card).border(BorderStroke(2.dp, accent), shape).clickable(onClick = onClick).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.width(40.dp).height(5.dp).background(accent, RoundedCornerShape(3.dp)))
        Spacer(Modifier.height(10.dp))
        Sx(tr(cat), size = 19.sp, weight = FontWeight.SemiBold, color = t.primary, align = TextAlign.Center, lineHeight = 25.sp)
        Spacer(Modifier.height(10.dp))
        Sx("$done / $total", size = 16.sp, weight = FontWeight.Medium, color = accent, align = TextAlign.Center)
        Sx(tr("completed_q"), size = 12.sp, color = t.muted, align = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(progress = { if (total == 0) 0f else done.toFloat() / total }, modifier = Modifier.fillMaxWidth(0.8f).height(7.dp).clip(RoundedCornerShape(4.dp)), color = accent, trackColor = t.outline)
    }
}

// ---------------------------------------------------------------- session state (persisted as JSON in the local database)

class QuizSession(val cat: String, val seq: List<String>) {
    var pos = 0; var correct = 0; var wrong = 0; var score = 0
    val history = JSONArray()                       // {q, chosen, ok}
    val vik = HashMap<String, MutableSet<Int>>()    // question id -> alternatives already practised
    var navFrom = ""                                // "lesson" / "practice" when the user left from a wrong answer
    val finished get() = pos >= seq.size

    fun toJson(): String {
        val v = JSONObject(); vik.forEach { (k, s) -> v.put(k, JSONArray(s.toList())) }
        return JSONObject().put("cat", cat).put("seq", JSONArray(seq)).put("pos", pos).put("correct", correct).put("wrong", wrong).put("score", score)
            .put("history", history).put("vik", v).put("nav", navFrom).put("v", 1).toString()
    }
    companion object {
        fun fromJson(s: String): QuizSession? = try {
            val o = JSONObject(s); val a = o.getJSONArray("seq")
            QuizSession(o.getString("cat"), (0 until a.length()).map { a.getString(it) }).apply {
                pos = o.getInt("pos"); correct = o.getInt("correct"); wrong = o.getInt("wrong"); score = o.getInt("score"); navFrom = o.optString("nav", "")
                val h = o.getJSONArray("history"); for (i in 0 until h.length()) history.put(h.getJSONObject(i))
                val v = o.getJSONObject("vik"); for (k in v.keys()) { val arr = v.getJSONArray(k); vik[k] = (0 until arr.length()).map { arr.getInt(it) }.toMutableSet() }
            }
        } catch (e: Exception) { null }

        /** A new mixed round: forward and reverse questions shuffled separately and interleaved so both directions alternate. */
        fun fresh(cat: String, qs: List<QuizQuestion>, seed: Long = System.currentTimeMillis()): QuizSession {
            val rnd = java.util.Random(seed)
            val f = qs.filter { it.dir == "F" }.map { it.id }.shuffled(rnd); val r = qs.filter { it.dir == "R" }.map { it.id }.shuffled(rnd)
            val seq = ArrayList<String>(); var i = 0; var j = 0; var takeF = rnd.nextBoolean()
            while (i < f.size || j < r.size) {
                if ((takeF && i < f.size) || j >= r.size) seq += f[i++] else seq += r[j++]
                takeF = !takeF
            }
            return QuizSession(cat, seq)
        }
    }
}

// ---------------------------------------------------------------- the mixed quiz

@Composable
fun QuizScreen(app: SandhimitraApp, cat: String, onMenu: () -> Unit, onBack: () -> Unit, go: (String) -> Unit) {
    val t = LocalTokens.current; val lang = LocalLang.current
    val questions = remember(cat) { app.quiz.questions(cat) }
    val byId = remember(cat) { questions.associateBy { it.id } }
    // the saved session is restored exactly (position, score, history, vikalpa state); a new one is started only when none exists
    val session = remember(cat) { app.progress.quizSession(cat)?.let { QuizSession.fromJson(it) }?.takeIf { s -> s.seq.all { it in byId } } ?: QuizSession.fresh(cat, questions).also { app.progress.saveQuizSession(cat, it.toJson()) } }
    var tick by remember { mutableIntStateOf(0) }
    fun save() { app.progress.saveQuizSession(cat, session.toJson()); tick++ }
    // per-question interaction state (also restored: a wrong answer the user left from is shown again with its links)
    @Suppress("UNUSED_VARIABLE") val revision = tick   // read so that every saved change recomposes this screen
    var chosen by remember(cat, session.pos) { mutableStateOf<Int?>(null) }
    var verdict by remember(cat, session.pos) { mutableStateOf("") }   // "", "ok", "ok_more", "wrong", "retry"
    LaunchedEffect(cat) {
        if (session.navFrom.isNotEmpty() && !session.finished) {
            val last = if (session.history.length() > 0) session.history.getJSONObject(session.history.length() - 1) else null
            if (last != null && last.getString("q") == session.seq[session.pos] && !last.getBoolean("ok")) { chosen = last.getInt("chosen"); verdict = "wrong" }
            session.navFrom = ""; save()
        }
    }

    if (questions.isEmpty()) { Page(onMenu, onBack, tr("games")) { Sx(tr("no_questions")) }; return }

    Page(onMenu, onBack, tr(cat)) {
        if (session.finished) {
            CardBox(Modifier.fillMaxWidth()) {
                Sx(tr("quiz_done"), size = 20.sp, weight = FontWeight.SemiBold, color = t.primary)
                Spacer(Modifier.height(8.dp))
                Sx("${lab("score")}${session.score}", size = 17.sp)
                Sx("${tr("correct_ans")} ${session.correct}   ·   ${tr("wrong_ans")} ${session.wrong}", size = 14.sp, color = t.muted)
                PrimaryButton(tr("play_again"), { val n = QuizSession.fresh(cat, questions); app.progress.saveQuizSession(cat, n.toJson()); onBack(); go(Routes.quiz(cat)) }, Modifier.padding(top = 12.dp))
            }
            return@Page
        }
        val q = byId.getValue(session.seq[session.pos])
        val practised = session.vik.getOrPut(q.id) { HashSet() }
        // header line: question number, score
        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Sx("${tr("q_of")} ${session.pos + 1} / ${session.seq.size}", size = 14.sp, color = t.muted, modifier = Modifier.weight(1f))
            Sx("${lab("score")}${session.score}", size = 14.sp, weight = FontWeight.SemiBold, color = t.accent)
        }
        LinearProgressIndicator(progress = { session.pos.toFloat() / session.seq.size }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).padding(bottom = 0.dp), color = t.primary, trackColor = t.outline)
        Spacer(Modifier.height(12.dp))
        CardBox(Modifier.fillMaxWidth().testTag("quiz_prompt")) {
            Sx(tr(if (q.dir == "F") "q_join" else "q_split"), size = 13.sp, color = t.accent, weight = FontWeight.SemiBold)
            Sx(Script.render(q.prompt, lang), size = 24.sp, weight = FontWeight.SemiBold, color = t.primary, lineHeight = 34.sp)
            if (q.vikalpa) Sx(tr("optional_form"), size = 12.sp, color = t.muted, italic = true)
        }
        Spacer(Modifier.height(12.dp))
        val locked = verdict == "ok" || verdict == "wrong" || verdict == "ok_more"
        q.options.forEachIndexed { i, opt ->
            val isAns = i in q.answers
            val col = when {
                verdict.isEmpty() || verdict == "retry" && chosen != i -> t.outline
                chosen == i && (verdict == "ok" || verdict == "ok_more") -> t.ok
                chosen == i && (verdict == "wrong" || verdict == "retry") -> t.bad
                verdict == "wrong" && isAns -> t.ok
                i in practised && verdict != "retry" -> t.ok
                else -> t.outline
            }
            val shape = RoundedCornerShape(12.dp)
            Box(Modifier.fillMaxWidth().padding(bottom = 8.dp).heightIn(min = 52.dp).clip(shape).background(t.card).border(BorderStroke(if (col == t.outline) 1.dp else 2.dp, col), shape)
                .testTag("opt_$i").semantics { contentDescription = "Option ${i + 1}" }
                .clickable(enabled = !locked) {
                    if (locked) return@clickable
                    chosen = i
                    if (isAns) {
                        if (q.vikalpa && i in practised) { verdict = "retry"; return@clickable }     // the exercise asks for the other valid form
                        practised += i
                        session.history.put(JSONObject().put("q", q.id).put("chosen", i).put("ok", true))
                        session.correct++; session.score += 10
                        if (q.vikalpa && practised.size < q.answers.size) verdict = "ok_more" else { verdict = "ok"; app.progress.markQuizDone(cat, q.id) }
                        save()
                    } else {
                        session.history.put(JSONObject().put("q", q.id).put("chosen", i).put("ok", false)); session.wrong++
                        verdict = "wrong"; save()
                    }
                }.padding(horizontal = 14.dp, vertical = 12.dp), contentAlignment = Alignment.CenterStart) {
                Sx(Script.render(opt, lang), size = 19.sp, color = if (col == t.outline) t.text else col, weight = if (col == t.outline) FontWeight.Normal else FontWeight.SemiBold, lineHeight = 27.sp)
            }
        }
        // verdict
        when (verdict) {
            "ok" -> { Sx(tr("correct_ans"), size = 18.sp, weight = FontWeight.SemiBold, color = t.ok, modifier = Modifier.testTag("verdict_ok")); LaunchedEffect(q.id, practised.size) { delay(900); session.pos++; verdict = ""; chosen = null; save() } }
            "ok_more" -> { Sx(tr("vik_next"), size = 16.sp, weight = FontWeight.SemiBold, color = t.ok); LaunchedEffect(q.id, practised.size) { delay(1100); verdict = ""; chosen = null } }
            "retry" -> Sx(tr("try_again"), size = 16.sp, weight = FontWeight.SemiBold, color = t.bad)
            "wrong" -> {
                Sx(tr("wrong_ans"), size = 18.sp, weight = FontWeight.SemiBold, color = t.bad, modifier = Modifier.testTag("verdict_wrong"))
                Sx("${lab("right_answer")}${q.answers.joinToString(" / ") { Script.render(q.options[it], lang) }}", size = 15.sp, color = t.ok)
                Spacer(Modifier.height(6.dp))
                Sx("${lab("explanation")}${when (lang) { "kn" -> q.exKn; "en" -> q.exEn; else -> q.exSa }}", size = 14.sp, color = t.muted)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton(tr("read_lesson"), { session.navFrom = "lesson"; save(); go(Routes.topic(q.lesson)) }, Modifier.weight(1f))
                    PrimaryButton(tr("view_practice"), { session.navFrom = "practice"; save(); go(Routes.pset(q.practice)) }, Modifier.weight(1f), filled = false)
                }
                Spacer(Modifier.height(8.dp))
                PrimaryButton(tr("next_q"), { session.pos++; verdict = ""; chosen = null; save() }, Modifier.fillMaxWidth(), filled = false)
            }
        }
    }
}

// ---------------------------------------------------------------- bookmarks

@Composable
fun BookmarksScreen(app: SandhimitraApp, onMenu: () -> Unit, onBack: () -> Unit, go: (String) -> Unit) {
    val t = LocalTokens.current; val lang = LocalLang.current
    val ids = app.progress.version.let { app.progress.bookmarks() }
    Page(onMenu, onBack, tr("bookmarks")) {
        if (ids.isEmpty()) { Sx(tr("no_bookmarks"), color = t.muted); return@Page }
        ids.forEach { id ->
            val ref = app.content.ref(id) ?: return@forEach
            val group = app.content.groupOf(id)
            CardBox(Modifier.fillMaxWidth().padding(bottom = 10.dp), onClick = { go(Routes.topic(id)) }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Sx(ref.title(lang), size = 17.sp, weight = FontWeight.Medium)
                        Sx(tr(group ?: "topics"), size = 13.sp, color = t.muted)
                    }
                    BookmarkToggle(app, id)
                }
            }
        }
    }
}

/** Outlined when not bookmarked, filled when bookmarked; one tap toggles, no dialog. */
@Composable
fun BookmarkToggle(app: SandhimitraApp, id: String) {
    val t = LocalTokens.current
    val on = app.progress.version.let { app.progress.isBookmarked(id) }
    IconButton(onClick = { app.progress.setBookmark(id, !on) }, modifier = Modifier.semantics { contentDescription = if (on) "Remove bookmark" else "Bookmark" }.testTag("bm_$id")) {
        Icon(painterResource(if (on) R.drawable.ic_bookmark else R.drawable.ic_bookmark_outline), contentDescription = null, tint = t.accent, modifier = Modifier.size(28.dp))
    }
}
