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

    private val SUB = mapOf(
        'a' to "AeoiuEO", 'A' to "aeEoOiIuU", 'i' to "IeyaAE", 'I' to "ieyaAE", 'u' to "UovaAoO", 'U' to "uovaAO", 'f' to "FaraA", 'e' to "EoaAiI", 'E' to "eoAai", 'o' to "OeaAuU", 'O' to "oeAau",
        'y' to "iIv", 'v' to "uUy", 'r' to "Hso", 'H' to "rsoM", 'M' to "nmNYR", 'n' to "MRYN", 'm' to "Mn", 'N' to "Mn", 'Y' to "Mn", 'R' to "nM",
        't' to "dcwn", 'd' to "tjqn", 'c' to "tjS", 'j' to "dc", 'w' to "tq", 'q' to "dw", 'S' to "szC", 'z' to "sS", 's' to "SzH", 'C' to "cS", 'k' to "g", 'g' to "k", 'p' to "b", 'b' to "p", 'l' to "r", 'h' to "G"
    )

    @Test fun build() {
        // a build tool, not a regression test: runs only when asked for (QUIZ_BUILD=1 ./gradlew :app:testDebugUnitTest --tests '*QuizBankBuild*')
        if (System.getenv("QUIZ_BUILD") != "1") { println("QUIZBANK skipped (set QUIZ_BUILD=1)"); return }
        SandhiEngineTest.setup(); val e = SandhiEngineTest.engine
        val qs = JSONArray(); val audit = StringBuilder(); val counts = HashMap<String, IntArray>()
        val excluded = mutableListOf<String>(); val partial = mutableListOf<String>()
        val seenForms = HashMap<String, MutableSet<String>>()   // form -> accepted "L+R" (for reverse alternatives from other rows)
        val all = rows()
        // reverse alternatives come only from rows whose analysis the engine reproduces (a documented pair the engine rejects is never an answer)
        for (r in all) if (devOnly(r.left) && devOnly(r.right) && devOnly(r.form)) {
            val forms = r.form.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            val got = try { e.join(r.left, r.right).alts.map { ns(it.surface) } + (if (r.group == "prakriti") e.join(r.left, r.right, Ctx(leftPragrhya = true)).alts.map { ns(it.surface) } else emptyList()) } catch (x: Exception) { emptyList() }
            for (fm in forms) if (Script.invalidChars(Script.toSlp1(fm)).isEmpty() && ns(Script.toSlp1(fm)) in got) seenForms.getOrPut(ns(fm)) { LinkedHashSet() }.add("${r.left}+${r.right}")
        }
        for (r in all) {
            val src = "${r.topic} ${r.table} row ${r.row}"
            if (!devOnly(r.left) || !devOnly(r.right) || !devOnly(r.form)) { excluded += "$src: non-Devanagari characters"; continue }
            val docForms = r.form.split('/').map { it.trim() }.filter { it.isNotEmpty() }
            val L = Script.toSlp1(r.left).trim(); val R = Script.toSlp1(r.right).trim()
            if (Script.invalidChars(L + R).isNotEmpty()) { excluded += "$src: unsupported characters"; continue }
            // the lessons on prakṛtibhāva present pragṛhya words; the engine is told so (as the user does in Sandhātrī)
            var jr = try { e.join(r.left, r.right) } catch (x: Exception) { null }
            var plainSurfaces = emptyList<String>()
            if (r.group == "prakriti" && jr != null && jr.alts.none { a -> docForms.any { d -> ns(a.surface) == ns(Script.toSlp1(d)) } }) {
                plainSurfaces = jr.alts.map { it.surface }
                jr = try { e.join(r.left, r.right, Ctx(leftPragrhya = true)) } catch (x: Exception) { null }
            }
            val surfaces = jr?.alts?.map { it.surface }.orEmpty()
            val reproduced = docForms.filter { d -> Script.invalidChars(Script.toSlp1(d)).isEmpty() && surfaces.any { ns(it) == ns(Script.toSlp1(d)) } }
            val missing = docForms - reproduced.toSet()
            if (jr == null || reproduced.isEmpty()) { excluded += "$src: engine does not reproduce ${missing.joinToString("/")} from ${r.left} + ${r.right} (got ${surfaces.joinToString(" | ") { dev(it) }})"; continue }
            if (missing.isNotEmpty()) partial += "$src: documented alternative ${missing.joinToString("/")} is outside the engine (not offered, never a distractor); offered: ${reproduced.joinToString("/")}"
            val F = Script.toSlp1(reproduced[0]).trim()
            val primary = jr.alts.first { ns(it.surface) == ns(F) }
            val ruleIds = primary.steps.map { it.ruleId }.distinct()
            val cat = when (r.group) {
                "vowel", "consonant", "visarga", "prakriti" -> r.group
                else -> when (ruleIds.firstOrNull()?.let { Rules.find(it)?.category }) {
                    Category.VOWEL -> "vowel"; Category.CONSONANT -> "consonant"; Category.VISARGA -> "visarga"; Category.PRAKRITIBHAVA -> "prakriti"; null -> null
                }
            }
            if (cat == null) { excluded += "$src: no Sandhi rule operates (cannot be assigned to a category)"; continue }
            val ruleTopic = ruleIds.firstOrNull()?.let { Rules.find(it)?.topic }
            val lesson = if (r.group in listOf("intro", "practice")) (ruleTopic ?: r.topic) else r.topic
            val practice = if (r.group == "practice") r.topic else lesson
            // explanations
            val regNames = ruleIds.mapNotNull { Rules.find(it) }
            val exSa = (if (r.exSa.isNotBlank()) r.exSa + " — " else "") + regNames.joinToString("; ") { "${it.sutraDeva} (${it.ref})" }
            val exKn = (if (r.exKn.isNotBlank()) r.exKn + " — " else "") + regNames.joinToString("; ") { "${Script.render(it.sutraDeva, "kn")} (${it.ref})" }
            val exEn = regNames.joinToString("; ") { "${Script.render(it.sutraDeva, "en")} (${it.ref}): ${it.scope}" }.ifEmpty { if (r.group == "prakriti") "No Sandhi change (prakṛtibhāva)." else "No Sandhi rule operates here: the two words stay as they are." }
            // ---- forward question
            val acceptedF = LinkedHashSet<String>(); acceptedF += reproduced
            if (jr.status == Status.VERIFIED) jr.alts.forEach { a -> if (docForms.none { ns(Script.toSlp1(it)) == ns(a.surface) }) acceptedF += dev(a.surface) }
            val fDis = forwardDistractors(L, R, F, surfaces, (acceptedF + docForms).map { ns(Script.toSlp1(it)) }.toSet(), plainSurfaces)
            if (fDis.size < 3) excluded += "$src (F): only ${fDis.size} safe distractors"
            else { val opts = (acceptedF.take(2) + fDis.take(4 - minOf(2, acceptedF.size))).shuffled(java.util.Random(src.hashCode().toLong()))
                qs.put(q("${r.topic}-${r.table}${r.row}-F", cat, "F", lesson, practice, "${r.left} + ${r.right}", opts, opts.indices.filter { opts[it] in acceptedF }, exSa, exKn, exEn, ruleIds, src))
                counts.getOrPut(cat) { IntArray(2) }[0]++ }
            // ---- reverse question
            val acceptedR = seenForms.getValue(ns(reproduced[0])).map { it.replace("+", " + ") }
            val rDis = reverseDistractors(e, L, R, F, acceptedR.map { ns(Script.toSlp1(it.replace(" + ", "+"))) }.toSet(), r.group == "prakriti")
            if (rDis.size < 3) excluded += "$src (R): only ${rDis.size} safe distractors"
            else { val opts = (acceptedR.take(2) + rDis.take(4 - minOf(2, acceptedR.size))).shuffled(java.util.Random(src.hashCode().toLong() + 1))
                qs.put(q("${r.topic}-${r.table}${r.row}-R", cat, "R", lesson, practice, reproduced[0], opts, opts.indices.filter { opts[it] in acceptedR }, exSa, exKn, exEn, ruleIds, src))
                counts.getOrPut(cat) { IntArray(2) }[1]++ }
        }
        File("build/quiz").mkdirs()
        val meta = JSONObject().put("built_from", "example (ex) and exercise (pr) rows of the supplied documents; verified by the Sandhātrī engine").put("rows_considered", all.size)
        counts.forEach { (c, n) -> meta.put("count_$c", JSONObject().put("forward", n[0]).put("reverse", n[1]).put("total", n[0] + n[1])) }
        File("build/quiz/questions.json").writeText(JSONObject().put("meta", meta).put("questions", qs).toString(1))
        audit.append("rows considered: ${all.size}\n")
        counts.toSortedMap().forEach { (c, n) -> audit.append("$c: forward=${n[0]} reverse=${n[1]} total=${n[0] + n[1]}\n") }
        audit.append("partially offered (${partial.size}):\n"); partial.forEach { audit.append("  $it\n") }
        audit.append("excluded (${excluded.size}):\n"); excluded.forEach { audit.append("  $it\n") }
        File("build/quiz/audit.txt").writeText(audit.toString())
        println("QUIZBANK " + counts.map { "${it.key}=${it.value[0]}+${it.value[1]}" })
    }

    private fun q(id: String, cat: String, dir: String, lesson: String, practice: String, prompt: String, opts: List<String>, answers: List<Int>,
                  exSa: String, exKn: String, exEn: String, rules: List<String>, src: String) = JSONObject()
        .put("id", id).put("cat", cat).put("dir", dir).put("lesson", lesson).put("practice", practice).put("prompt", prompt)
        .put("options", JSONArray(opts)).put("answers", JSONArray(answers)).put("ex_sa", exSa).put("ex_kn", exKn).put("ex_en", exEn).put("rules", JSONArray(rules)).put("src", src)

    /** Wrong joined forms: the plain concatenation and single-sound mutations at the junction, none reproducible by the engine. */
    private fun forwardDistractors(L: String, R: String, F: String, surfaces: List<String>, accepted: Set<String>, plainSurfaces: List<String>): List<String> {
        val bad = surfaces.map { ns(it) }.toSet() + accepted
        val out = LinkedHashSet<String>()
        fun ok(c: String) = c.isNotEmpty() && ns(c) !in bad && Script.invalidChars(c).isEmpty() && !adjVowels(ns(c)) && c.first() != '\''
        val f = ns(F)
        // prakṛtibhāva: the forms the ordinary Sandhi rules would give are the natural wrong answers
        for (p in plainSurfaces) if (ok(p)) out += p
        if (ok(L + R)) out += L + R
        if (F.contains(' ')) {
            // words left apart (देवा इह, स गच्छति): wrong forms keep or replace the lost sound at the gap
            if (ok(ns(F))) out += ns(F)
            val g = F.indexOf(' ')
            for (ins in listOf("y", "r", "H", "s", "v", "M")) { val c = F.substring(0, g) + ins + F.substring(g + 1); if (ok(c)) out += c }
            if (g > 0) for (sv in SUB[F[g - 1]] ?: "") { val c = F.substring(0, g - 1) + sv + F.substring(g); if (ok(c)) out += c; val c2 = F.substring(0, g - 1) + sv + F.substring(g + 1); if (ok(c2)) out += c2 }
        }
        val j = (L.length - 1).coerceIn(0, f.length - 1)
        val order = listOf(j, j + 1, j - 1, j + 2, j - 2).filter { it in f.indices }
        for (p in order) for (s in SUB[f[p]] ?: "") { val c = f.substring(0, p) + s + f.substring(p + 1); if (ok(c)) out += c }
        if (f.contains('\'')) { val c = f.replace("'", "a"); if (ok(c)) out += c }
        return out.map { dev(it) }.distinct()
    }

    /** Wrong analyses: mutations of the final sound of the first word, the initial sound of the second, or the boundary; none joins to the form. */
    private fun reverseDistractors(e: SandhiEngine, L: String, R: String, F: String, accepted: Set<String>, pragrhya: Boolean): List<String> {
        val f = ns(F); val out = LinkedHashSet<String>()
        fun ok(l: String, r: String): Boolean {
            if (l.isEmpty() || r.isEmpty() || ns(l + "+" + r) in accepted) return false
            if (l.none { it in V } || r.none { it in V } || adjVowels(l) || adjVowels(r)) return false
            if (Script.invalidChars(l + r).isNotEmpty()) return false
            val alts = try { e.join(dev(l), dev(r)).alts + (if (pragrhya) e.join(dev(l), dev(r), Ctx(leftPragrhya = true)).alts else emptyList()) } catch (x: Exception) { return false }
            return alts.none { ns(it.surface) == f }
        }
        fun add(l: String, r: String) { if (ok(l, r)) out += "${dev(l)} + ${dev(r)}" }
        val lastL = L.last(); val firstR = R.first()
        for (s in SUB[lastL] ?: "") add(L.dropLast(1) + s, R)
        for (s in SUB[firstR] ?: "") add(L, s + R.drop(1))
        if (R.length > 2) add(L + R.first(), R.drop(1))
        if (L.length > 2) add(L.dropLast(1), L.last() + R)
        if (lastL in V && lastL !in "aA") add(L.dropLast(1) + "a", R)
        if (lastL == 'H' || lastL == 'M') add(L.dropLast(1), R)
        if (lastL in V) { add(L + "H", R); add(L + "m", R) }
        if (firstR in V && R.length > 2) add(L, R.drop(1))
        if (firstR !in V) for (v in "aAiu") add(L, v + R)
        return out.toList()
    }
}
