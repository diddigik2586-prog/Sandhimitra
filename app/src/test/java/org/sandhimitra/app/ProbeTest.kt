package org.sandhimitra.app
import org.junit.Test
import org.sandhimitra.app.engine.*
import java.io.File
class ProbeTest {
    @Test fun probe() {
        SandhiEngineTest.setup(); val e = SandhiEngineTest.engine
        fun d(s: String) = Script.slp1ToDevanagari(s)
        val out = StringBuilder()
        val fwd = File("src/test/probe_fwd.txt").readLines().filter { it.isNotBlank() }
        for (l in fwd) { val (a, b) = l.split("+").map { it.trim() }
            val r = e.join(a, b, Ctx())
            out.append("F ").append(l).append(" => ").append(r.status).append(" ").append(r.alts.joinToString(" | ") { d(it.surface) + " [" + it.steps.joinToString(",") { s -> s.ruleId } + "]" }).append(" notes=").append(r.notes).append('\n') }
        val rev = File("src/test/probe_rev.txt").readLines().filter { it.isNotBlank() }
        for (w in rev) { val r = e.split(w)
            out.append("R ").append(w).append(" => ").append(r.status).append(" ").append(r.analyses.joinToString(" | ") { it.parts.joinToString("+") { p -> d(p) } + "(" + it.status + ")" }).append(" unsup=").append(r.unsupported.take(3).joinToString(" | ") { d(it.left) + "+" + d(it.right) }).append('\n') }
        File("build/probe.txt").writeText(out.toString())
    }
}
