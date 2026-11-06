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
                 val leftSupport: SupportInfo, val rightSupport: SupportInfo, val leftPada: Boolean = true,
                 /** all words of the analysis (more than two when a form joins three or more words) */
                 val parts: List<String> = listOf(left, right))

data class SplitResult(val input: String, val analyses: List<Split>, val unsupported: List<Split>, val status: Status, val notes: List<String>)

/**
 * Sandhātrī: a deterministic, rule-based Sanskrit Sandhi engine. All strings are SLP1.
 * Forward ([join]) applies the registry rules in Pāṇinian order; reverse ([split]) proposes candidate
 * pairs and keeps only those that [join] reproduces exactly, so both directions share one rule implementation.
 */
class SandhiEngine(private val lexicon: LexiconApi? = null) {
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
                for (r in res) next += Alt(head + spaced(last, parts[i], r), acc.steps + r.steps, r.optional || acc.optional)
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

    /** A declined word or an indeclinable (रामः, रामेण, रामम्, तत्र) — as opposed to a bare stem used as a compound member (राम). */
    private fun isPadaForm(w: String): Boolean {
        if (w.endsWith("H") || w.endsWith("M")) return true
        if (lexicon?.closedClass(w) == "avyaya") return true
        val c = support.classify(w)
        return c.level == Support.DERIVED && c.ending.isNotEmpty()
    }

    /** Words stay separate when the junction leaves them separate (रामो गच्छति, रामेण सह, रामं पश्यति);
     *  they close up when the dictionary knows the whole as one word (मनोहरः, अन्तःकरणम्, संहितापाठः). */
    private fun spaced(L: String, R: String, r: Alt): String {
        if (!hasLexicon) return r.surface
        var surf = r.surface
        val ids = r.steps.map { it.ruleId }
        val open = (r.steps.isEmpty() && !(Ph.isCons(L.last()) && Ph.isVowel(R.first()))) || "H07" in ids || "H10" in ids || ("C06" in ids && "C07" !in ids)
        if (!surf.contains(' ') && open && isPadaForm(L)) {
            val cut = surf.length - R.length
            surf = surf.substring(0, cut) + " " + surf.substring(cut)
        }
        if (surf.contains(' ')) {
            val closed = surf.replace(" ", "")
            if (!ADJ_VOWELS.containsMatchIn(closed) && (lexicon!!.isHeadword(closed) || lexicon.isAttested(closed) || lexicon.isHeadword(support.stemOf(closed)) && !lexicon.isSimple(support.stemOf(closed)))) surf = closed
        }
        return surf
    }

    /** Core pairwise rule application on two SLP1 strings; returns the alternatives (empty if unsupported). */
    internal fun joinPair(L: String, R: String, ctx: Ctx, notes: MutableList<String>): List<Alt> {
        val a = L.last(); val b = R.first()
        if (Ph.isVowel(b) || L.endsWith("3")) pragrhya(L, ctx, notes)?.let { return listOf(Alt("$L $R", listOf(Step(it, L.takeLast(2), "", "unchanged")))) }
        SPECIAL[L to R]?.let { sp -> return sp.map { Alt(it.first, listOf(Step("V10", L.takeLast(1) + "+" + R.take(1), it.first.take(0), it.second)), sp.size > 1) } }
        if (Ph.isVowel(b) && a in "IU" && !ctx.leftPragrhya && looksDual(L)) {
            val normal = joinPairCore(L, R, ctx, notes)
            return listOf(Alt("$L $R", listOf(Step("P02", L.takeLast(1), "", "dual")), true)) + normal.map { it.copy(optional = true) }
        }
        return joinPairCore(L, R, ctx, notes)
    }

    /** Nominative dual in ī/ū/e is pragṛhya; recognised when the stem is a headword and the form itself is not. */
    private fun looksDual(L: String): Boolean {
        val lx = lexicon ?: return false
        if (lx.isHeadword(L)) return false
        val st = L.dropLast(1)
        return when (L.last()) { 'I' -> lx.isHeadword(st + "i"); 'U' -> lx.isHeadword(st + "u"); else -> false }
    }

