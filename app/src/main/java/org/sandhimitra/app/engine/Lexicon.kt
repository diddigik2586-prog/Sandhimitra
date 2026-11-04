package org.sandhimitra.app.engine

/** Read-only access to the bundled lexical data (all keys are SLP1). */
interface LexiconApi {
    fun isHeadword(k1: String): Boolean
    fun isAttested(form: String): Boolean
    fun closedClass(form: String): String?
}

enum class Support { EXACT, DERIVED, VERBLIKE, NONE }

data class SupportInfo(val level: Support, val detail: String = "")

/**
 * Decides how well a surface form is supported by the lexicon:
 *  EXACT    – a headword of the bundled dictionary, a closed-class item or a form attested in the supplied documents;
 *  DERIVED  – obtained from a headword by one of the listed nominal endings (the full inflection tables are not bundled);
 *  VERBLIKE – ends like a finite verb; the root is not checked (never counted as verified);
 *  NONE     – not found.
 */
class LexSupport(private val lex: LexiconApi?) {
    private val nominalEndings: List<Pair<String, String>> = listOf(
        "aH" to "a", "am" to "a", "ena" to "a", "Aya" to "a", "At" to "a", "asya" to "a", "e" to "a", "O" to "a", "AH" to "a",
        "An" to "a", "EH" to "a", "ebyaH" to "a", "ezu" to "a", "AnAm" to "a", "ayoH" to "a", "Ani" to "a", "ena" to "a",
        "Am" to "A", "ayA" to "A", "AyE" to "A", "AyAH" to "A", "AyAm" to "A", "AH" to "A", "Asu" to "A", "AByaH" to "A",
        "iH" to "i", "im" to "i", "inA" to "i", "aye" to "i", "eH" to "i", "ayaH" to "i", "In" to "i", "ibyaH" to "i", "izu" to "i", "InAm" to "i", "O" to "i",
        "uH" to "u", "um" to "u", "unA" to "u", "ave" to "u", "oH" to "u", "avaH" to "u", "Un" to "u", "uzu" to "u", "UnAm" to "u", "O" to "u",
        "Im" to "I", "yAH" to "I", "yAm" to "I", "yE" to "I", "yaH" to "I", "ByaH" to "I",
        "Um" to "U", "vAH" to "U", "vAm" to "U",
        "A" to "f", "Aram" to "f", "AraH" to "f", "rA" to "f", "re" to "f", "uH" to "f",
        "A" to "an", "AnaH" to "an", "AnaM" to "an", "I" to "in", "inaH" to "in", "inam" to "in", "I" to "in",
        "aH" to "as", "asA" to "as", "aso" to "as", "asi" to "as", "AMsi" to "as", "aSAn" to "as",
        "An" to "at", "ataH" to "at", "atA" to "at", "at" to "at", "an" to "an"
    )
    private val verbEndings = listOf("anti", "ati", "ate", "ante", "asi", "ase", "ami", "Ami", "Ayati", "yati", "ti", "te", "nti", "si", "mi", "maH", "ma", "Ati", "etu", "atu", "at", "Ata", "ayet", "et")

    fun classify(word0: String): SupportInfo {
        val word = word0.replace("'", "").trim()
        if (word.isEmpty() || lex == null) return SupportInfo(Support.NONE)
        if (lex.isHeadword(word)) return SupportInfo(Support.EXACT, "headword")
        lex.closedClass(word)?.let { return SupportInfo(Support.EXACT, it) }
        if (lex.isAttested(word)) return SupportInfo(Support.EXACT, "attested")
        // a trailing visarga/anusvāra variant of an avyaya or stem
        if (word.endsWith("M") && lex.isHeadword(word.dropLast(1) + "m")) return SupportInfo(Support.EXACT, "headword")
        for ((suf, stemEnd) in nominalEndings) {
            if (word.length > suf.length && word.endsWith(suf)) {
                val stem = word.dropLast(suf.length) + stemEnd
                if (stem.length >= 2 && lex.isHeadword(stem)) return SupportInfo(Support.DERIVED, stem)
            }
        }
        for (v in verbEndings) if (word.length >= v.length + 2 && word.endsWith(v)) return SupportInfo(Support.VERBLIKE, v)
        return SupportInfo(Support.NONE)
    }
}
