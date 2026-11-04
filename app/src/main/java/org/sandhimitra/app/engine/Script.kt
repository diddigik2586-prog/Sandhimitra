package org.sandhimitra.app.engine

/**
 * Transliteration between Devanagari, Kannada, IAST and SLP1 (one ASCII character per phoneme).
 * SLP1 is the engine's internal representation. The original user input is never destroyed:
 * callers keep the raw string and only convert a copy.
 */
object Script {
    private val devVowel = mapOf('अ' to 'a', 'आ' to 'A', 'इ' to 'i', 'ई' to 'I', 'उ' to 'u', 'ऊ' to 'U', 'ऋ' to 'f', 'ॠ' to 'F',
        'ऌ' to 'x', 'ॡ' to 'X', 'ए' to 'e', 'ऐ' to 'E', 'ओ' to 'o', 'औ' to 'O')
    private const val SL_CONS = "kKgGNcCjJYwWqQRtTdDnpPbBmyrlvSzshL"
    private const val DEV_CONS = "कखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसहळ"
    private const val KN_CONS = "ಕಖಗಘಙಚಛಜಝಞಟಠಡಢಣತಥದಧನಪಫಬಭಮಯರಲವಶಷಸಹಳ"
    private const val SL_VOW = "aAiIuUfFxXeEoO"
    private const val DEV_VOW = "अआइईउऊऋॠऌॡएऐओऔ"
    private const val KN_VOW = "ಅಆಇಈಉಊಋೠಌೡಏಐಓಔ"
    private val devDigits = "०१२३४५६७८९"
    private val knDigits = "೦೧೨೩೪೫೬೭೮೯"
    const val ANUSVARA_DEV = 'ं'
    const val VISARGA_DEV = 'ः'
    const val VIRAMA_DEV = '्'
    const val VIRAMA_KN = '್'

    private val devMatraStr = "ा ि ी ु ू ृ ॄ ॢ ॣ े ै ो ौ".split(" ").map { it[0] }
    private val knMatraStr = "ಾ ಿ ೀ ು ೂ ೃ ೄ ೢ ೣ ೇ ೈ ೋ ೌ".split(" ").map { it[0] }
    private val slMatraOrder = "AiIuUfFxXeEoO"

    /** Devanagari (or Kannada) text to SLP1. Unknown characters are passed through unchanged. */
    fun toSlp1(text: String): String = normalise(toSlp1Raw(text))

    /** Input variants: consonant ल + vowel sign ृ/ॄ ("लृ") is the usual way to type the vowels ऌ/ॡ; ':' is accepted for visarga. */
    private fun normalise(s: String) = s.replace("lF", "X").replace("lf", "x").replace("lX", "X").replace("lx", "x").replace(':', 'H')

    /** Live clean-up of typed text: ल/ಲ followed by the ṛ/ṝ/ḷ/ḹ vowel sign is replaced by the vowel letter ऌ/ॡ (ಌ/ೡ), which keyboards do not offer. */
    fun fixTyped(t: String): String = t
        .replace("ल\u0943", "ऌ").replace("ल\u0944", "ॡ").replace("ल\u0962", "ऌ").replace("ल\u0963", "ॡ")
        .replace("ल्\u090B", "ऌ").replace("ल्\u0960", "ॡ")
        .replace("ಲ\u0CC3", "ಌ").replace("ಲ\u0CC4", "ೡ").replace("ಲ\u0CE2", "ಌ").replace("ಲ\u0CE3", "ೡ")

    private fun toSlp1Raw(text: String): String {
        val dev = text.any { it in 'ऀ'..'ॿ' }
        val kn = text.any { it in 'ಀ'..'೿' }
        return when {
            dev -> brahmiToSlp1(text, DEV_CONS, DEV_VOW, devMatraStr, VIRAMA_DEV, 'ं', 'ः', 'ऽ', 'ँ', devDigits)
            kn -> brahmiToSlp1(text, KN_CONS, KN_VOW, knMatraStr, VIRAMA_KN, 'ಂ', 'ಃ', 'ಽ', 'ಁ', knDigits)
            else -> iastToSlp1(text)
        }
    }