    private fun joinPairCore(L: String, R: String, ctx: Ctx, notes: MutableList<String>): List<Alt> {
        val a = L.last(); val b = R.first()
        return when {
            Ph.isVowel(a) && Ph.isVowel(b) -> vowelVowel(L, R, ctx)
            Ph.isVowel(a) -> vowelConsonant(L, R)
            a == 'H' || a == 's' || a == 'r' -> visarga(L, R)
            a == 'm' || a == 'M' -> anusvara(L, R)
            a in "NRn" -> nasalFinal(L, R, notes, ctx)
            Ph.isJhalStop(a) -> stopFinal(L, R, notes, ctx)
            a in "yvl" -> listOf(Alt(L + R, emptyList()))
            (a == 'z' || a == 'S') && !ctx.leftPada -> {
                if (a == 'z' && b in "tTdDn") { val r = Ph.toRetroflex(b); listOf(Alt(L + r + R.drop(1), listOf(Step("C03", "z$b", "z$r")))) } else listOf(Alt(L + R, emptyList()))
            }
            else -> emptyList()
        }
    }

    /** Irregular joins taught as vārttikas in the source (topics t09, t10, t14, t17...). Value: result forms with a note. */
    private val SPECIAL: Map<Pair<String, String>, List<Pair<String, String>>> = mapOf(
        ("akza" to "UhinI") to listOf("akzOhiRI" to "vartika"), ("pra" to "UhaH") to listOf("prOhaH" to "vartika"), ("pra" to "UQaH") to listOf("prOQaH" to "vartika"),
        ("pra" to "eSaH") to listOf("prEzaH" to "vartika"), ("pra" to "eSyaH") to listOf("prEzyaH" to "vartika"), ("sva" to "IraH") to listOf("svEraH" to "vartika"),
        ("sva" to "IriRI") to listOf("svEriRI" to "vartika"), ("sukha" to "ftaH") to listOf("sukhArtaH" to "vartika"), ("sTUla" to "otuH") to listOf("sTUlOtuH" to "vartika", "sTUlotuH" to "vartika"),
        ("bimba" to "oSTaH") to listOf("bimbOSTaH" to "vartika", "bimboSTaH" to "vartika"), ("tava" to "oSTaH") to listOf("tavOSTaH" to "vartika"),
        ("Saka" to "anDuH") to listOf("SakanDuH" to "vartika"), ("karka" to "anDuH") to listOf("karkanDuH" to "vartika"), ("go" to "indraH") to listOf("gavendraH" to "vartika"),
        ("kula" to "awA") to listOf("kulawA" to "vartika"), ("sAra" to "aNgaH") to listOf("sAraNgaH" to "vartika"), ("patat" to "aYjaliH") to listOf("pataYjaliH" to "vartika"),
        ("manas" to "ISA") to listOf("maRISA" to "vartika"), ("mfta" to "aRqaH") to listOf("mArtaRqaH" to "vartika")
    ).filterValues { it.isNotEmpty() }

    private fun pragrhya(L: String, ctx: Ctx, notes: MutableList<String>): String? {
        if (L.endsWith("3")) return "P01"
        if (L == "amI" || L == "amU") return "P03"
        if (ctx.leftPragrhya && L.last() in "IUe") return "P02"
        if (L in setOf("i", "u", "aho", "utAho", "atho")) return "P04"
        if (L.last() in "IUe") notes += "hint_pragrhya_unchecked"
        return null
    }

    private fun a1(res: String, vararg st: Step, opt: Boolean = false) = Alt(res, st.toList(), opt)

    private val eDhatuHints = listOf("eti", "eD", "eh", "ej", "ez", "ew", "oz", "ok", "ow")
    private val rDhatuHints = listOf("fc", "fD", "fj", "fN", "fh")

