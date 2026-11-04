package org.sandhimitra.app.engine

/** One recorded derivation step: rule id (see [Rules]), the sounds before and after (SLP1) and an optional note key. */
data class Step(val ruleId: String, val from: String, val to: String, val note: String = "")

/** One result form. [optional] marks forms that are alternatives (vikalpa) of each other. */
data class Alt(val surface: String, val steps: List<Step>, val optional: Boolean = false)

/** Optional grammatical context declared by the user or inferred. */
data class Ctx(val leftPragrhya: Boolean = false, val leftUpasarga: Boolean? = null, val rightDhatu: Boolean? = null,
               /** true: the left part is a complete word (pada); false: a stem/prefix at a morpheme boundary (e.g. ne + ana). */
               val leftPada: Boolean = true)

data class JoinResult(val parts: List<String>, val alts: List<Alt>, val status: Status, val notes: List<String>)

data class Split(val left: String, val right: String, val alt: Alt, val ruleIds: List<String>, val status: Status,
                 val leftSupport: SupportInfo, val rightSupport: SupportInfo, val leftPada: Boolean = true)

data class SplitResult(val input: String, val analyses: List<Split>, val unsupported: List<Split>, val status: Status, val notes: List<String>)

/**
 * Sandhātrī: a deterministic, rule-based Sanskrit Sandhi engine. All strings are SLP1.
 * Forward ([join]) applies the registry rules in Pāṇinian order; reverse ([split]) proposes candidate
 * pairs and keeps only those that [join] reproduces exactly, so both directions share one rule implementation.
 */
class SandhiEngine(lexicon: LexiconApi? = null) {
    private val support = LexSupport(lexicon)
    private val hasLexicon = lexicon != null

    fun lookup(word: String): SupportInfo = support.classify(Script.toSlp1(word).trim())

    // ------------------------------------------------------------------ forward
    fun joinAll(parts0: List<String>, ctx: Ctx = Ctx()): JoinResult {
        val parts = parts0.map { Script.toSlp1(it).trim().replace(" ", "") }
        if (parts.size < 2 || parts.any { it.isEmpty() }) return JoinResult(parts, emptyList(), Status.NO_RESULT, listOf("need_two_parts"))
        val bad = parts.joinToString("") { Script.invalidChars(it) }
        if (bad.isNotEmpty()) return JoinResult(parts, emptyList(), Status.NO_RESULT, listOf("invalid_char:$bad"))
        val notes = mutableListOf<String>()
        var current = listOf(Alt(parts[0], emptyList()))
        for (i in 1 until parts.size) {
            val next = mutableListOf<Alt>()
            for (acc in current) {
                val idx = acc.surface.lastIndexOf(' ')
                val head = if (idx >= 0) acc.surface.substring(0, idx + 1) else ""
                val last = acc.surface.substring(idx + 1)
                val res = joinPair(last, parts[i], if (i == 1) ctx else Ctx(), notes)
                if (res.isEmpty()) return JoinResult(parts, emptyList(), Status.NO_RESULT, notes + "unsupported_final")
                for (r in res) next += Alt(head + r.surface, acc.steps + r.steps, r.optional || acc.optional)
                if (next.size > 12) break
            }
            current = next.distinctBy { it.surface }.take(12)
        }
        var status = Status.VERIFIED
        for (p in parts) {
            val s = support.classify(p).level
            if (!hasLexicon || s == Support.NONE || s == Support.VERBLIKE) { notes += "component_unverified:$p"; status = Status.PROVISIONAL }
        }
        if (current.any { a -> a.steps.any { st -> Rules.find(st.ruleId)?.verified == false } }) status = Status.PROVISIONAL
        if (notes.any { it == "padanta_conjunct" }) status = Status.PROVISIONAL
        return JoinResult(parts, current, status, notes.distinct())
    }

    fun join(left: String, right: String, ctx: Ctx = Ctx()) = joinAll(listOf(left, right), ctx)