    private fun brahmiToSlp1(s: String, cons: String, vow: String, matras: List<Char>, vir: Char,
                             anu: Char, vis: Char, avag: Char, cand: Char, digits: String): String {
        val sb = StringBuilder()
        var i = 0
        val vowelSl = listOf('a', 'A', 'i', 'I', 'u', 'U', 'f', 'F', 'x', 'X', 'e', 'E', 'o', 'O')
        while (i < s.length) {
            val ch = s[i]
            val ci = cons.indexOf(ch)
            when {
                ci >= 0 -> {
                    sb.append(SL_CONS[ci])
                    val n = if (i + 1 < s.length) s[i + 1] else '\u0000'
                    val mi = matras.indexOf(n)
                    when {
                        n == vir -> i++
                        mi >= 0 -> { sb.append(slMatraOrder[mi]); i++ }
                        else -> sb.append('a')
                    }
                }
                vow.indexOf(ch) >= 0 -> sb.append(vowelSl[vow.indexOf(ch)])
                ch == anu -> sb.append('M')
                ch == vis -> sb.append('H')
                ch == avag -> sb.append('\'')
                ch == cand -> sb.append('~')
                digits.indexOf(ch) >= 0 -> sb.append(digits.indexOf(ch).toString())
                ch == '‌' || ch == '‍' -> {}
                else -> sb.append(ch)
            }
            i++
        }
        return sb.toString()
    }

    private val iastMulti = linkedMapOf(
        "kh" to "K", "gh" to "G", "ch" to "C", "jh" to "J", "ṭh" to "W", "ḍh" to "Q", "th" to "T", "dh" to "D", "ph" to "P", "bh" to "B",
        "ai" to "E", "au" to "O", "ṝ" to "F", "ḹ" to "X", "ṛ" to "f", "ḷ" to "x", "ā" to "A", "ī" to "I", "ū" to "U",
        "ṅ" to "N", "ñ" to "Y", "ṭ" to "w", "ḍ" to "q", "ṇ" to "R", "ś" to "S", "ṣ" to "z", "ṃ" to "M", "ṁ" to "M", "ḥ" to "H", "’" to "'")

