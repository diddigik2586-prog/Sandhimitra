package org.sandhimitra.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sandhimitra.app.engine.*
import java.io.File

/**
 * Builds the Sandhicakram question bank (build/quiz/questions.json) from the example (ex) and exercise (pr)
 * rows of the supplied documents. A row becomes playable only when the Sandhātrī engine reproduces the
 * documented joined form from the documented parts; every distractor is proven NOT reproducible.
 * Rows that fail are listed in build/quiz/audit.txt with the reason. Copy the output to
 * app/src/main/assets/quiz/questions.json after reviewing the audit.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuizBankBuild {
    private class Cand(val idx: Int, val row: Row, val cat: String, val lesson: String, val practice: String, val reproduced: List<String>, val rules: List<String>,
                        val exSa: String, val exKn: String, val exEn: String, val src: String, val pairKey: String, val score: Int,
                        val valid: Set<String>, val wrongPlain: List<String>, val prakriti: Boolean)
    private data class Row(val topic: String, val group: String, val table: String, val row: Int, val left: String, val right: String, val form: String,
                           val exSa: String, val exKn: String)

    private fun clean(s: String) = s.replace(Regex("\\(.*?\\)"), "").replace("।", "").trim()
    private fun devOnly(s: String) = s.all { it in 'ऀ'..'ॿ' || it == ' ' || it == '\'' || it == '३' || it == '‌' || it == '/' || it == '\u1CF5' || it == '\u1CF6' }

    private fun rows(): List<Row> {
        val out = mutableListOf<Row>()
        val idx = JSONObject(File("src/main/assets/content/index.json").readText())
        val groups = idx.getJSONObject("groups")
        for (g in listOf("vowel", "consonant", "visarga", "prakriti", "intro", "practice")) {
            val arr = groups.getJSONArray(g)
            for (i in 0 until arr.length()) {
                val id = arr.getJSONObject(i).getString("id")
                val o = JSONObject(File("src/main/assets/content/topics/$id.json").readText())
                val sa = o.getJSONObject("sa"); val kn = o.getJSONObject("kn")
                val ex = sa.optJSONArray("ex") ?: JSONArray(); val exk = kn.optJSONArray("ex") ?: JSONArray()
                for (r in 1 until ex.length()) {
                    val row = ex.getJSONArray(r); if (row.length() < 3) continue
                    val l = clean(row.getString(0)); if (!l.contains('+')) continue
                    val parts = l.split('+').map { it.trim() }
                    val form = clean(row.getString(1)).replace(" / ", "/").replace("/ ", "/").replace(" /", "/")
                    if (parts.size != 2 || parts.any { it.isEmpty() || it.contains(',') } || form.isEmpty() || form.contains(',')) continue
                    val kr = if (r < exk.length()) exk.getJSONArray(r) else null
                    out += Row(id, g, "ex", r, parts[0], parts[1], form, row.getString(2).trim(), kr?.optString(2)?.trim() ?: "")
                }
                val pr = sa.optJSONArray("pr") ?: JSONArray(); val prk = kn.optJSONArray("pr") ?: JSONArray()
                for (r in 1 until pr.length()) {
                    val row = pr.getJSONArray(r); if (row.length() < 3) continue
                    val q = row.getString(1); val a = row.getString(2)
                    val qc = clean(q); val kr = if (r < prk.length()) prk.getJSONArray(r) else null
                    if (qc.contains('+')) {
                        val parts = qc.split('+').map { it.trim() }
                        val form = a.substringAfter("रूपम्:", a).substringBefore("|").substringBefore("(").substringBefore("—").trim()
                        if (parts.size == 2 && parts.none { it.isEmpty() || it.contains(',') } && form.isNotEmpty() && !form.contains(','))
                            out += Row(id, g, "pr", r, parts[0], parts[1], form, a.substringAfter("|", a).trim(), kr?.optString(2)?.substringAfter("|")?.trim() ?: "")
                    } else {
                        val ans = a.substringBefore("(").trim()
                        if (ans.contains('+')) {
                            val parts = ans.split('+').map { it.trim() }
                            if (parts.size == 2 && parts.none { it.isEmpty() || it.contains(',') } && qc.isNotEmpty() && !qc.contains(','))
                                out += Row(id, g, "pr", r, parts[0], parts[1], qc, a.substringAfter("(", "").substringBefore(")").trim(), kr?.optString(2)?.substringAfter("(", "")?.substringBefore(")")?.trim() ?: "")
                        }
                    }
                }
            }
        }
        return out
    }

    private fun ns(s: String) = s.replace(" ", "")
    private fun dev(slp: String) = Script.slp1ToDevanagari(slp)
    private val V = "aAiIuUfFxXeEoO"
    private fun adjVowels(s: String) = Regex("[$V][$V]").containsMatchIn(s)

    private val VOW_FIN = "aAiIuUfeEoO"
    private val VARGAS = listOf("kgN", "cjY", "wqR", "tdn", "pbm")
    private val EXTRA = mapOf('t' to "cwk", 'T' to "CW", 'd' to "jqg", 'D' to "JQ", 'n' to "YR", 's' to "Sz", 'S' to "sz", 'z' to "sS", 'H' to "rsSz", 'r' to "Hs", 'M' to "mnNYR", 'm' to "Mn",
        'k' to "ct", 'g' to "jd", 'c' to "kt", 'j' to "gd", 'w' to "t", 'q' to "d")
    /** Sounds a learner confuses with c at a consonant junction: its own varga, the palatal/retroflex counterpart of a dental, the sibilants, visarga/r, anusvāra/nasals. */
    private fun rel(c: Char): String = ((VARGAS.firstOrNull { c in it } ?: "").filter { it != c } + (EXTRA[c] ?: "")).filter { it != c }
    /** y/v directly before a consonant, or a semivowel/visarga cluster no Sanskrit word has, marks a machine-made form. */
    private val JUNK = Regex("[yv][kKgGNcCjJYwWqQRtTdDnpPbBmszSh]|HH|MM|Hr|rH|[rl][rl]|[HM]'|^[HM]" +
        "|[sz][gGdDbBjJcCY]|S[kKgGpPbBwWqQR]|z[tTdD]|sh" +
        "|[KGCJWQTDPB][kKgGcCjJwWqQtTdDpPbBnmszSh]" +          // an aspirate is never followed by another stop, a nasal or a sibilant
        "|N(?![kKgGN]|$)|(?<!j)Y(?![cCjJY]|$)")                 // ṅ and ñ only before their own varga
    private val CATS = listOf("vowel", "consonant", "visarga", "prakriti")
    /** Rules that name a Sandhi a learner can be asked about (composite labels, the generic vārttika entry, an optionality label and the generic "visarga" are not). */
    private val NOT_ASKABLE = setOf("V04", "V10", "C14", "H02", "H04")

    private fun catOf(c: Category?) = when (c) { Category.VOWEL -> "vowel"; Category.CONSONANT -> "consonant"; Category.VISARGA -> "visarga"; Category.PRAKRITIBHAVA -> "prakriti"; null -> null }
    /** Comparison key: spaces, candrabindu/anusvāra and the avagraha sign do not make two spellings different words. */
    private fun normForm(s: String) = ns(s).replace('ँ', 'ं').replace("ऽ", "").replace("'", "")
    private fun badOrth(slp: String) = Regex("[HM][$V]").containsMatchIn(ns(slp)) || ns(slp).startsWith("H") || ns(slp).startsWith("M")

    /** Two spellings of the same analysis (देव/देवा + इन्द्रः, तत्/तद् + च, हरिः/हरिस् + शेते) share one key. */
    private fun pairKey(L: String, R: String): String {
        fun shortEnd(w: String) = if (w.isNotEmpty() && w.last() in "AIUF") w.dropLast(1) + "aiuf"["AIUF".indexOf(w.last())] else w
        fun shortStart(w: String) = if (w.isNotEmpty() && w.first() in "AIUF") "aiuf"["AIUF".indexOf(w.first())] + w.drop(1) else w
        fun consNorm(w: String) = when { w.isEmpty() -> w; w.last() in "rsH" -> w.dropLast(1) + "%"; Ph.varga(w.last()) >= 0 && w.last() !in "NYRnm" -> w.dropLast(1) + "#"; else -> w }
        return consNorm(shortEnd(ns(L))) + "|" + shortStart(ns(R))
    }

    private fun lev(a: String, b: String): Int {
        val d = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) d[i][0] = i; for (j in 0..b.length) d[0][j] = j
        for (i in 1..a.length) for (j in 1..b.length) d[i][j] = minOf(d[i - 1][j] + 1, d[i][j - 1] + 1, d[i - 1][j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        return d[a.length][b.length]
    }

    private fun lastVariants(L: String): List<String> {
        val last = L.last(); val base = L.dropLast(1)
        val pool = if (last in V) (VOW_FIN + "Hm") else rel(last)
        return pool.filter { it != last }.map { base + it }
    }
    private fun firstVariants(R: String): List<String> {
        val first = R.first(); val rest = R.drop(1)
        val pool = if (first in V) VOW_FIN else rel(first)
        return pool.filter { it != first }.map { it + rest }
    }
    /** The mistakes learners make at a vowel junction: the results of the other vowel rules, as far as they are defined for these vowels. */
    private fun vowelJunctionMistakes(L: String, R: String): List<String> {
        val fv = L.last(); val iv = R.first(); val base = L.dropLast(1); val rest = R.drop(1)
        val x = LinkedHashSet<String>()
        x += Ph.longOf(fv).toString()                                              // savarṇadīrgha
        when (fv) {
            'a', 'A' -> { x += listOf("e", "o", "ar", "E", "O", "Ar", "al"); x += iv.toString() }   // guṇa, vṛddhi, pararūpa
            'i', 'I' -> { x += "y$iv"; x += listOf("e", "E", "ay$iv") }
            'u', 'U' -> { x += "v$iv"; x += listOf("o", "O", "av$iv") }
            'f', 'F' -> { x += "r$iv"; x += listOf("ar", "Ar") }
            'x', 'X' -> { x += "l$iv"; x += listOf("al", "Al") }
            'e' -> { x += listOf("ay$iv", "a$iv", "E", iv.toString()); if (iv == 'a') x += "e'" }
            'E' -> { x += listOf("Ay$iv", "A$iv", "e", iv.toString()) }
            'o' -> { x += listOf("av$iv", "a$iv", "O", iv.toString()); if (iv == 'a') x += "o'" }
            'O' -> { x += listOf("Av$iv", "A$iv", "o", iv.toString()) }
        }
        return x.map { base + it + rest }
    }

    @Test fun build() {
        // a build tool, not a regression test: runs only when asked for (QUIZ_BUILD=1 ./gradlew :app:testDebugUnitTest --tests '*QuizBankBuild*')
        if (System.getenv("QUIZ_BUILD") != "1") { println("QUIZBANK skipped (set QUIZ_BUILD=1)"); return }
        SandhiEngineTest.setup(); val e = SandhiEngineTest.engine
        val audit = StringBuilder()
        val excluded = mutableListOf<String>(); val partial = mutableListOf<String>(); val extraSkipped = mutableListOf<String>(); val reclass = mutableListOf<String>(); val dupes = mutableListOf<String>()
        val all = rows()
        fun outs(L: String, R: String, ctx: Ctx): List<String> = try { e.join(dev(L), dev(R), ctx).alts.map { ns(it.surface) } } catch (x: Exception) { emptyList() }
        fun outsAll(L: String, R: String): Set<String> = (outs(L, R, Ctx()) + outs(L, R, Ctx(leftPada = false)) + outs(L, R, Ctx(leftPragrhya = true))).toSet()
        /** a real word: a closed-class word, a form attested in the lessons, or a dictionary noun/adjective/indeclinable of some weight (never a verbal root, a compound-only stem or a fragment) */
        fun meaningful(slp: String): Boolean {
            if (slp.length < 2 || badOrth(slp) || Script.invalidChars(slp).isNotEmpty() || adjVowels(slp) || slp.none { it in V } || JUNK.containsMatchIn(slp)) return false
            val d = dev(slp); val info = e.lookup(d)
            if (info.level != Support.EXACT && info.level != Support.DERIVED) return false
            if (info.detail in setOf("upasarga", "avyaya", "numeral", "cpd", "attested")) return true
            val stem = if (info.level == Support.DERIVED) info.detail else slp
            if (SandhiEngineTest.lex.category(stem) in setOf("r", "c", "x")) return false
            return e.wordWeight(d) >= 3 && stem.length >= 3
        }
        // ---- 1. every row the engine reproduces becomes a candidate; documented forms are the only accepted answers
        val cands = mutableListOf<Cand>()
        val docPairs = HashMap<String, MutableList<Pair<String, String>>>()      // normalised form -> documented (L, R) pairs the engine reproduces
        for ((idx, r) in all.withIndex()) {
            val src = "${r.topic} ${r.table} row ${r.row}"
            if (!devOnly(r.left) || !devOnly(r.right) || !devOnly(r.form)) { excluded += "$src: not a plain example (notes, arrows or Latin text in the row)"; continue }
            val docForms = r.form.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            val L = Script.toSlp1(r.left).trim(); val R = Script.toSlp1(r.right).trim()
            if (Script.invalidChars(L + R).isNotEmpty()) { excluded += "$src: unsupported characters"; continue }
            val docSlp = docForms.map { ns(Script.toSlp1(it)) }
            val prakGroup = r.group == "prakriti"
            var jr = try { e.join(r.left, r.right) } catch (x: Exception) { null }
            var wrongPlain = emptyList<String>()
            if (prakGroup && jr != null) {
                // pragṛhya blocks the ordinary rules: what the ordinary rules would give (कवीमौ) is a wrong answer here
                wrongPlain = jr.alts.map { ns(it.surface) }.filter { it !in docSlp }
                if (jr.alts.none { a -> ns(a.surface) in docSlp }) jr = try { e.join(r.left, r.right, Ctx(leftPragrhya = true)) } catch (x: Exception) { null }
            }
            if (jr == null) { excluded += "$src: engine cannot join ${r.left} + ${r.right}"; continue }
            val reproduced = docForms.filter { d -> Script.invalidChars(Script.toSlp1(d)).isEmpty() && jr.alts.any { ns(it.surface) == ns(Script.toSlp1(d)) } }
            if (reproduced.isEmpty()) { excluded += "$src: engine does not reproduce ${docForms.joinToString("/")} from ${r.left} + ${r.right} (got ${jr.alts.joinToString(" | ") { dev(it.surface) }})"; continue }
            val missing = docForms - reproduced.toSet()
            if (missing.isNotEmpty()) partial += "$src: documented alternative ${missing.joinToString("/")} is outside the engine (not offered, never a distractor); offered: ${reproduced.joinToString("/")}"
            val primary = jr.alts.first { ns(it.surface) == ns(Script.toSlp1(reproduced[0])) }
            val ruleIds = primary.steps.map { it.ruleId }.distinct()
            val ruleCats = ruleIds.mapNotNull { catOf(Rules.find(it)?.category) }.toSet()
            // ---- category and lesson come from the rules that actually apply
            val groupCat = if (r.group in CATS) r.group else null
            var cat: String; var lesson: String; var practice: String
            if (groupCat != null && (ruleCats.isEmpty() || groupCat in ruleCats)) { cat = groupCat; lesson = r.topic; practice = r.topic }
            else if (ruleCats.isNotEmpty()) {
                val first = Rules.find(ruleIds.first())!!
                cat = catOf(first.category)!!; lesson = first.topic; practice = if (r.group == "practice") r.topic else first.topic
                if (groupCat != null) reclass += "$src ($groupCat lesson): the rules that apply are ${ruleIds.joinToString(",")} ($cat) — filed under $cat, lesson ${first.topic}"
            } else { excluded += "$src: no Sandhi rule operates (cannot be assigned to a category)"; continue }
            if (r.group in listOf("intro", "practice")) { lesson = Rules.find(ruleIds.firstOrNull())?.topic ?: r.topic; practice = if (r.group == "practice") r.topic else lesson }
            val prak = cat == "prakriti"
            val regs = ruleIds.mapNotNull { Rules.find(it) }
            val exSa = (if (r.exSa.isNotBlank()) r.exSa + " — " else "") + regs.joinToString("; ") { "${it.sutraDeva} (${it.ref})" }
            val exKn = (if (r.exKn.isNotBlank()) r.exKn + " — " else "") + regs.joinToString("; ") { "${Script.render(it.sutraDeva, "kn")} (${it.ref})" }
            val exEn = regs.joinToString("; ") { "${Script.render(it.sutraDeva, "en")} (${it.ref}): ${it.scope}" }.ifEmpty { if (prak) "No Sandhi change (prakṛtibhāva)." else "No Sandhi rule operates here: the two words stay as they are." }
            // every form the engine can derive from this pair is "valid": it is never offered as a wrong option
            val valid = if (prak) outs(L, R, Ctx(leftPragrhya = true)).toSet() + docSlp else outsAll(L, R) + docSlp
            val wrong = if (prak) wrongPlain.filter { it !in valid } else emptyList()
            val score = (if (groupCat != null && groupCat in ruleCats) 8 else 0) + (if (r.table == "ex") 4 else 0) + (if (groupCat != null) 2 else 0)
            cands += Cand(idx, r, cat, lesson, practice, reproduced, ruleIds, exSa, exKn, exEn, src, pairKey(L, R), score, valid, wrong, prak)
            for (fm in reproduced) docPairs.getOrPut(normForm(fm)) { mutableListOf() }.add(r.left to r.right)
        }
        // ---- 2. one row per word: same analysis (pair key) or a shared documented form counts as the same word; the row from the lesson that teaches it is kept
        val kept = mutableListOf<Cand>(); val seenKeys = HashSet<String>(); val seenForms = HashSet<String>()
        for (c in cands.sortedWith(compareByDescending<Cand> { it.score }.thenBy { it.idx })) {
            val forms = c.reproduced.map { normForm(it) }
            if (c.pairKey in seenKeys || forms.any { it in seenForms }) { dupes += "${c.src}: ${c.row.left} + ${c.row.right} → ${c.reproduced[0]} (same word as a row already kept)"; continue }
            seenKeys += c.pairKey; seenForms += forms; kept += c
        }
        kept.sortBy { it.idx }
        // ---- 3. questions
        val qs = JSONArray(); val counts = HashMap<String, IntArray>()
        fun bump(cat: String, k: Int) { counts.getOrPut(cat) { IntArray(5) }[k]++ }
        for (c in kept) {
            val r = c.row; val L = Script.toSlp1(r.left).trim(); val R = Script.toSlp1(r.right).trim(); val F = Script.toSlp1(c.reproduced[0]).trim()
            val rnd0 = c.src.hashCode().toLong()
            // forward: join the pair
            run {
                val acc = c.reproduced.take(2).toList(); val accN = c.reproduced.map { normForm(it) }.toSet()
                val cand = ArrayList<Pair<String, Int>>()
                fun consider(slp: String, bias: Int, plain: Boolean = false) {
                    val n = ns(slp); val raw = slp.trim().replace(Regex(" +"), " ")
                    if (n.isEmpty() || n in c.valid || normForm(dev(n)) in accN || badOrth(raw) || adjVowels(raw) || Script.invalidChars(n).isNotEmpty() || n.startsWith("'") || (!plain && JUNK.containsMatchIn(n))) return
                    cand += dev(raw) to (lev(n, ns(F)) + bias)
                }
                val f = ns(F)
                for (w in c.wrongPlain) consider(w, -6)                                  // prakṛtibhāva: what the ordinary rules would wrongly give
                if (L.last() in V && R.first() in V) for (m in vowelJunctionMistakes(L, R)) consider(m, 0)   // wrong rule at a vowel junction
                if (!(L.last() in V && R.first() in V)) {
                    consider(L + R, 0, plain = true)                                     // no Sandhi applied
                    val lc = L.last(); val base = L.dropLast(1)
                    if (lc == 'H') {
                        // visarga: the classic mistakes
                        consider(base + R, 2)                                            // visarga simply dropped
                        if (base.isNotEmpty() && base.last() in "iu") consider(base.dropLast(1) + Ph.longOf(base.last()) + R, 2)   // r lost with lengthening where it does not apply
                        if (base.endsWith("a")) { consider(base.dropLast(1) + "o" + R, 1); consider(base.dropLast(1) + "o " + R, 1) }   // ः → o where it is not the rule (स/सः, before a vowel)
                        if (base.endsWith("A")) { consider(base.dropLast(1) + "o" + R, 2); consider(base.dropLast(1) + "o " + R, 2) }
                        for (f2 in (if (R.first() in V) "rs" else "rsSz")) consider(base + f2 + R, 2)
                        if (R.first() in V && base.isNotEmpty() && base.last() in V) for (m in vowelJunctionMistakes(base, R)) consider(m, 3)   // a vowel Sandhi wrongly applied across the lost visarga
                        if (R.first() in V && base.last() !in "aA") consider(base + "y" + R, 3)                                                   // the य् of 8.3.17 where it does not apply
                    }
                    if (lc == 's' || lc == 'r') consider(base + "H" + R, 1, plain = true)    // the visarga kept
                    if (lc !in V && lc != 'H') {
                        val wrongFinals = rel(lc)
                        for (f2 in wrongFinals) consider(base + f2 + R, 1)               // the wrong final sound, nothing else changed
                        consider(base + R, 2)                                            // the final sound dropped
                        // the second word's first sound changes only where phonetics allows it: h takes the aspirate of the preceding stop, ś/s/ṣ and ch
                        val inits = when (R.first()) { 'h' -> "GJQDB"; 'S' -> "Cs"; 's' -> "Sz"; else -> "" }
                        for (f2 in wrongFinals + lc) for (i2 in inits) consider(base + f2 + i2 + R.drop(1), 2)   // a wrong assimilation
                        for (L2 in lastVariants(L)) for (o in outs(L2, R, Ctx())) consider(o, 3)                 // correct Sandhi of a mistaken stem
                    }
                    if (lc in V) for (ins in "tckdjgnm") consider(L + ins + R, 2)       // a consonant wrongly inserted
                }
                val dis = cand.sortedBy { it.second }.map { it.first }.distinctBy { normForm(it) }.take(4 - acc.size)
                if (dis.size < 4 - acc.size) excluded += "${c.src} (F): fewer than three sound wrong forms (${dis.size} found: ${dis.joinToString(", ")}) for ${r.left} + ${r.right} → ${c.reproduced.joinToString("/")}"
                else { val opts = (acc + dis).shuffled(java.util.Random(rnd0)); qs.put(q("${r.topic}-${r.table}${r.row}-F", c.cat, "F", c.lesson, c.practice, "${r.left} + ${r.right}", opts, opts.indices.filter { opts[it] in acc }, c.exSa, c.exKn, c.exEn, c.rules, c.src)); bump(c.cat, 0) }
            }
            // reverse: separate the form (all documented, engine-reproduced analyses with a different stem/ending are accepted; citation variants are one analysis)
            run {
                val own = pairKey(L, R)
                val accPairs = LinkedHashMap<String, Pair<String, String>>(); accPairs[own] = r.left to r.right
                for ((l2, r2) in docPairs[normForm(c.reproduced[0])].orEmpty()) { val k = pairKey(Script.toSlp1(l2), Script.toSlp1(r2)); if (k !in accPairs) accPairs[k] = l2 to r2 }
                val acc = accPairs.values.take(2).map { "${it.first} + ${it.second}" }
                val accKeys = accPairs.keys
                val cand = ArrayList<Pair<String, Int>>()
                fun consider(l: String, rr: String, changedLeft: Boolean) {
                    val changed = if (changedLeft) l else rr
                    if (!meaningful(changed)) return
                    if (pairKey(l, rr) in accKeys) return
                    if (ns(F) in outsAll(l, rr)) return
                    cand += "${dev(l)} + ${dev(rr)}" to lev(ns(l + rr), ns(L + R))
                }
                for (l2 in lastVariants(L)) consider(l2, R, true)
                for (r2 in firstVariants(R)) consider(L, r2, false)
                val dis = cand.sortedBy { it.second }.map { it.first }.distinct().take(4 - acc.size)
                if (dis.size < 4 - acc.size) excluded += "${c.src} (R): fewer than three real-word wrong analyses (${dis.size} found)"
                else { val opts = (acc + dis).shuffled(java.util.Random(rnd0 + 1)); qs.put(q("${r.topic}-${r.table}${r.row}-R", c.cat, "R", c.lesson, c.practice, c.reproduced[0], opts, opts.indices.filter { opts[it] in acc }, c.exSa, c.exKn, c.exEn, c.rules, c.src)); bump(c.cat, 1) }
            }
        }
        // ---- additional formats (example rows only): identify the Sandhi (NAME), identify the sūtra (SUTRA), select an example (EXAMPLE)
        val askable = Rules.all.filter { it.id !in NOT_ASKABLE }
        for (a in kept) {
            if (a.row.table != "ex") continue
            val applied = a.rules.mapNotNull { Rules.find(it) }
            val target = applied.filter { it.topic == a.lesson && it.id !in NOT_ASKABLE }.distinctBy { it.name }
            if (target.size != 1) { extraSkipped += "${a.src}: ${if (target.isEmpty()) "no askable rule of lesson ${a.lesson} applies" else "more than one named rule of the lesson applies"}"; continue }
            val t0 = target[0]
            val appliedNames = applied.map { it.name }.toSet(); val appliedTopics = applied.map { it.topic }.toSet(); val appliedSutras = applied.map { it.sutraDeva }.toSet()
            val rnd = java.util.Random(a.src.hashCode().toLong() + 7)
            // every visarga/ru derivation starts from ससजुषो रुः (8.2.66): it is never offered as "not applying" there
            val viaRu = applied.any { it.category == Category.VISARGA } || Script.toSlp1(a.row.left).trim().last() in "Hsr"
            fun pool(same: Boolean) = askable.filter { !(viaRu && it.id in setOf("H01", "H12")) }.filter { it.name !in appliedNames && it.topic !in appliedTopics && it.sutraDeva !in appliedSutras && (it.category == t0.category) == same }.distinctBy { it.name }.distinctBy { it.sutraDeva }.shuffled(rnd)
            val dRules = (pool(true) + pool(false)).take(3)
            val shown = "${a.row.left} + ${a.row.right} → ${a.reproduced[0]}"
            val exRule = "${t0.name} — ${t0.sutraDeva} (${t0.ref})"
            if (dRules.size == 3) {
                val opts = (listOf(t0.name) + dRules.map { it.name }).shuffled(rnd)
                qs.put(q("${a.row.topic}-${a.row.table}${a.row.row}-N", a.cat, "O", a.lesson, a.practice, shown, opts, listOf(opts.indexOf(t0.name)), exRule, Script.render(exRule, "kn"), a.exEn, a.rules, a.src).put("type", "NAME")); bump(a.cat, 2)
                val sopts = (listOf(t0) + dRules).map { "${it.sutraDeva} (${it.ref})" }.shuffled(rnd)
                qs.put(q("${a.row.topic}-${a.row.table}${a.row.row}-S", a.cat, "O", a.lesson, a.practice, shown, sopts, listOf(sopts.indexOf("${t0.sutraDeva} (${t0.ref})")), exRule, Script.render(exRule, "kn"), a.exEn, a.rules, a.src).put("type", "SUTRA")); bump(a.cat, 3)
            } else extraSkipped += "${a.src}: fewer than three other Sandhi names available"
            if (a.row.row % 2 == 1) {
                // forms of other kept rows in which no rule of this lesson's Sandhi operates
                val others = kept.filter { o -> o !== a && o.rules.none { Rules.find(it)?.topic == t0.topic || Rules.find(it)?.name == t0.name } }.shuffled(rnd).take(3)
                if (others.size == 3) {
                    val items = (listOf(a) + others).shuffled(rnd)
                    qs.put(q("${a.row.topic}-${a.row.table}${a.row.row}-E", a.cat, "O", a.lesson, a.practice, t0.name, items.map { it.reproduced[0] }, listOf(items.indexOf(a)), exRule, Script.render(exRule, "kn"), a.exEn, a.rules, a.src)
                        .put("type", "EXAMPLE").put("osrc", JSONArray(items.map { "${it.row.left} + ${it.row.right}" }))); bump(a.cat, 4)
                }
            }
        }
        File("build/quiz").mkdirs()
        val meta = JSONObject().put("built_from", "example (ex) and exercise (pr) rows of the supplied documents; verified by the Sandhātrī engine").put("rows_considered", all.size).put("rows_kept", kept.size)
        counts.forEach { (c, n) -> meta.put("count_$c", JSONObject().put("forward", n[0]).put("reverse", n[1]).put("name", n[2]).put("sutra", n[3]).put("example", n[4]).put("total", n.sum())) }
        File("build/quiz/questions.json").writeText(JSONObject().put("meta", meta).put("questions", qs).toString(1))
        audit.append("rows considered: ${all.size}; one row kept per word: ${kept.size}\n")
        counts.toSortedMap().forEach { (c, n) -> audit.append("$c: forward=${n[0]} reverse=${n[1]} name=${n[2]} sutra=${n[3]} example=${n[4]} total=${n.sum()}\n") }
        audit.append("rows refiled by the rule that applies (${reclass.size}):\n"); reclass.forEach { audit.append("  $it\n") }
        audit.append("same word in several lessons — later copies dropped (${dupes.size}):\n"); dupes.forEach { audit.append("  $it\n") }
        audit.append("additional formats not generated (${extraSkipped.size}):\n"); extraSkipped.forEach { audit.append("  $it\n") }
        audit.append("partially offered (${partial.size}):\n"); partial.forEach { audit.append("  $it\n") }
        audit.append("excluded (${excluded.size}):\n"); excluded.forEach { audit.append("  $it\n") }
        File("build/quiz/audit.txt").writeText(audit.toString())
        println("QUIZBANK " + counts.map { "${it.key}=${it.value.toList()}" })
    }

    private fun q(id: String, cat: String, dir: String, lesson: String, practice: String, prompt: String, opts: List<String>, answers: List<Int>,
                  exSa: String, exKn: String, exEn: String, rules: List<String>, src: String) = JSONObject()
        .put("id", id).put("cat", cat).put("dir", dir).put("lesson", lesson).put("practice", practice).put("prompt", prompt)
        .put("options", JSONArray(opts)).put("answers", JSONArray(answers)).put("ex_sa", exSa).put("ex_kn", exKn).put("ex_en", exEn).put("rules", JSONArray(rules)).put("src", src).put("type", dir)
}