    /** Core pairwise rule application on two SLP1 strings; returns the alternatives (empty if unsupported). */
    internal fun joinPair(L: String, R: String, ctx: Ctx, notes: MutableList<String>): List<Alt> {
        val a = L.last(); val b = R.first()
        if (Ph.isVowel(b) || L.endsWith("3")) pragrhya(L, ctx, notes)?.let { return listOf(Alt("$L $R", listOf(Step(it, L.takeLast(2), "", "unchanged")))) }
        return when {
            Ph.isVowel(a) && Ph.isVowel(b) -> vowelVowel(L, R, ctx)
            Ph.isVowel(a) -> vowelConsonant(L, R)
            a == 'H' || a == 's' || a == 'r' -> visarga(L, R)
            a == 'm' || a == 'M' -> anusvara(L, R)
            a in "NRn" -> nasalFinal(L, R, notes)
            Ph.isJhalStop(a) -> stopFinal(L, R, notes, ctx)
            a in "yvl" -> listOf(Alt(L + R, emptyList()))
            else -> emptyList()
        }
    }

    private fun pragrhya(L: String, ctx: Ctx, notes: MutableList<String>): String? {
        if (L.endsWith("3")) return "P01"
        if (L == "amI" || L == "amU") return "P03"
        if (ctx.leftPragrhya && L.last() in "IUe") return "P02"
        if (L in setOf("i", "u", "aho", "utAho", "atho")) return "P04"
        if (L.last() in "IUe") notes += "hint_pragrhya_unchecked"
        return null
    }

    private fun a1(res: String, vararg st: Step, opt: Boolean = false) = Alt(res, st.toList(), opt)

    private val eDhatuHints = listOf("eti", "eD", "ej", "ez", "ew", "oz", "ok", "ow")
    private val rDhatuHints = listOf("fc", "fD", "fj", "fN", "fh")

    private fun vowelVowel(L: String, R: String, ctx: Ctx): List<Alt> {
        val a = L.last(); val b = R.first(); val pre = L.dropLast(1); val post = R.drop(1)
        val upasarga = ctx.leftUpasarga ?: (L in Ph.UPASARGAS)
        val dhatu = ctx.rightDhatu ?: (eDhatuHints.any { R.startsWith(it) } || rDhatuHints.any { R.startsWith(it) })
        return when (a) {
            'a', 'A' -> when (b) {
                'a', 'A' -> listOf(a1(pre + "A" + post, Step("V01", "$a+$b", "A")))
                'i', 'I' -> listOf(a1(pre + "e" + post, Step("V02", "$a+$b", "e")))
                'u', 'U' -> listOf(a1(pre + "o" + post, Step("V02", "$a+$b", "o")))
                'f', 'F' -> if (upasarga && dhatu) listOf(a1(pre + "Ar" + post, Step("V04", "$a+$b", "Ar", "6.1.91")))
                            else listOf(a1(pre + "ar" + post, Step("V02", "$a+$b", "ar", "1.1.51")))
                'x', 'X' -> listOf(a1(pre + "al" + post, Step("V02", "$a+$b", "al", "1.1.51")))
                'e', 'E', 'o', 'O' -> {
                    if (upasarga && dhatu && b in "eo") {
                        if (R.startsWith("eti") || R.startsWith("eD")) listOf(a1(pre + (if (b == 'e') "E" else "O") + post, Step("V04", "$a+$b", if (b == 'e') "E" else "O", "6.1.89")))
                        else listOf(a1(pre + b + post, Step("V08", "$a+$b", b.toString())))
                    } else listOf(a1(pre + (if (b == 'e' || b == 'E') "E" else "O") + post, Step("V03", "$a+$b", if (b == 'e' || b == 'E') "E" else "O")))
                }
                else -> emptyList()
            }
            'i', 'I' -> if (b == 'i' || b == 'I') listOf(a1(pre + "I" + post, Step("V01", "$a+$b", "I"))) else listOf(a1(pre + "y" + R, Step("V05", a.toString(), "y")))
            'u', 'U' -> if (b == 'u' || b == 'U') listOf(a1(pre + "U" + post, Step("V01", "$a+$b", "U"))) else listOf(a1(pre + "v" + R, Step("V05", a.toString(), "v")))
            'f', 'F' -> if (b in "fFxX") listOf(a1(pre + "F" + post, Step("V01", "$a+$b", "F"), opt = true), a1(pre + "f" + post, Step("V01", "$a+$b", "f", "vartika"), opt = true))
                        else listOf(a1(pre + "r" + R, Step("V05", a.toString(), "r")))
            'x', 'X' -> if (b in "fFxX") listOf(a1(pre + "X" + post, Step("V01", "$a+$b", "X"), opt = true), a1(pre + "x" + post, Step("V01", "$a+$b", "x", "vartika"), opt = true))
                        else listOf(a1(pre + "l" + R, Step("V05", a.toString(), "l")))
            'e', 'o' -> if (b == 'a' && ctx.leftPada) {
                val out = mutableListOf(a1("$L'$post", Step("V07", "$a+a", "$a'")))
                if (L == "go") {
                    out[0] = out[0].copy(optional = true)
                    out += Alt(L + " " + R, listOf(Step("V07", "go+a", "go a", "6.1.122")), true)
                    out += Alt("gavA$post", listOf(Step("V07", "go+a", "gavA", "6.1.123")), true)
                }
                out
            } else ayadi(pre, a, R, ctx.leftPada)
            'E', 'O' -> ayadi(pre, a, R, ctx.leftPada)
            else -> emptyList()
        }
    }