    private fun iastToSlp1(text: String): String {
        val s = java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFC)
            .replace("l\u0325\u0304", "ḹ").replace("l\u0325", "ḷ").replace("r\u0325\u0304", "ṝ").replace("r\u0325", "ṛ").replace("m\u0307", "ṃ")
        val sb = StringBuilder(); var i = 0
        while (i < s.length) {
            var done = false
            if (i + 1 < s.length) {
                val two = s.substring(i, i + 2)
                iastMulti[two]?.let { sb.append(it); i += 2; done = true }
            }
            if (!done) {
                val one = s[i].toString()
                val m = iastMulti[one]
                if (m != null) sb.append(m) else sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }

    // ---------------- SLP1 -> scripts
    private fun isVow(c: Char) = SL_VOW.indexOf(c) >= 0
    private fun isCons(c: Char) = SL_CONS.indexOf(c) >= 0

    fun slp1ToDevanagari(s: String): String = slp1ToBrahmi(s, DEV_CONS, DEV_VOW, devMatraStr, VIRAMA_DEV, 'ं', 'ः', 'ऽ', 'ँ', devDigits)
    fun slp1ToKannada(s: String): String = slp1ToBrahmi(s, KN_CONS, KN_VOW, knMatraStr, VIRAMA_KN, 'ಂ', 'ಃ', 'ಽ', 'ಁ', knDigits)

    private fun slp1ToBrahmi(s: String, cons: String, vow: String, matras: List<Char>, vir: Char, anu: Char, vis: Char,
                             avag: Char, cand: Char, digits: String): String {
        val sb = StringBuilder()
        val vowelSl = "aAiIuUfFxXeEoO"
        var prevCons = false
        for ((idx, c) in s.withIndex()) {
            if (isCons(c)) {
                if (prevCons) sb.append(vir)
                sb.append(cons[SL_CONS.indexOf(c)]); prevCons = true
                continue
            }
            if (isVow(c)) {
                if (prevCons) { if (c != 'a') sb.append(matras[slMatraOrder.indexOf(c)]); prevCons = false }
                else sb.append(vow[vowelSl.indexOf(c)])
                continue
            }
            if (prevCons && c != '~') { sb.append(vir); prevCons = false }
            when (c) {
                'M' -> sb.append(anu)
                'H' -> sb.append(vis)
                '\'' -> sb.append(avag)
                '~' -> { if (prevCons) { sb.append(vir); prevCons = false }; sb.append(cand) }
                in '0'..'9' -> sb.append(digits[c - '0'])
                else -> sb.append(c)
            }
        }
        if (prevCons) sb.append(vir)
        return sb.toString()
    }

    private val iastOut = mapOf('A' to "ā", 'I' to "ī", 'U' to "ū", 'f' to "ṛ", 'F' to "ṝ", 'x' to "ḷ", 'X' to "ḹ", 'E' to "ai", 'O' to "au",
        'K' to "kh", 'G' to "gh", 'N' to "ṅ", 'C' to "ch", 'J' to "jh", 'Y' to "ñ", 'w' to "ṭ", 'W' to "ṭh", 'q' to "ḍ", 'Q' to "ḍh", 'R' to "ṇ",
        'T' to "th", 'D' to "dh", 'P' to "ph", 'B' to "bh", 'S' to "ś", 'z' to "ṣ", 'M' to "ṃ", 'H' to "ḥ", '~' to "̃", 'L' to "ḷ")

    fun slp1ToIast(s: String): String {
        val sb = StringBuilder()
        for (c in s) sb.append(iastOut[c] ?: c.toString())
        return sb.toString()
    }

    /** Render any Sanskrit text (Devanagari/Kannada/IAST input) in the requested interface language: sa, kn or en. */
    fun render(text: String, lang: String): String = when (lang) {
        "kn" -> if (text.any { it in 'ऀ'..'ॿ' }) slp1ToKannada(toSlp1Safe(text)) else text
        "en" -> if (text.any { it in 'ऀ'..'ॿ' }) devanagariTextToIast(text) else text
        else -> text
    }

    private fun toSlp1Safe(t: String) = toSlp1(t)

    /** Transliterates only the Devanagari runs of a mixed string to IAST, leaving other characters. */
    fun devanagariTextToIast(text: String): String = convertRuns(text) { slp1ToIast(brahmiToSlp1(it, DEV_CONS, DEV_VOW, devMatraStr, VIRAMA_DEV, 'ं', 'ः', 'ऽ', 'ँ', devDigits)) }

    fun devanagariTextToKannada(text: String): String = convertRuns(text) { slp1ToKannada(toSlp1(it)) }

    private fun convertRuns(text: String, f: (String) -> String): String {
        val sb = StringBuilder(); val run = StringBuilder()
        fun isDev(c: Char) = c in 'ऀ'..'ॿ' && c != '।' && c != '॥'
        for (c in text) {
            if (isDev(c) || ((c == '‌' || c == '‍') && run.isNotEmpty())) run.append(c)
            else { if (run.isNotEmpty()) { sb.append(f(run.toString())); run.clear() }; sb.append(if (c == '।') '|' else if (c == '॥') '‖' else c) }
        }
        if (run.isNotEmpty()) sb.append(f(run.toString()))
        return sb.toString()
    }

    /** Allowed SLP1 phoneme characters for engine input. */
    const val SLP1_ALPHABET = "aAiIuUfFxXeEoOkKgGNcCjJYwWqQRtTdDnpPbBmyrlvSzshLMH'~3 +"
    fun invalidChars(slp1: String): String = slp1.filter { SLP1_ALPHABET.indexOf(it) < 0 }.toSet().joinToString("")
}