    private fun vowelVowel(L: String, R: String, ctx: Ctx): List<Alt> {
        val a = L.last(); val b = R.first(); val pre = L.dropLast(1); val post = R.drop(1)
        val upasarga = ctx.leftUpasarga ?: (L in Ph.UPASARGAS)
        if (a in "aA" && R == "om") return listOf(a1(pre + "om", Step("V08", "$a+o", "o", "6.1.95")))
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
                if (L.length <= 3) {
                out[0] = out[0].copy(optional = true)
                out += Alt(natva(pre + (if (a == 'e') "ay" else "av") + R), listOf(Step("V06", a.toString(), if (a == 'e') "ay" else "av", "stem")), true)
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
        if (!pada) return listOf(Alt(natva(pre + y + R), listOf(Step("V06", a.toString(), y))))
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

    /** Ṇatva (8.4.1) inside a word: n after r/ṣ/ṛ(+ vowels, h y v r, k- and p-varga, anusvāra) becomes ṇ before a vowel or n m y v. */
    private fun natva(w: String): String {
        val sb = StringBuilder(w); var armed = false
        for (i in sb.indices) {
            val c = sb[i]
            when {
                c in "rzfF" -> armed = true
                c == 'n' && armed && i + 1 < sb.length && (Ph.isVowel(sb[i + 1]) || sb[i + 1] in "nmyv") -> { sb[i] = 'R'; armed = false }
                armed && (Ph.isVowel(c) || c in "hyvrkKgGNpPbBmM") -> {}
                else -> armed = false
            }
        }
        return sb.toString()
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

    private fun nasalFinal(L: String, R: String, notes: MutableList<String>, ctx: Ctx = Ctx()): List<Alt> {
        val c = L.last(); val b = R.first(); val base = L.dropLast(1)
        if (!ctx.leftPada && c == 'n' && !Ph.isVowel(b) && b in "Szsh") return listOf(Alt(base + "M" + R, listOf(Step("C15", "n$b", "M$b"))))
        if (!ctx.leftPada && c == 'n' && Ph.isJhalStop(b)) return listOf(Alt(L + R, listOf(Step("C15", "n$b", "M$b"), Step("C07", "M$b", "n$b", "8.4.58"))))
        if (Ph.isVowel(b)) {
            val prevShort = L.length >= 2 && Ph.isShort(L[L.length - 2])
            return if (prevShort) listOf(Alt(L + c + R, listOf(Step("C09", "$c+${b}", "$c$c$b"))))
            else listOf(Alt(L + R, emptyList()))
        }
        if (c == 'n') {
            when {
                b in "cC" && L.endsWith("praSAn") -> return listOf(Alt(base + "Y" + R, listOf(Step("C02", "n$b", "Y$b", "8.3.7"))))
                b in "wW" && L.endsWith("praSAn") -> return listOf(Alt(base + "R" + R, listOf(Step("C03", "n$b", "R$b", "8.3.7"))))
                b in "cCwWtT" && !L.endsWith("praSAn") -> {
                    val s = when (b) { 'c', 'C' -> "S"; 'w', 'W' -> "z"; else -> "s" }
                    val second = if (s == "S") Step("C02", "s$b", "S$b") else if (s == "z") Step("C03", "s$b", "z$b") else null
                    if (!ctx.leftPada) return listOf(Alt(L + R, emptyList()))
                    val st = listOfNotNull(Step("C12", "n$b", "Ms$b"), second)
                    return listOf(Alt(base + "M" + s + R, st, true), Alt(base + "~" + s + R, st + Step("C12", "M", "~", "8.3.2"), true))
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
        if (L == "ud" && R.startsWith("stamB")) return listOf(Alt("ut" + R.drop(1), listOf(Step("C01", "d", "d"), Step("C11", "stamB", "tamB", "8.4.61"), Step("C04", "d", "t"))))
        if (Ph.isTVarga(Ph.jash(c0)) && b == 'l') return listOf(Alt(base + "l" + R, listOf(Step("C01", c0.toString(), Ph.jash(c0).toString()), Step("C13", "${Ph.jash(c0)}l", "ll"))))
        var cur = if (ctx.leftPada || b in "gGjJqQdDbB") Ph.jash(c0) else c0
        if (cur != c0 || ctx.leftPada) steps += Step(if (ctx.leftPada) "C01" else "C01", c0.toString(), cur.toString(), if (ctx.leftPada) "" else "8.4.53")
        var bn = b
        when {
            Ph.isTVarga(cur) && b != 'n' && (Ph.varga(b) == 1 || b == 'S') -> { val n = Ph.toPalatal(cur); steps += Step("C02", "$cur$b", "$n$b"); cur = n }
            Ph.isTVarga(cur) && (Ph.varga(b) == 2 && b != 'n') -> { val n = Ph.toRetroflex(cur); steps += Step("C03", "$cur$b", "$n$b"); cur = n }
            Ph.varga(cur) == 1 && b in "tTdDns" && b != 'n' || (Ph.varga(cur) == 1 && b == 'n') -> { bn = Ph.toPalatal(b); steps += Step("C02", "$cur$b", "$cur$bn") }
            Ph.varga(cur) == 2 && b in "tTdDns" -> {
                val exempt = R.startsWith("nAm") || R.startsWith("nagar") || R.startsWith("navati")
                if (exempt) { bn = Ph.toRetroflex(b); steps += Step("C03", "$cur$b", "$cur$bn") }
            }
        }
        if (Ph.isKhar(bn) && Ph.isJhalStop(cur)) { val n = Ph.car(cur); if (n != cur) steps += Step("C04", cur.toString(), n.toString()); cur = n }
        val rest = R.drop(1)
        val out = mutableListOf(Alt(base + cur + bn + rest, steps.toList()))
        val s1 = if (out[0].steps.isEmpty()) emptyList() else out[0].steps
        if (b == 'n' || b == 'm') {
            out[0] = out[0].copy(optional = true)
            out.add(0, Alt(base + Ph.nasalOf(cur) + bn + rest, s1 + Step("C05", "$cur$b", "${Ph.nasalOf(cur)}$b", "8.4.45"), true))
        }
        if (b == 'h') {
            out[0] = out[0].copy(optional = true)
            out += Alt(base + cur + Ph.voicedAspirate(cur) + rest, s1 + Step("C11", "${cur}h", "${cur}${Ph.voicedAspirate(cur)}", "8.4.62"), true)
        }
        if (b == 'S' && R.length > 1 && (Ph.isVowel(R[1]) || R[1] in "hyvrlmnNYR")) {
            out[0] = out[0].copy(optional = true)
            out += Alt(base + cur + "C" + rest, s1 + Step("C10", "S", "C", "8.4.63"), true)
        }
        return out
    }

    private fun visarga(L: String, R: String): List<Alt> {
        val last = L.last(); val core = L.dropLast(1)
        val sru = last != 'r'
        val s0 = if (last == 's') listOf(Step("H01", "s", "ru")) else emptyList<Step>()
        if (last == 'H' && L in RADICAL_R) {
            return visargaCore(core + "r", R, false, core + "r", emptyList())
        }
        return visargaCore(L, R, sru, L, s0)
    }

    private val RADICAL_R = setOf("antaH", "punaH", "prAtaH", "svaH", "ahaH")
    private val GATI_S = setOf("namaH", "puraH")
    /** suffix-like second members before which the visarga may become s (सोऽपदादौ 8.3.38): यशस्कल्पम्, पयस्पाशम् */
    private val SUFFIX_KP = listOf("kalp", "kAmy", "pAS", "kAra")
    private val IU_S = setOf("niH", "duH", "AviH", "prAduH", "bahiH", "DanuH", "sarpiH", "jyotiH", "haviH", "arciH", "catuH", "AyuH")

    private fun visargaCore(L0: String, R: String, sruIn: Boolean, unused: String, s0in: List<Step>): List<Alt> {
        val last = L0.last(); val core = L0.dropLast(1); val b = R.first()
        val preV = core.lastOrNull() ?: ' '
        val sru = sruIn && last != 'r'
        val s0 = if (L0.last() == 's') listOf(Step("H01", "s", "ru")) else s0in
        val coreNoV = if (core.isNotEmpty()) core.dropLast(1) else core
        if (b in "kKpP" && L0 in GATI_S) return listOf(Alt(core + "s" + R, s0 + Step("H13", "H$b", "s$b", "8.3.40")))
        if (b in "kKpP" && L0 in IU_S) return listOf(Alt(core + "z" + R, s0 + Step("H14", "H$b", "z$b", "8.3.41")))
        if (sru && (core == "sa" || core == "eza") && Ph.isCons(b)) return listOf(Alt("$core $R", s0 + Step("H11", "${core}H", core)))
        if (Ph.isKhar(b)) {
            val h = s0 + Step("H02", "ru", "H")
            if (b !in "Szs" && R.length > 1 && R[1] in "Szs") return listOf(Alt(core + "H " + R, h + Step("H04", "H$b", "H$b", "8.3.35")))
            return when (b) {
                'c', 'C' -> listOf(Alt(core + "S" + R, h + Step("H03", "H$b", "S$b") + Step("C02", "s$b", "S$b")))
                'w', 'W' -> listOf(Alt(core + "z" + R, h + Step("H03", "H$b", "z$b") + Step("C03", "s$b", "z$b")))
                't', 'T' -> listOf(Alt(core + "s" + R, h + Step("H03", "H$b", "s$b")))
                'k', 'K', 'p', 'P' -> if (sru && SUFFIX_KP.any { R.startsWith(it) }) listOf(Alt(core + "H " + R, h + Step("H05", "H$b", "H$b"), true), Alt(core + "s" + R, h + Step("H13", "H$b", "s$b", "8.3.38"), true))
                                      else listOf(Alt(core + "H " + R, h + Step("H05", "H$b", "H$b")))
                else -> listOf(Alt(core + "H " + R, h + Step("H04", "H$b", "H$b", "8.3.36"), true), Alt(core + b + R, h + Step("H04", "H$b", "$b$b", "8.3.36"), true))
            }
        }
        if (Ph.isVowel(b)) {
            if (sru && preV == 'a') {
                return if (b == 'a') listOf(Alt(coreNoV + "o'" + R.drop(1), s0 + listOf(Step("H06", "ru", "u"), Step("V02", "a+u", "o"), Step("V07", "o+a", "o'"))))
                else listOf(Alt("$core $R", s0 + listOf(Step("H08", "ru", "y"), Step("V09", "y", ""))))
            }
            if (sru && preV == 'A') return listOf(Alt("$core $R", s0 + listOf(Step("H08", "ru", "y"), Step("V09", "y", ""))))
            return listOf(Alt(core + "r" + R, s0 + Step("H12", "ru", "r")))
        }
        if (Ph.isVoicedCons(b)) {
            if (b == 'r' && !(sru && preV == 'a')) {
                // ḍhralope pūrvasya dīrgho 'ṇaḥ (6.3.111) lengthens only aṇ (a i u); after other vowels r is simply dropped
                if (preV !in "aAiIuU") return listOf(Alt(core + " " + R, s0 + Step("H10", "r r", "$preV r")))
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

    private val ADJ_VOWELS = Regex("[aAiIuUfFxXeEoO][aAiIuUfFxXeEoO]")

    fun split(input: String): SplitResult = splitDepth(input, 0)

    private fun splitDepth(input: String, depth: Int): SplitResult {
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
        // junctions where no Sandhi rule changes anything (e.g. saMhitA + pAWaH): accepted only when both parts are lexically supported
        if (hasLexicon) for (k in 2..n - 2) {
            val L = J.substring(0, k); val R = J.substring(k)
            if (spaceAt >= 0 && spaceAt != k) continue
            val nasalJunction = L.last() in "nNRYm" && Ph.isVowel(R.first()) && (L.last() != 'm' || support.classify(L).ending.isNotEmpty() || lexicon?.isAttested(L) == true || lexicon?.category(L) == "i")
            if ((!Ph.isVowel(L.last()) && !nasalJunction) || R.length < (if (lexicon?.closedClass(R) != null) 3 else 4) || (L.length < 3 && lexicon?.closedClass(L) != "upasarga")) continue
            if (L.none { Ph.isVowel(it) } || R.none { Ph.isVowel(it) }) continue
            val ls = support.classify(L); val rs = support.classify(R)
            val prefixLeft = ls.detail == "upasarga"
            // a short particle of the dictionary (अह, उ, ह) is not split off a consonant-initial word (no अह + रहः)
            if (L.length <= 3 && Ph.isCons(R.first()) && lexicon?.category(L) == "i" && lexicon.closedClass(L) == null && !lexicon.isAttested(L)) continue
            if (ls.level == Support.NONE || rs.level == Support.NONE || ls.level == Support.VERBLIKE || (rs.level == Support.VERBLIKE && !prefixLeft)) continue
            val alts = try { joinPair(L, R, Ctx(), mutableListOf()) } catch (e: Exception) { emptyList() }
            if (alts.any { it.steps.isEmpty() && it.surface.replace(" ", "") == J }) found.putIfAbsent("$L|$R|true", Split(L, R, Alt(L + R, emptyList()), emptyList(), Status.PROVISIONAL, ls, rs, true))
        }
        // a form that is itself a dictionary word is not split at junctions where nothing changes (kamala is not ka + mala)
        if (lexicon?.isHeadword(J) == true && lexicon.isSimple(J)) found.entries.removeAll { it.value.alt.steps.isEmpty() }
        // a simple dictionary word (not a compound in the dictionary) is not split, except into the morpheme pairs taught in the lessons
        val jInfo = support.classify(J)
        val jStem = support.stemOf(J)
        if (spaceAt < 0 && lexicon?.isSimple(jStem) == true)
            found.entries.removeAll { (_, sp) ->
                val taught = (lexicon.isAttested(sp.left) || lexicon.closedClass(sp.left) != null) && (lexicon.isAttested(sp.right) || lexicon.closedClass(sp.right) != null)
                val prefixed = lexicon.closedClass(sp.left) == "upasarga"
                val krt = !sp.leftPada   // a root's guṇa/vṛddhi form before a suffix (नै + अकः, भो + अनम्)
                !taught && !prefixed && !krt
            }
        for ((k, v) in SPECIAL) for (res in v) if (res.first == J) {
            val ls = support.classify(k.first); val rs = support.classify(k.second)
            found.putIfAbsent("${k.first}|${k.second}|true", Split(k.first, k.second, Alt(J, listOf(Step("V10", "", "", res.second))), listOf("V10"), Status.PROVISIONAL, ls, rs, true))
        }
        // prune trivial candidates (very short invented parts)
        val ok = mutableListOf<Split>(); val unsupported = mutableListOf<Split>()
        val seen = HashSet<String>()
        for (sp in found.values) {
            if (!seen.add(sp.left + "|" + sp.right)) continue
            // a form typed without a space: the only readings written apart are those after ः, ो or the lengthened vowel of रो रि (रामो गच्छति)
            val gap = sp.alt.surface.indexOf(' ')
            if (spaceAt < 0 && gap > 0 && !((sp.alt.surface[gap - 1] in "oH" || "H10" in sp.ruleIds) && gap + 1 < sp.alt.surface.length && Ph.isCons(sp.alt.surface[gap + 1]))) continue
            // a prefix (अति, प्र) is never the last word
            if (lexicon?.closedClass(sp.right) == "upasarga" && !lexicon.isAttested(sp.right)) continue
            // a part is a word: never the whole form, never two vowels in a row, never a bare verbal root (वद्, उपे)
            if (sp.left == J || sp.right == J) continue
            if (ADJ_VOWELS.containsMatchIn(sp.left) || ADJ_VOWELS.containsMatchIn(sp.right)) continue
            if (listOf(sp.left, sp.right).any { p -> lexicon?.category(p) == "r" && lexicon.closedClass(p) == null && !lexicon.isAttested(p) }) continue
            // every part must contain a consonant unless it is a listed particle/prefix (no "वृक्षच्छाया + अ")
            if (sp.right.none { Ph.isCons(it) && it != 'H' && it != 'M' }) continue
            if (sp.left.none { Ph.isCons(it) && it != 'H' && it != 'M' } && lexicon?.closedClass(sp.left) == null && lexicon?.isAttested(sp.left) != true) continue
            // the avagraha only arises from the join itself; it never belongs inside a part
            if (sp.left.contains('\'') || sp.right.contains('\'')) continue
            // one-syllable dictionary fragments (श, ग, प…) are not offered as parts unless they are listed particles/prefixes or attested in the lessons
            if (listOf(sp.left, sp.right).any { p -> p.length <= 2 && lexicon?.closedClass(p) == null && lexicon?.isAttested(p) != true && !(p.last() in "eEoO" && lexicon?.isHeadword(p) == true) }) continue
            val ls = sp.leftSupport.level; val rs = sp.rightSupport.level
            val bothKnown = ls != Support.NONE && rs != Support.NONE
            if (!hasLexicon) { unsupported += sp; continue }
            if (bothKnown) {
                val allVerifiedRules = sp.ruleIds.all { Rules.find(it)?.verified == true }
                val nominal = { x: Support -> x == Support.EXACT || x == Support.DERIVED }
                val st = if (nominal(ls) && nominal(rs) && allVerifiedRules) Status.VERIFIED else Status.PROVISIONAL
                ok += sp.copy(status = st)
            } else unsupported += sp
        }
        // a compound listed in the dictionary decides its own boundary: an intact second member (मनो—हर, संहिता—पाठ) names the right part;
        // otherwise the vowels fused at the boundary and the left part is the headword the compound is listed under (हिम + आलयः)
        val cinfo = lexicon?.compound(jStem) ?: lexicon?.compound(J)
        if (cinfo != null && spaceAt < 0) {
            val second = cinfo.second
            // second member intact → it is the right part; no intact member → the vowels fused, so the reading must contain a vowel sandhi
            val pick = if (second.isNotEmpty()) ok.filter { support.stemOf(it.right) == second || it.right == second }
                       else ok.filter { s -> s.alt.steps.any { Rules.find(it.ruleId)?.category == Category.VOWEL } }
            // prefer a real word on the left over a compound-only form (मनः, not मनो)
            val real = pick.filter { !(lexicon?.isHeadword(it.left) == true && lexicon.weight(it.left) == 0) }
            val chosen = if (real.isNotEmpty()) real else pick
            if (chosen.isNotEmpty()) ok.retainAll { it in chosen }
        }
        // the visarga word is cited, not its Sandhi spelling (तेजः + मयः, never तेजो + मयः)
        ok.removeAll { sp -> sp.left.endsWith("o") && ok.any { o -> o.right == sp.right && o.left == sp.left.dropLast(1) + "aH" } }
        // a compound-only stem (तेजो, आत्म) is not offered when the same right part follows a real word (तेजः + मयः)
        ok.removeAll { sp -> lexicon?.isHeadword(sp.left) == true && lexicon.weight(sp.left) == 0 && lexicon.closedClass(sp.left) == null &&
            ok.any { o -> o !== sp && o.right == sp.right && (support.weight(o.left) > 0 || lexicon.closedClass(o.left) != null) } }
        // Occam: when a verified split without any Sandhi change exists, sandhi splits resting on stem+ending guesses are not offered
        val plain = ok.filter { it.alt.steps.isEmpty() && it.status == Status.VERIFIED }
        if (cinfo == null && plain.isNotEmpty()) ok.removeAll { sp -> sp.alt.steps.isNotEmpty() && Ph.isVowel(sp.right.first()) && !sp.leftSupport.detail.let { d -> d == "upasarga" || d == "avyaya" } &&
            plain.any { n -> n.right == sp.right.drop(1) && support.weight(sp.left) + support.weight(sp.right) < support.weight(n.left) + support.weight(n.right) } }
        // splits that differ only in the length of the joining vowel (आत्म/आत्मा + अवलोकनम्, देव/देवा + आलयः) are one
        // analysis: keep the reading whose words are better attested (compound-only stems such as आत्म weigh 0)
        fun shortEnd(w: String) = if (w.isNotEmpty() && w.last() in "AIUF") w.dropLast(1) + "aiuf"["AIUF".indexOf(w.last())] else w
        fun shortStart(w: String) = if (w.isNotEmpty() && w.first() in "AIUF") "aiuf"["AIUF".indexOf(w.first())] + w.drop(1) else w
        val groups = ok.groupBy { shortEnd(it.left) + "|" + shortStart(it.right) + "|" + it.ruleIds.joinToString(",") }
        val keep = groups.values.flatMap { g ->
            if (g.size == 1) g else {
                val w = g.associateWith { support.weight(it.left) + support.weight(it.right) + (if (lexicon?.closedClass(it.left) == "avyaya") 2 else 0) }; val m = w.values.max()
                // tie: the short-vowel stem on the left (देव, हिम) and the long-vowel word on the right (आलयः)
                listOf(g.filter { w.getValue(it) == m }.minBy { (if (it.left.last() in "AIUF") 1 else 0) + (if (it.right.first() in "aiuf") 1 else 0) })
            }
        }.toSet()
        ok.retainAll { it in keep }
        // the same word with alternative final consonants (सत्/सद्/सच्/सज् + चित्, वाक्/वाग् + ईशः) is one analysis:
        // keep the best-attested form, and on a tie the citation form (unvoiced, dental before palatal/retroflex)
        val pref = "tkpwcTKPWCdgbqjDGBQJ"
        fun consKey(w: String) = when {
            w.isNotEmpty() && w.last() in "rsH" -> w.dropLast(1) + "%"          // अन्तर्/अन्तस्/अन्तः are one word: cited with visarga
            w.isNotEmpty() && Ph.varga(w.last()) >= 0 && w.last() !in "NYRnm" -> w.dropLast(1) + "#"
            else -> w
        }
        val cgroups = ok.groupBy { consKey(it.left) + "|" + it.right }
        val ckeep = cgroups.values.flatMap { g ->
            if (g.size == 1) g else {
                if (g.all { it.left.last() in "rsH" }) listOf(g.minBy { "Hsr".indexOf(it.left.last()) }) else {
                val attested = g.filter { lexicon?.isAttested(it.left) == true }
                val cited = if (attested.isNotEmpty()) attested else g.filter { lexicon?.isHeadword(it.left) == true || lexicon?.closedClass(it.left) != null }
                val pool = if (cited.isNotEmpty()) cited else g
                val w = pool.associateWith { support.weight(it.left) + support.weight(it.right) }; val m = w.values.max()
                val top = pool.filter { w.getValue(it) == m }
                listOf((if (cited.isNotEmpty()) pool else top).minBy { pref.indexOf(it.left.last()).let { i -> if (i < 0) 99 else i } }) }
            }
        }.toSet()
        ok.retainAll { it in ckeep }
        fun score(s: Split) = (if (s.status == Status.VERIFIED) 100 else 0) - (if (spaceAt < 0 && s.leftSupport.ending in LexSupport.OBLIQUE_ENDINGS) 30 else 0) + 4 * (support.weight(s.left) + support.weight(s.right)) - s.alt.steps.size + s.leftSupport.level.ordinal.let { 3 - it } * 5 + s.rightSupport.level.ordinal.let { 3 - it } * 5 + minOf(s.left.length, s.right.length).coerceAtMost(6) + (if (s.alt.steps.isEmpty() && Ph.isVowel(s.left.last())) 8 else 0) + (if (s.leftSupport.detail == "upasarga") 15 else 0) + (if ("V10" in s.ruleIds) 40 else 0) - (if (s.alt.steps.isEmpty() && s.left.last() == 'm' && lexicon?.category(s.left) == "i") 12 else 0)
        ok.sortByDescending { score(it) }
        unsupported.sortByDescending { score(it) }
        // further readings are offered only when every word in them is a well-attested dictionary word (no fragments, no rare or compound-only forms)
        if (ok.size > 1) {
            val best = ok.first()
            val others = ok.drop(1).filter { s ->
                s.status == Status.VERIFIED && listOf(s.left, s.right).all { p -> support.weight(p) >= 4 && !(lexicon?.isHeadword(p) == true && lexicon.weight(p) == 0) } &&
                    s.leftSupport.ending !in LexSupport.OBLIQUE_ENDINGS
            }
            ok.clear(); ok += best; ok += others
        }
        // a part that is not itself a dictionary word but is a further Sandhi form is analysed again (आत्मेति → आत्मा + इति)
        if (ok.isNotEmpty() && depth < 2) {
            val best = ok.first()
            val newParts = mutableListOf<String>(); var steps = best.alt.steps; val ids = best.ruleIds.toMutableList(); var expanded = false
            for (p in best.parts) {
                val isWord = lexicon?.isHeadword(support.stemOf(p)) == true || lexicon?.closedClass(p) != null || lexicon?.isAttested(p) == true
                val sub = if (isWord || p.length < 5) null else splitDepth(Script.slp1ToDevanagari(p), depth + 1).analyses.firstOrNull()
                if (sub != null && sub.alt.steps.isNotEmpty() && sub.status == Status.VERIFIED && sub.parts.none { it == p }) { newParts += sub.parts; steps = steps + sub.alt.steps; ids += sub.ruleIds; expanded = true } else newParts += p
            }
            if (expanded) ok[0] = best.copy(alt = best.alt.copy(steps = steps), ruleIds = ids.distinct(), parts = newParts)
        }
        // a form that joins three or more words: one side of a junction is a known word and the other side is itself a Sandhi form
        // (आत्मेत्यवधारणम् → आत्मा + इति + अवधारणम्, आत्मेत्यभिमानम् → आत्मा + इति + अभिमानम्). A fully verified multi-word
        // reading is preferred to any two-word reading that rests on a guess.
        if (depth < 2 && spaceAt < 0 && ok.none { it.status == Status.VERIFIED }) {
            fun known(x: SupportInfo, w: String) = (x.level == Support.EXACT || x.level == Support.DERIVED) && support.weight(w) >= 2 && w.any { Ph.isCons(it) && it != 'H' && it != 'M' }
            val cands = (unsupported + ok).filter { c ->
                (known(c.leftSupport, c.left) && c.right.length >= 4 && !known(c.rightSupport, c.right)) ||
                (known(c.rightSupport, c.right) && c.left.length >= 4 && !known(c.leftSupport, c.left))
            }.distinctBy { it.left + "|" + it.right }
                .sortedByDescending { c -> if (known(c.leftSupport, c.left)) support.weight(c.left) else support.weight(c.right) }.take(6)
            var multi: Split? = null
            for (c in cands) {
                val leftKnown = known(c.leftSupport, c.left)
                val sub = splitDepth(Script.slp1ToDevanagari(if (leftKnown) c.right else c.left), depth + 1).analyses.firstOrNull() ?: continue
                if (sub.status != Status.VERIFIED || sub.alt.steps.isEmpty()) continue
                val ids = (sub.ruleIds + c.ruleIds).distinct()
                val st = if (c.ruleIds.all { Rules.find(it)?.verified == true }) Status.VERIFIED else Status.PROVISIONAL
                multi = if (leftKnown) Split(c.left, c.right, Alt(J, c.alt.steps + sub.alt.steps), ids, st, c.leftSupport, sub.rightSupport, c.leftPada, listOf(c.left) + sub.parts)
                        else Split(c.left, c.right, Alt(J, sub.alt.steps + c.alt.steps), ids, st, sub.leftSupport, c.rightSupport, c.leftPada, sub.parts + listOf(c.right))
                if (st == Status.VERIFIED) break
            }
            if (multi != null) { ok.removeAll { it.status != Status.VERIFIED }; ok.add(0, multi) }
        }
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