    private fun ayadi(pre: String, a: Char, R: String, pada: Boolean): List<Alt> {
        val y = when (a) { 'e' -> "ay"; 'o' -> "av"; 'E' -> "Ay"; else -> "Av" }
        val dropped = if (a == 'e' || a == 'o') "a" else "A"
        if (!pada) return listOf(Alt(pre + y + R, listOf(Step("V06", a.toString(), y))))
        return listOf(
            Alt(pre + y + R, listOf(Step("V06", a.toString(), y)), true),
            Alt("$pre$dropped $R", listOf(Step("V06", a.toString(), y), Step("V09", y, dropped)), true)
        )
    }

    private fun vowelConsonant(L: String, R: String): List<Alt> {
        val a = L.last(); val b = R.first()
        if (b == 'C') {
            if (Ph.isShort(a)) return listOf(Alt(L + "c" + R, listOf(Step("C08", "$a+C", "${a}cC", "6.1.73"), Step("C02", "tC", "cC"))))
            if (a in "AIUFX") {
                val nitya = L == "A" || L == "mA"
                val withTuk = Alt(L + "c" + R, listOf(Step("C08", "$a+C", "${a}cC", if (nitya) "6.1.74" else "6.1.76"), Step("C02", "tC", "cC")), !nitya)
                return if (nitya) listOf(withTuk) else listOf(withTuk, Alt(L + R, listOf(Step("C08", "$a+C", "$a C", "6.1.76")), true))
            }
        }
        return listOf(Alt(L + R, emptyList()))
    }

    private fun anusvara(L: String, R: String): List<Alt> {
        val c = L.last(); val b = R.first(); val base = L.dropLast(1)
        if (Ph.isVowel(b)) return listOf(Alt(L + R, emptyList()))
        if (L == "sam" && R.startsWith("rA")) return listOf(Alt(L + R, listOf(Step("C06", "m", "m", "8.3.25"))))
        val st = if (c == 'm') listOf(Step("C06", "m", "M")) else emptyList()
        val out = mutableListOf(Alt(base + "M" + R, st, true))
        when {
            b in "yvl" -> out += Alt(base + "~" + b + R, st + Step("C07", "M$b", "~$b", "8.4.59"), true)
            Ph.varga(b) >= 0 -> out += Alt(base + Ph.nasalOf(b) + R, st + Step("C07", "M$b", "${Ph.nasalOf(b)}$b", "8.4.59"), true)
        }
        if (out.size == 1) out[0] = out[0].copy(optional = false)
        return out
    }

