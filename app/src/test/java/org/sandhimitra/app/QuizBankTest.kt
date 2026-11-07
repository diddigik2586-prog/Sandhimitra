package org.sandhimitra.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sandhimitra.app.data.QuizBank
import org.sandhimitra.app.data.QuizQuestion
import org.sandhimitra.app.engine.Category
import org.sandhimitra.app.engine.Ctx
import org.sandhimitra.app.engine.Rules
import org.sandhimitra.app.engine.Script
import org.sandhimitra.app.engine.Support
import java.io.File

/**
 * Re-verifies the shipped question bank (assets/quiz/questions.json) against the engine, the lexicon, the rule registry,
 * the content index and the lesson text itself. Everything the bank builder promises is checked again here, independently.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuizBankTest {
    private fun ns(s: String) = s.replace(" ", "")
    /** comparison key: spaces, '/', candrabindu/anusvāra and the avagraha sign are not differences between words */
    private fun key(s: String) = s.replace(" ", "").replace("/", "").replace('ँ', 'ं').replace("ऽ", "")
    private fun catOf(c: Category) = when (c) { Category.VOWEL -> "vowel"; Category.CONSONANT -> "consonant"; Category.VISARGA -> "visarga"; Category.PRAKRITIBHAVA -> "prakriti" }
    private val notAskable = setOf("V04", "V10", "C14", "H02", "H04")

    private fun load(): List<QuizQuestion> = QuizBank.parse(File("src/main/assets/quiz/questions.json").readText())

    /** the text of the lesson row a question was made from ("t08 ex row 3") */
    private fun sourceText(src: String): String {
        val (topic, table, _, row) = src.split(" ").let { listOf(it[0], it[1], it[2], it[3]) }
        val o = JSONObject(File("src/main/assets/content/topics/$topic.json").readText()).getJSONObject("sa")
        val rows = o.getJSONArray(table); val r = rows.getJSONArray(row.toInt())
        return key((0 until r.length()).joinToString(" ") { r.getString(it) })
    }

    @Test fun noQuestionIsRepeated() {
        val qs = load()
        assertEquals("duplicate ids", qs.size, qs.map { it.id }.toSet().size)
        // one question per word and format
        val byWord = qs.groupBy { if (it.type == "EXAMPLE") "E|${key(it.prompt)}|${key(it.word)}" else "${it.type}|${key(it.word)}" }.filter { it.value.size > 1 }
        assertTrue("same word asked twice in the same format: " + byWord.entries.take(5).map { it.key to it.value.map { q -> q.id } }, byWord.isEmpty())
        // the two spellings of one analysis (हरिः/हरिस् + शेते, तत्/तद् + च, देव/देवा + इन्द्रः) are one word as well
        val pair = qs.filter { it.type == "F" }.groupBy { q ->
            val (l, r) = q.prompt.split(" + ").let { Script.toSlp1(it[0]).trim() to Script.toSlp1(it[1]).trim() }
            val lk = l.dropLast(1) + when { l.last() in "rsH" -> "%"; l.last() in "kKgGcCjJwWqQtTdDpPbB" -> "#"; l.last() in "AIUF" -> "aiuf"["AIUF".indexOf(l.last())].toString(); else -> l.last().toString() }
            lk + "|" + (if (r.first() in "AIUF") "aiuf"["AIUF".indexOf(r.first())] + r.drop(1) else r)
        }.filter { it.value.size > 1 }
        assertTrue("same analysis in two forward questions: " + pair.values.take(5).map { v -> v.map { it.id } }, pair.isEmpty())
        // a wrong option is never a look-alike of another option (the documented candrabindu/anusvāra pair may both be accepted)
        qs.forEach { q ->
            assertEquals("${q.id} repeated option", 4, q.options.map { ns(it) }.toSet().size)
            val wrong = q.options.filterIndexed { i, _ -> i !in q.answers }.map { key(it) }
            val accepted = q.answers.map { key(q.options[it]) }
            assertEquals("${q.id} look-alike wrong options", wrong.size, wrong.toSet().size)
            assertTrue("${q.id} a wrong option looks like an accepted one", wrong.none { it in accepted })
        }
    }

    @Test fun categoryAndLessonFollowTheRulesThatApply() {
        val qs = load()
        val idx = JSONObject(File("src/main/assets/content/index.json").readText()).getJSONObject("groups")
        val topicIds = idx.keys().asSequence().flatMap { g -> val a = idx.getJSONArray(g); (0 until a.length()).map { a.getJSONObject(it).getString("id") }.asSequence() }.toSet()
        for (q in qs) {
            assertTrue("${q.id} lesson ${q.lesson}", q.lesson in topicIds); assertTrue("${q.id} practice ${q.practice}", q.practice in topicIds)
            val applied = q.rules.mapNotNull { Rules.find(it) }
            assertEquals("${q.id} unknown rule id", q.rules.size, applied.size)
            if (applied.isNotEmpty()) assertTrue("${q.id} is filed under ${q.cat} but the rules ${q.rules} are ${applied.map { catOf(it.category) }}", q.cat in applied.map { catOf(it.category) })
            // prakṛtibhāva holds only pragṛhya questions; a vowel/consonant/visarga rule in a prakṛtibhāva question (e.g. ayādi) is a misfiling
            if (q.cat == "prakriti") assertTrue("${q.id} (prakṛtibhāva) applies ${q.rules}", q.rules.all { it.startsWith("P") })
            else assertTrue("${q.id} (${q.cat}) applies a prakṛtibhāva rule", q.rules.none { it.startsWith("P") })
            if (q.type == "NAME" || q.type == "SUTRA" || q.type == "EXAMPLE") {
                val target = applied.filter { it.topic == q.lesson && it.id !in notAskable }.distinctBy { it.name }
                assertEquals("${q.id} exactly one askable rule of the lesson ${q.lesson}", 1, target.size)
            }
        }
    }

    @Test fun acceptedAnswersAreTheDocumentsOwnForms() {
        for (q in load()) {
            val text = sourceText(q.source)
            when (q.type) {
                "F" -> q.answers.forEach { assertTrue("${q.id}: accepted ${q.options[it]} is not in the lesson row (${q.source})", key(q.options[it]) in text) }
                "R" -> { assertTrue("${q.id}: ${q.prompt} is not in the lesson row (${q.source})", key(q.prompt) in text)
                         q.answers.forEach { val (l, r) = q.options[it].split(" + "); assertTrue("${q.id}: documented pair ${q.options[it]} not found anywhere in the lessons", allLessonText.contains(key(l)) && allLessonText.contains(key(r))) } }
                "NAME", "SUTRA" -> assertTrue("${q.id}: example not in the lesson row", key(q.prompt.substringAfterLast("→")) in text)
                "EXAMPLE" -> assertTrue("${q.id}: example not in the lesson row", key(q.options[q.answers[0]]) in text)
            }
        }
    }
    private val allLessonText: String by lazy { File("src/main/assets/content/topics").listFiles()!!.joinToString(" ") { key(it.readText()) } }

    @Test fun everyShippedQuestionIsVerified() {
        SandhiEngineTest.setup(); val e = SandhiEngineTest.engine
        val qs = load()
        val counts = HashMap<String, IntArray>()
        fun joins(l: String, r: String, pragrhya: Boolean): Set<String> = e.join(l, r, if (pragrhya) Ctx(leftPragrhya = true) else Ctx()).alts.map { ns(it.surface) }.toSet()
        fun allJoins(l: String, r: String): Set<String> = joins(l, r, false) + joins(l, r, true) + e.join(l, r, Ctx(leftPada = false)).alts.map { ns(it.surface) }
        /** a real word: closed class / attested / dictionary noun or indeclinable — never a root, a compound-only stem or a fragment */
        fun meaningful(slp: String): Boolean {
            val d = Script.slp1ToDevanagari(slp); val info = e.lookup(d)
            if (info.level != Support.EXACT && info.level != Support.DERIVED) return false
            if (info.detail in setOf("upasarga", "avyaya", "numeral", "cpd", "attested")) return true
            val stem = if (info.level == Support.DERIVED) info.detail else slp
            return SandhiEngineTest.lex.category(stem) !in setOf("r", "c", "x") && e.wordWeight(d) >= 3
        }
        for (q in qs) {
            assertEquals(q.id, 4, q.options.size)
            assertTrue(q.id, q.answers.isNotEmpty() && q.answers.all { it in 0..3 })
            assertTrue(q.id, q.cat in listOf("vowel", "consonant", "visarga", "prakriti"))
            if (q.dir == "O") {
                val applied = q.rules.mapNotNull { Rules.find(it) }
                val target = applied.filter { it.topic == q.lesson && it.id !in notAskable }.distinctBy { it.name }[0]
                when (q.type) {
                    "NAME" -> q.options.forEachIndexed { i, o ->
                        assertEquals("${q.id} option $o", i in q.answers, o == target.name)
                        if (i !in q.answers) { assertTrue("${q.id} distractor $o is an applied rule", applied.none { it.name == o || it.sutraDeva == Rules.all.first { r -> r.name == o }.sutraDeva }); assertTrue("${q.id} distractor $o is not a Sandhi name", Rules.all.first { r -> r.name == o }.id !in notAskable) } }
                    "SUTRA" -> q.options.forEachIndexed { i, o ->
                        assertEquals("${q.id} option $o", i in q.answers, o == "${target.sutraDeva} (${target.ref})")
                        if (i !in q.answers) assertTrue("${q.id} distractor $o", applied.none { o.startsWith(it.sutraDeva) }); assertTrue(target.verified) }
                    "EXAMPLE" -> {
                        assertEquals(q.id, 4, q.optionSources.size)
                        q.options.forEachIndexed { i, o ->
                            val (l, r) = q.optionSources[i].split(" + ").let { it[0] to it[1] }
                            val alt = (e.join(l, r).alts + e.join(l, r, Ctx(leftPragrhya = true)).alts).firstOrNull { ns(it.surface) == ns(Script.toSlp1(o)) }
                            assertNotNull("${q.id} option $o is a documented, reproducible form", alt)
                            assertEquals("${q.id} option $o", i in q.answers, alt!!.steps.any { Rules.find(it.ruleId)?.topic == target.topic || Rules.find(it.ruleId)?.name == target.name })
                        }
                    }
                    else -> fail("${q.id} unknown type ${q.type}")
                }
                counts.getOrPut(q.cat) { IntArray(3) }[2]++
                continue
            }
            if (q.dir == "F") {
                val (l, r) = q.prompt.split(" + ").let { it[0] to it[1] }
                val plain = joins(l, r, false); val all = allJoins(l, r)
                if (q.cat == "prakriti") {
                    val prag = joins(l, r, true)
                    q.options.forEachIndexed { i, o -> val f = ns(Script.toSlp1(o)); if (i in q.answers) assertTrue("${q.id} answer $o", f in prag) else assertTrue("${q.id} distractor $o is a form of the pragṛhya pair", f !in prag) }
                } else q.options.forEachIndexed { i, o ->
                    val f = ns(Script.toSlp1(o))
                    if (i in q.answers) assertTrue("${q.id} answer $o is not produced by the engine", f in plain)
                    else assertTrue("${q.id} distractor $o is a valid form (ambiguous)", f !in all)
                }
                // no machine-made noise among the wrong options: two vowels in a row
                q.options.forEachIndexed { i, o -> if (i !in q.answers) assertFalse("${q.id} distractor $o: adjacent vowels", Regex("[aAiIuUfFxXeEoO][aAiIuUfFxXeEoO]").containsMatchIn(Script.toSlp1(o))) }
            } else {
                val form = ns(Script.toSlp1(q.prompt))
                val pragrhya = q.cat == "prakriti"
                val acc = q.answers.map { q.options[it].split(" + ") }
                q.options.forEachIndexed { i, o ->
                    val (l, r) = o.split(" + ").let { it[0] to it[1] }
                    if (i in q.answers) assertTrue("${q.id} accepted analysis $o does not join to the form", form in joins(l, r, pragrhya))
                    else {
                        assertFalse("${q.id} distractor $o also joins to $form (ambiguous)", form in allJoins(l, r))
                        // a wrong analysis is made of real words: whichever part differs from the documented pair must be a dictionary word
                        val (al, ar) = acc[0].let { it[0] to it[1] }
                        if (l != al) assertTrue("${q.id} distractor part $l is not a real word", meaningful(Script.toSlp1(l)))
                        if (r != ar) assertTrue("${q.id} distractor part $r is not a real word", meaningful(Script.toSlp1(r)))
                    }
                }
            }
            counts.getOrPut(q.cat) { IntArray(3) }[if (q.dir == "F") 0 else 1]++
        }
        println("QUIZBANK_VERIFIED " + counts.toSortedMap().map { "${it.key}: F=${it.value[0]} R=${it.value[1]} other=${it.value[2]} total=${it.value.sum()}" })
        // both directions are present in every category (the counts themselves are whatever the lesson text supports)
        counts.values.forEach { assertTrue(it[0] > 0 && it[1] > 0) }
    }
}
