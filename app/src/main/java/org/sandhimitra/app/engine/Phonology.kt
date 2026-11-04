package org.sandhimitra.app.engine

/** SLP1 phoneme classes used by the Sandhātrī engine. */
object Ph {
    const val VOWELS = "aAiIuUfFxXeEoO"
    const val SHORT = "aiufx"
    const val STOPS = "kKgGNcCjJYwWqQRtTdDnpPbBm"
    val VARGAS = listOf("kKgGN", "cCjJY", "wWqQR", "tTdDn", "pPbBm")
    const val NASALS_OF = "NYRnm"
    const val JHAL_STOPS = "kKgGcCjJwWqQtTdDpPbB"
    const val KHAR = "kKcCwWtTpPSzs"
    const val GHOSA_CONS = "gGNjJYqQRdDnbBmyrlvh"

    fun isVowel(c: Char) = VOWELS.indexOf(c) >= 0
    fun isCons(c: Char) = !isVowel(c) && c.isLetter()
    fun isShort(c: Char) = SHORT.indexOf(c) >= 0
    fun varga(c: Char) = VARGAS.indexOfFirst { it.indexOf(c) >= 0 }
    fun isJhalStop(c: Char) = JHAL_STOPS.indexOf(c) >= 0
    fun isKhar(c: Char) = KHAR.indexOf(c) >= 0
    fun isVoicedCons(c: Char) = GHOSA_CONS.indexOf(c) >= 0

    /** jaś substitute: the unaspirated voiced stop of the same varga. */
    fun jash(c: Char): Char = when (varga(c)) { 0 -> 'g'; 1 -> 'j'; 2 -> 'q'; 3 -> 'd'; 4 -> 'b'; else -> c }
    /** car substitute: the unaspirated unvoiced stop of the same varga. */
    fun car(c: Char): Char = when (varga(c)) { 0 -> 'k'; 1 -> 'c'; 2 -> 'w'; 3 -> 't'; 4 -> 'p'; else -> c }
    fun nasalOf(c: Char): Char = NASALS_OF[varga(c)]
    fun voicedAspirate(c: Char): Char = when (varga(c)) { 0 -> 'G'; 1 -> 'J'; 2 -> 'Q'; 3 -> 'D'; 4 -> 'B'; else -> c }

    fun longOf(c: Char): Char = when (c) { 'a' -> 'A'; 'i' -> 'I'; 'u' -> 'U'; 'f' -> 'F'; 'x' -> 'X'; else -> c }
    fun isDental(c: Char) = varga(c) == 3 && c != 'n' || c == 'n'
    fun isTVarga(c: Char) = varga(c) == 3
    fun isPalatalGroup(c: Char) = varga(c) == 1 || c == 'S'
    fun isRetroflexGroup(c: Char) = varga(c) == 2 || c == 'z'
    fun toPalatal(c: Char): Char = when (c) { 't' -> 'c'; 'T' -> 'C'; 'd' -> 'j'; 'D' -> 'J'; 'n' -> 'Y'; 's' -> 'S'; else -> c }
    fun toRetroflex(c: Char): Char = when (c) { 't' -> 'w'; 'T' -> 'W'; 'd' -> 'q'; 'D' -> 'Q'; 'n' -> 'R'; 's' -> 'z'; else -> c }

    val UPASARGAS = setOf("pra", "parA", "apa", "sam", "anu", "ava", "nis", "nir", "dus", "dur", "vi", "A", "ni", "aDi", "api", "ati", "su", "ud", "aBi", "prati", "pari", "upa")
}