    private fun nasalFinal(L: String, R: String, notes: MutableList<String>): List<Alt> {
        val c = L.last(); val b = R.first(); val base = L.dropLast(1)
        if (Ph.isVowel(b)) {
            val prevShort = L.length >= 2 && Ph.isShort(L[L.length - 2])
            return if (prevShort) listOf(Alt(L + c + R, listOf(Step("C09", "$c+${b}", "$c$c$b"))))
            else listOf(Alt(L + R, emptyList()))
        }
        if (c == 'n') {
            when {
                b in "cCwWtT" && !L.endsWith("praSAn") -> {
                    val s = when (b) { 'c', 'C' -> "S"; 'w', 'W' -> "z"; else -> "s" }
                    val second = if (s == "S") Step("C02", "s$b", "S$b") else if (s == "z") Step("C03", "s$b", "z$b") else null
                    return listOf(Alt(base + "M" + s + R, listOfNotNull(Step("C12", "n$b", "Ms$b"), second)))
                }
                b == 'l' -> return listOf(Alt(base + "~" + "l" + R, listOf(Step("C13", "nl", "~ll"))))
                b in "jJYS" -> return listOf(Alt(base + "Y" + R, listOf(Step("C02", "n$b", "Y$b"))))
                b in "qQR" -> return listOf(Alt(base + "R" + R, listOf(Step("C03", "n$b", "R$b"))))
            }
        }
        return listOf(Alt(L + R, emptyList()))
    }

    private fun stopFinal(L: String, R: String, notes: MutableList<String>, ctx: Ctx): List<Alt> {
        val c0 = L.last(); val b = R.first(); val base = L.dropLast(1)
        if (base.isNotEmpty() && Ph.isCons(base.last())) notes += "padanta_conjunct"
        val steps = mutableListOf<Step>()
        if (L == "ud" && (R.startsWith("sTA"))) return listOf(Alt("utTA" + R.drop(3), listOf(Step("C01", "d", "d"), Step("C11", "sTA", "TA", "8.4.61"), Step("C04", "d", "t"))))
        var cur = if (ctx.leftPada || b in "gGjJqQdDbB") Ph.jash(c0) else c0
        if (cur != c0 || ctx.leftPada) steps += Step(if (ctx.leftPada) "C01" else "C01", c0.toString(), cur.toString(), if (ctx.leftPada) "" else "8.4.53")
        var bn = b
        when {
            Ph.isTVarga(cur) && b != 'n' && (Ph.varga(b) == 1 || b == 'S') -> { val n = Ph.toPalatal(cur); steps += Step("C02", "$cur$b", "$n$b"); cur = n }
            Ph.isTVarga(cur) && (Ph.varga(b) == 2 && b != 'n') -> { val n = Ph.toRetroflex(cur); steps += Step("C03", "$cur$b", "$n$b"); cur = n }
            Ph.varga(cur) == 1 && b in "tTdDns" && b != 'n' || (Ph.varga(cur) == 1 && b == 'n') -> { bn = Ph.toPalatal(b); steps += Step("C02", "$cur$b", "$cur$bn") }
            Ph.varga(cur) == 2 && b in "tTdDns" -> {
                val exempt = R.startsWith("nAm") || R.startsWith("nagarI") || R.startsWith("navati")
                if (exempt) { bn = Ph.toRetroflex(b); steps += Step("C03", "$cur$b", "$cur$bn") }
            }
        }
        if (Ph.isKhar(bn) && Ph.isJhalStop(cur)) { val n = Ph.car(cur); if (n != cur) steps += Step("C04", cur.toString(), n.toString()); cur = n }
        val rest = R.drop(1)
        val out = mutableListOf(Alt(base + cur + bn + rest, steps.toList()))
        val s1 = if (out[0].steps.isEmpty()) emptyList() else out[0].steps
        if (b == 'n' || b == 'm') {
            out[0] = out[0].copy(optional = true)
            out += Alt(base + Ph.nasalOf(cur) + R, s1 + Step("C05", "$cur$b", "${Ph.nasalOf(cur)}$b", "8.4.45"), true)
        }
        if (b == 'h') {
            out[0] = out[0].copy(optional = true)
            out += Alt(base + cur + Ph.voicedAspirate(cur) + rest, s1 + Step("C11", "${cur}h", "${cur}${Ph.voicedAspirate(cur)}", "8.4.62"), true)
        }
        if (b == 'S' && R.length > 1 && (Ph.isVowel(R[1]) || R[1] in "hyvr")) {
            out[0] = out[0].copy(optional = true)
            out += Alt(base + cur + "C" + rest, s1 + Step("C10", "S", "C", "8.4.63"), true)
        }
        return out
    }

