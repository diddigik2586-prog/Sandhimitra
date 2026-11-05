package org.sandhimitra.app
import org.junit.Test
import org.sandhimitra.app.engine.*
import java.io.File
class AmbiguityDump { @Test fun dump() { SandhiEngineTest.setup(); val e = SandhiEngineTest.engine
  val words = LinkedHashSet<String>()
  AmbiguityDump::class.java.getResource("/corpus_forward.tsv")!!.readText().lines().filter { it.isNotBlank() }.forEach { words += it.split("\t")[4] }
  AmbiguityDump::class.java.getResource("/corpus_reverse.tsv")!!.readText().lines().filter { it.isNotBlank() }.forEach { words += it.split("\t")[2] }
  val sb = StringBuilder()
  for (w in words) { val r = e.split(w); sb.append(w + " :: [" + r.analyses.size + "] " + r.analyses.take(3).joinToString(" | ") { Script.slp1ToDevanagari(it.left) + "+" + Script.slp1ToDevanagari(it.right) + "(" + it.ruleIds.joinToString(",") + ")" } + "\n") }
  File("build/ambiguity.txt").writeText(sb.toString()) } }
