package org.sandhimitra.app

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.sandhimitra.app.data.QuizBank
import org.sandhimitra.app.engine.Ctx
import org.sandhimitra.app.engine.Rules
import org.sandhimitra.app.engine.Script
import java.io.File

/**
 * Re-verifies the shipped question bank (assets/quiz/questions.json) against the engine and the content index:
 * every accepted answer is reproduced by the forward engine, every distractor is not, every lesson link exists.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class QuizBankTest {
    private fun ns(s: String) = s.replace(" ", "")

    @Test fun everyShippedQuestionIsVerified() {
        SandhiEngineTest.setup(); val e = SandhiEngineTest.engine
        val qs = QuizBank.parse(File("src/main/assets/quiz/questions.json").readText())
        val idx = JSONObject(File("src/main/assets/content/index.json").readText()).getJSONObject("groups")
        val topicIds = idx.keys().asSequence().flatMap { g -> val a = idx.getJSONArray(g); (0 until a.length()).map { a.getJSONObject(it).getString("id") }.asSequence() }.toSet()
        assertEquals("duplicate ids", qs.size, qs.map { it.id }.toSet().size)
        val counts = HashMap<String, IntArray>()
        for (q in qs) {
            assertEquals(q.id, 4, q.options.size)
            assertTrue(q.id, q.answers.isNotEmpty() && q.answers.all { it in 0..3 })
            assertTrue(q.id, q.cat in listOf("vowel", "consonant", "visarga", "prakriti"))
            assertTrue("${q.id} lesson ${q.lesson}", q.lesson in topicIds); assertTrue("${q.id} practice ${q.practice}", q.practice in topicIds)
            // prakṛtibhāva lessons present pragṛhya words: the engine is told so, exactly as the builder did; the plain-Sandhi form is then a wrong answer
            fun joins(l: String, r: String, pragrhya: Boolean): Set<String> = e.join(l, r, if (pragrhya) Ctx(leftPragrhya = true) else Ctx()).alts.map { ns(it.surface) }.toSet()
            if (q.dir == "O") {
                val applied = q.rules.mapNotNull { Rules.find(it) }
                val target = applied.filter { it.topic == q.lesson }.distinctBy { it.name }
                assertEquals("${q.id} exactly one named rule of the lesson", 1, target.size)
                when (q.type) {
                    "NAME" -> q.options.forEachIndexed { i, o -> assertEquals("${q.id} option $o", i in q.answers, o == target[0].name); if (i !in q.answers) assertTrue("${q.id} distractor $o is an applied rule", applied.none { it.name == o }) }
                    "SUTRA" -> q.options.forEachIndexed { i, o -> assertEquals("${q.id} option $o", i in q.answers, o == "${target[0].sutraDeva} (${target[0].ref})"); if (i !in q.answers) assertTrue("${q.id} distractor $o", applied.none { o.startsWith(it.sutraDeva) }); assertTrue(target[0].verified) }
                    "EXAMPLE" -> {
                        assertEquals(q.id, 4, q.optionSources.size)
                        q.options.forEachIndexed { i, o ->
                            val (l, r) = q.optionSources[i].split(" + ").let { it[0] to it[1] }
                            val jr = e.join(l, r); val alt = jr.alts.firstOrNull { ns(it.surface) == ns(Script.toSlp1(o)) } ?: (if (q.cat == "prakriti" || true) e.join(l, r, Ctx(leftPragrhya = true)).alts.firstOrNull { ns(it.surface) == ns(Script.toSlp1(o)) } else null)
                            assertNotNull("${q.id} option $o is a documented, reproducible form", alt)
                            val usesTarget = alt!!.steps.any { Rules.find(it.ruleId)?.topic == target[0].topic }
                            assertEquals("${q.id} option $o", i in q.answers, usesTarget)
                        }
                    }
                    else -> fail("${q.id} unknown type ${q.type}")
                }
                counts.getOrPut(q.cat) { IntArray(3) }[2]++
                continue
            }
            if (q.dir == "F") {
                val (l, r) = q.prompt.split(" + ").let { it[0] to it[1] }
                val plain = joins(l, r, false)
                val got = if (q.cat == "prakriti" && q.answers.any { ns(Script.toSlp1(q.options[it])) !in plain }) joins(l, r, true) else plain
                q.options.forEachIndexed { i, o -> val hit = ns(Script.toSlp1(o)) in got; assertEquals("${q.id} option $o", i in q.answers, hit) }
            } else {
                val form = ns(Script.toSlp1(q.prompt))
                val pragrhya = q.cat == "prakriti" && q.answers.any { a -> val (l, r) = q.options[a].split(" + ").let { it[0] to it[1] }; form !in joins(l, r, false) }
                q.options.forEachIndexed { i, o ->
                    val (l, r) = o.split(" + ").let { it[0] to it[1] }
                    val hit = form in joins(l, r, pragrhya)
                    assertEquals("${q.id} option $o", i in q.answers, hit)
                }
            }
            counts.getOrPut(q.cat) { IntArray(3) }[if (q.dir == "F") 0 else 1]++
        }
        println("QUIZBANK_VERIFIED " + counts.toSortedMap().map { "${it.key}: F=${it.value[0]} R=${it.value[1]} other=${it.value[2]} total=${it.value.sum()}" })
        // the forward/reverse coverage alone meets the minimum; the additional formats are extra
        for (c in listOf("vowel", "consonant", "visarga")) assertTrue(c, counts.getValue(c)[0] + counts.getValue(c)[1] >= 150)
        // both directions present in every category
        counts.values.forEach { assertTrue(it[0] > 0 && it[1] > 0) }
    }
}