    private fun visarga(L: String, R: String): List<Alt> {
        val last = L.last(); val core = L.dropLast(1); val b = R.first()
        val preV = core.lastOrNull() ?: ' '
        val sru = last != 'r'
        val s0 = if (last == 's') listOf(Step("H01", "s", "ru")) else emptyList()
        val coreNoV = if (core.isNotEmpty()) core.dropLast(1) else core
        if (sru && (core == "sa" || core == "eza") && Ph.isCons(b)) return listOf(Alt("$core $R", s0 + Step("H11", "${core}H", core)))
        if (Ph.isKhar(b)) {
            val h = s0 + Step("H02", "ru", "H")
            return when (b) {
                'c', 'C' -> listOf(Alt(core + "S" + R, h + Step("H03", "H$b", "S$b") + Step("C02", "s$b", "S$b")))
                'w', 'W' -> listOf(Alt(core + "z" + R, h + Step("H03", "H$b", "z$b") + Step("C03", "s$b", "z$b")))
                't', 'T' -> listOf(Alt(core + "s" + R, h + Step("H03", "H$b", "s$b")))
                'k', 'K', 'p', 'P' -> listOf(Alt(core + "H " + R, h + Step("H05", "H$b", "H$b")))
                else -> if (R.length > 1 && Ph.isKhar(R[1])) listOf(Alt(core + "H " + R, h + Step("H04", "H$b", "H$b", "8.3.35")))
                        else listOf(Alt(core + b + R, h + Step("H04", "H$b", "$b$b", "8.3.36"), true), Alt(core + "H " + R, h + Step("H04", "H$b", "H$b", "8.3.36"), true))
            }
        }
        if (Ph.isVowel(b)) {
            if (sru && preV == 'a') {
                return if (b == 'a') listOf(Alt(coreNoV + "o'" + R.drop(1), s0 + listOf(Step("H06", "ru", "u"), Step("V02", "a+u", "o"), Step("V07", "o+a", "o'"))))
                else listOf(
                    Alt("$core $R", s0 + listOf(Step("H08", "ru", "y"), Step("V09", "y", "")), true),
                    Alt(core + "y" + R, s0 + Step("H08", "ru", "y"), true))
            }
            if (sru && preV == 'A') return listOf(
                Alt("$core $R", s0 + listOf(Step("H08", "ru", "y"), Step("V09", "y", "")), true),
                Alt(core + "y" + R, s0 + Step("H08", "ru", "y"), true))
            return listOf(Alt(core + "r" + R, s0 + Step("H12", "ru", "r")))
        }
        if (Ph.isVoicedCons(b)) {
            if (b == 'r' && !(sru && preV == 'a')) {
                return listOf(Alt(coreNoV + Ph.longOf(preV) + R, s0 + Step("H10", "r r", "${Ph.longOf(preV)} r", "6.3.111")))
            }
            if (sru && preV == 'a') return listOf(Alt(coreNoV + "o" + R, s0 + listOf(Step("H07", "ru", "u"), Step("V02", "a+u", "o"))))
            if (sru && preV == 'A') return listOf(Alt("$core $R", s0 + listOf(Step("H08", "ru", "y"), Step("H09", "y", ""))))
            return listOf(Alt(core + "r" + R, s0 + Step("H12", "ru", "r")))
        }
        return emptyList()
    }

