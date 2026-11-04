package org.sandhimitra.app

import org.junit.Test
import org.sandhimitra.app.engine.*
import java.io.File

/** Runs every example/exercise of the supplied source tables through the engine; writes build/corpus_report.txt. */
class CorpusTest {
    private fun lines(n: String) = CorpusTest::class.java.getResource("/$n")!!.readText().lines().filter { it.isNotBlank() }.map { it.split("\t") }
    @Test fun corpus() {
        SandhiEngineTest.setup(); val e = SandhiEngineTest.engine
        val sb = StringBuilder(); var okF = 0; var badF = 0; var okR = 0; var badR = 0
        for (r in lines("corpus_forward.tsv")) {
            val (t, k, a, b, c) = r
            val want = Script.toSlp1(c)
            var res = e.joinAll(listOf(a, b))
            var got = res.alts.map { it.surface.replace(" ", "") }
            if (!(want in got)) { val r2 = e.joinAll(listOf(a, b), Ctx(leftPada = false)); got = got + r2.alts.map { it.surface.replace(" ", "") } }
            if (want in got || got.any { it.replace("'", "") == want.replace("'", "") }) okF++ else { badF++; sb.append("FWD t$t $a + $b -> want $c | got ${got.map { Script.slp1ToDevanagari(it) }} ${res.status} ${res.notes}\n") }
        }
        for (r in lines("corpus_reverse.tsv")) {
            val (t, k, w, a, b) = r
            val res = e.split(w)
            val ok = (res.analyses + res.unsupported).any { Script.slp1ToDevanagari(it.left) == Script.slp1ToDevanagari(Script.toSlp1(a)) && Script.slp1ToDevanagari(it.right) == Script.slp1ToDevanagari(Script.toSlp1(b)) }
            if (ok) okR++ else { badR++; sb.append("REV t$t $w -> want $a + $b | got ${res.analyses.map { Script.slp1ToDevanagari(it.left) + "+" + Script.slp1ToDevanagari(it.right) }} ${res.status}\n") }
        }
        File("build/corpus_report.txt").writeText("forward ok=$okF bad=$badF ; reverse ok=$okR bad=$badR\n" + sb)
    }
}