    // ------------------------------------------------------------------ reverse
    private val lEnds: List<String> by lazy {
        val v = Ph.VOWELS.map { it.toString() }
        val h = Ph.VOWELS.map { "${it}H" }
        v + h + listOf("k", "g", "c", "j", "w", "q", "t", "d", "p", "b", "N", "Y", "R", "n", "m", "r", "M", "s", "S", "z", "l", "y", "v", "H")
    }
    private val rStarts: List<String> by lazy { (Ph.VOWELS + "kKgGNcCjJYwWqQRtTdDnpPbBmyrlvSzsh").map { it.toString() } }

    fun split(input: String): SplitResult {
        val raw = Script.toSlp1(input).trim()
        val spaceAt = raw.indexOf(' ').let { if (it < 0) -1 else it }
        val J = raw.replace(" ", "")
        if (J.length < 2) return SplitResult(input, emptyList(), emptyList(), Status.NO_RESULT, listOf("too_short"))
        val bad = Script.invalidChars(J)
        if (bad.isNotEmpty()) return SplitResult(input, emptyList(), emptyList(), Status.NO_RESULT, listOf("invalid_char:$bad"))
        if (J.length > 40) return SplitResult(input, emptyList(), emptyList(), Status.NO_RESULT, listOf("too_long"))
        val found = LinkedHashMap<String, Split>()
        val n = J.length
        for (s in 0..n) for (m in 0..3) {
            if (s + m > n) continue
            if (spaceAt >= 0 && !(spaceAt >= s && spaceAt <= s + m)) continue
            val prefix = J.substring(0, s); val suffix = J.substring(s + m)
            for (a in lEnds) {
                val L = prefix + a
                for (b in rStarts) {
                    val R = b + suffix
                    val notes = mutableListOf<String>()
                    if (L.none { Ph.isVowel(it) } || R.none { Ph.isVowel(it) }) continue
                    val variants = mutableListOf(true to (try { joinPair(L, R, Ctx(), notes) } catch (e: Exception) { emptyList() }))
                    if (a.length == 1 && a[0] in "eoEO") variants += false to (try { joinPair(L, R, Ctx(leftPada = false), notes) } catch (e: Exception) { emptyList() })
                    for ((pada, alts) in variants) for (alt in alts) {
                        if (alt.surface.replace(" ", "") == J && alt.steps.isNotEmpty()) {
                            val key = "$L|$R|$pada"
                            if (key !in found) {
                                val ls = support.classify(L); val rs = support.classify(R)
                                val ids = alt.steps.map { it.ruleId }.distinct()
                                found[key] = Split(L, R, alt, ids, Status.PROVISIONAL, ls, rs, pada)
                            }
                        }
                    }
                }
            }
        }
        // prune trivial candidates (very short invented parts)
        val ok = mutableListOf<Split>(); val unsupported = mutableListOf<Split>()
        for (sp in found.values) {
            val ls = sp.leftSupport.level; val rs = sp.rightSupport.level
            val bothKnown = ls != Support.NONE && rs != Support.NONE
            if (!hasLexicon) { unsupported += sp; continue }
            if (bothKnown) {
                val allVerifiedRules = sp.ruleIds.all { Rules.find(it)?.verified == true }
                val st = if (ls == Support.EXACT && rs == Support.EXACT && allVerifiedRules) Status.VERIFIED else Status.PROVISIONAL
                ok += sp.copy(status = st)
            } else unsupported += sp
        }
        fun score(s: Split) = (if (s.status == Status.VERIFIED) 100 else 0) + s.leftSupport.level.ordinal.let { 3 - it } * 5 + s.rightSupport.level.ordinal.let { 3 - it } * 5 + minOf(s.left.length, s.right.length).coerceAtMost(6)
        ok.sortByDescending { score(it) }
        unsupported.sortByDescending { score(it) }
        val status = when {
            ok.any { it.status == Status.VERIFIED } -> Status.VERIFIED
            ok.isNotEmpty() -> Status.PROVISIONAL
            else -> Status.NO_RESULT
        }
        val notes = mutableListOf<String>()
        if (status == Status.NO_RESULT) notes += "no_supported_analysis"
        return SplitResult(input, ok.take(20), unsupported.take(10), status, notes)
    }
}
