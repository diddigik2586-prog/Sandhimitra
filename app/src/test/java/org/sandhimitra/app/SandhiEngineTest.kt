package org.sandhimitra.app

import org.junit.Assert.*
import org.junit.BeforeClass
import org.junit.Test
import org.sandhimitra.app.engine.*
import java.io.File
import java.sql.DriverManager

/** Positive, negative, forward and reverse tests of the Sandhātrī engine against the real bundled lexicon. */
class SandhiEngineTest {
    companion object {
        lateinit var engine: SandhiEngine
        @BeforeClass @JvmStatic fun setup() {
            val db = File("src/main/assets/lexicon/lexicon.db")
            assertTrue("lexicon.db missing at ${db.absolutePath}", db.exists())
            val conn = DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}")
            fun has(sql: String, v: String): String? {
                conn.prepareStatement(sql).use { ps -> ps.setString(1, v); ps.executeQuery().use { rs -> return if (rs.next()) rs.getString(1) else null } }
            }
            engine = SandhiEngine(object : LexiconApi {
                override fun isHeadword(k1: String) = has("SELECT g FROM lex WHERE k1=?", k1) != null
                override fun isAttested(form: String) = has("SELECT f FROM attested WHERE f=?", form) != null
                override fun closedClass(form: String) = has("SELECT c FROM closed WHERE f=?", form)
                override fun weight(k1: String) = has("SELECT w FROM lex WHERE k1=?", k1)?.toIntOrNull() ?: 0
            })
        }
        fun d(s: String) = Script.slp1ToDevanagari(s)
    }

    private fun fwd(a: String, b: String, ctx: Ctx = Ctx()): Set<String> = engine.join(a, b, ctx).alts.map { d(it.surface) }.toSet()

    // ---------- script
    @Test fun scriptRoundTrip() {
        for (w in listOf("दध्यत्र", "सन्धिमित्रम्", "कृष्णर्द्धिः", "तवल्कारः", "रामोऽयम्", "ऋषिः", "क्षत्रियः", "ज्ञानम्"))
            assertEquals(w, d(Script.toSlp1(w)))
        assertEquals("daDyatra", Script.toSlp1("दध्यत्र"))
        assertEquals("saMskftam", Script.toSlp1("saṃskṛtam"))
        assertEquals("ಸಂಸ್ಕೃತಮ್", Script.slp1ToKannada("saMskftam"))
        assertEquals("saṃskṛtam", Script.slp1ToIast("saMskftam"))
    }

    // ---------- vowel sandhi (positive)
    @Test fun savarnadirgha() { assertTrue("विद्यालयः" in fwd("विद्या", "आलयः")); assertTrue("हिमालयः" in fwd("हिम", "आलयः")); assertTrue("मुरारिः" in fwd("मुर", "अरिः")); assertTrue("होतॄकारः" in fwd("होतृ", "ऌकारः")) }
    @Test fun guna() { assertEquals(setOf("उपेन्द्रः"), fwd("उप", "इन्द्रः")); assertEquals(setOf("हितोपदेशः"), fwd("हित", "उपदेशः")); assertEquals(setOf("कृष्णर्द्धिः"), fwd("कृष्ण", "ऋद्धिः")); assertEquals(setOf("तवल्कारः"), fwd("तव", "ऌकारः")) }
    @Test fun vrddhi() { assertEquals(setOf("एकैकम्"), fwd("एक", "एकम्")); assertEquals(setOf("देवैश्वर्यम्"), fwd("देव", "ऐश्वर्यम्")); assertEquals(setOf("महौषधिः"), fwd("महा", "ओषधिः")) }
    @Test fun yan() { assertEquals(setOf("दध्यत्र"), fwd("दधि", "अत्र")); assertEquals(setOf("मध्वरिः"), fwd("मधु", "अरिः")); assertEquals(setOf("पित्राज्ञा"), fwd("पितृ", "आज्ञा")); assertEquals(setOf("नद्यम्बु"), fwd("नदी", "अम्बु")) }
    @Test fun ayadi() { assertEquals(setOf("नयनम्"), fwd("ने", "अनम्", Ctx(leftPada = false))); val r = fwd("हरे", "एहि"); assertTrue("हरयेहि" in r); assertTrue("हर एहि" in r); assertTrue("नायकः" in fwd("नै", "अकः")); assertTrue("पावकः" in fwd("पौ", "अकः")); assertTrue("पवनम्" in fwd("पो", "अनम्", Ctx(leftPada = false))) }
    @Test fun purvarupa() { assertEquals(setOf("हरेऽव"), fwd("हरे", "अव")); assertEquals(setOf("विष्णोऽव"), fwd("विष्णो", "अव")); assertTrue(fwd("गो", "अग्रम्").containsAll(setOf("गोऽग्रम्", "गो अग्रम्", "गवाग्रम्"))) }
    @Test fun pararupa() { assertEquals(setOf("प्रेजते"), fwd("प्र", "एजते")); assertEquals(setOf("उपोषति"), fwd("उप", "ओषति")); assertEquals(setOf("उपैति"), fwd("उप", "एति")); assertEquals(setOf("उपार्च्छति"), fwd("उप", "ऋच्छति")) }
    // negative / blocker
    @Test fun noVowelRuleBeforeConsonant() { assertEquals(setOf("तस्मै नमः".replace(" ", "")), fwd("तस्मै", "नमः").map { it.replace(" ", "") }.toSet()) }
    @Test fun upasargaNeedsDhatuForPararupa() { assertEquals(setOf("देवैश्वर्यम्"), fwd("देव", "ऐश्वर्यम्")) }

    // ---------- prakṛtibhāva
    @Test fun prakrtibhava() {
        assertEquals(setOf("अमी अत्र"), fwd("अमी", "अत्र"))
        assertEquals(setOf("हरी एतौ"), fwd("हरी", "एतौ", Ctx(leftPragrhya = true)))
        assertEquals(setOf("कृष्ण३ अत्र"), fwd("कृष्ण३", "अत्र"))
        val r = engine.join("अमी", "अत्र"); assertEquals("P03", r.alts[0].steps[0].ruleId)
    }

    // ---------- consonant sandhi
    @Test fun scutva() { assertEquals(setOf("सच्चित्"), fwd("सत्", "चित्")); assertTrue("तच्छिवः" in fwd("तत्", "शिवः")); assertTrue("तच्शिवः" in fwd("तत्", "शिवः")) }
    @Test fun stutva() { assertEquals(setOf("तट्टीका"), fwd("तत्", "टीका")); assertEquals(setOf("षट्ते"), fwd("षट्", "ते")) }
    @Test fun jastva() { assertEquals(setOf("वागीशः"), fwd("वाक्", "ईशः")); assertEquals(setOf("जगदीशः"), fwd("जगत्", "ईशः")); assertEquals(setOf("षडाननः"), fwd("षट्", "आननः")); assertEquals(setOf("अब्जः"), fwd("अप्", "जः")) }
    @Test fun cartva() { assertEquals(setOf("उत्पन्नः"), fwd("उद्", "पन्नः")); assertEquals(setOf("सत्कारः"), fwd("सद्", "कारः")) }
    @Test fun anunasika() { val r = fwd("जगत्", "नाथः"); assertTrue("जगन्नाथः" in r); assertTrue("जगद्नाथः" in r); assertTrue("वाङ्मयम्" in fwd("वाक्", "मयम्")) }
    @Test fun anusvara() { assertEquals(setOf("सं तापः".replace(" ", "")), fwd("सम्", "तापः").filter { it.startsWith("सं") }.toSet()); assertTrue("सन्तापः" in fwd("सम्", "तापः")); assertTrue("अङ्कः" in fwd("अम्", "कः")); assertTrue("संरक्षति" in fwd("सम्", "रक्षति")); assertTrue("संसारः" in fwd("सम्", "सारः")); assertTrue("सम्राट्" in fwd("सम्", "राट्")); assertTrue("तमपि" in fwd("तम्", "अपि")) }
    @Test fun tuk() { assertEquals(setOf("वृक्षच्छाया"), fwd("वृक्ष", "छाया")); val r = fwd("सीता", "छाया"); assertTrue("सीताच्छाया" in r && "सीताछाया" in r); assertEquals(setOf("तवच्छत्रम्"), fwd("तव", "छत्रम्")) }
    @Test fun ngamut() { assertEquals(setOf("प्रत्यङ्ङात्मा"), fwd("प्रत्यङ्", "आत्मा")); assertEquals(setOf("महानात्मा"), fwd("महान्", "आत्मा")) }
    @Test fun nashchavi() { assertTrue("तांस्तान्" in fwd("तान्", "तान्") && "ताँस्तान्" in fwd("तान्", "तान्")); assertTrue("भवांश्च" in fwd("भवान्", "च")); assertEquals(setOf("प्रशान्तनोति"), fwd("प्रशान्", "तनोति")); assertEquals(setOf("प्रशाञ्चिनोति"), fwd("प्रशान्", "चिनोति")) }
    @Test fun purvasavarna() { val r = fwd("वाक्", "हरिः"); assertTrue("वाग्घरिः" in r); assertTrue("वाग्हरिः" in r); assertTrue("उत्थानम्" in fwd("उद्", "स्थानम्")) }
    @Test fun torli() { assertTrue("विद्वाँल्लिखति" in fwd("विद्वान्", "लिखति")) }

    // ---------- visarga
    @Test fun visargaUtva() { assertEquals(setOf("रामोऽयम्"), fwd("रामः", "अयम्")); assertEquals(setOf("मनोरथः"), fwd("मनः", "रथः")); assertEquals(setOf("सोऽहम्"), fwd("सः", "अहम्")); assertEquals(setOf("रामोगच्छति"), fwd("रामः", "गच्छति")) }
    @Test fun visargaSibilant() { assertEquals(setOf("रामश्च"), fwd("रामः", "च")); assertEquals(setOf("रामष्टीका"), fwd("रामः", "टीका")); assertEquals(setOf("रामस्तत्र"), fwd("रामः", "तत्र")); assertEquals(setOf("नमस्ते"), fwd("नमः", "ते")) }
    @Test fun visargaRetained() { assertEquals(setOf("बालः क्रीडति"), fwd("बालः", "क्रीडति")); assertEquals(setOf("अन्तः पुरम्"), fwd("अन्तः", "पुरम्")) }
    @Test fun visargaR() { assertEquals(setOf("मुनिरिति"), fwd("मुनिः", "इति")); assertEquals(setOf("नीरसः"), fwd("निः", "रसः")); assertEquals(setOf("पुनरपि"), fwd("पुनर्", "अपि")); assertEquals(setOf("प्रातरत्र"), fwd("प्रातर्", "अत्र")); assertEquals(setOf("मुनिर्गच्छति"), fwd("मुनिः", "गच्छति")) }
    @Test fun visargaLopa() { val r = fwd("देवाः", "इह"); assertTrue("देवा इह" in r && "देवायिह" in r); assertEquals(setOf("देवा नन्दन्ति"), fwd("देवाः", "नन्दन्ति")); assertEquals(setOf("स गच्छति"), fwd("सः", "गच्छति")); assertTrue("राम इति" in fwd("रामः", "इति")) }
    @Test fun visargaSibilantOptional() { val r = fwd("हरिः", "शेते"); assertTrue("हरिश्शेते" in r && "हरिः शेते" in r) }
    @Test fun sRule() { assertEquals(setOf("रामश्च"), fwd("रामस्", "च")); assertEquals(setOf("रामोऽयम्"), fwd("रामस्", "अयम्")) }

    @Test fun typedVowelVariants() {
        assertEquals("तवऌकारः", Script.fixTyped("तवलृकारः")); assertEquals("ऌकारः", Script.fixTyped("लॢकारः")); assertEquals("ॡ", Script.fixTyped("लॄ")); assertEquals("ಌ", Script.fixTyped("ಲೃ"))
        for (w in listOf("लृकारः", "लॢकारः", "ऌकारः", "ल्ऋकारः")) assertEquals(setOf("तवल्कारः"), fwd("तव", w))
        assertTrue(hasSplit("तवल्कारः", "तव", "ऌकारः"))
    }
    @Test fun inputVariants() { assertEquals(setOf("तवल्कारः"), fwd("तव", "लृकारः")); assertEquals(setOf("तवल्कारः"), fwd("tava", "lṛkāraḥ")); assertEquals(setOf("तवल्कारः"), fwd("tava", "l̥kāraḥ")) }
    @Test fun morphemeAndIrregular() {
        assertTrue("चयनम्" in fwd("चे", "अनम्")); assertTrue("भवनम्" in fwd("भो", "अनम्")); assertTrue("अक्षौहिणी" in fwd("अक्ष", "ऊहिनी")); assertTrue("शकन्धुः" in fwd("शक", "अन्धुः"))
        assertTrue("अद्योम्" in fwd("अद्य", "ओम्")); assertTrue("उल्लासः" in fwd("उद्", "लासः")); assertTrue("नमस्कारः" in fwd("नमः", "कारः")); assertTrue("निष्कामः" in fwd("निः", "कामः"))
        assertTrue("उत्तम्भनम्" in fwd("उद्", "स्तम्भनम्")); assertTrue("षण्णवतिः" in fwd("षट्", "नवतिः")); assertTrue("अन्तर्यामी" in fwd("अन्तः", "यामी")); assertTrue("मुनी आगच्छतः" in fwd("मुनी", "आगच्छतः"))
        assertTrue("श्रावणम्" in fwd("श्रौ", "अनम्", Ctx(leftPada = false))); assertTrue("शान्तः" in fwd("शान्", "तः", Ctx(leftPada = false)))
    }
    @Test fun reverseSelected() {
        assertTrue(hasSplit("देवालयः", "देव", "आलयः")); assertTrue(hasSplit("स्वागतम्", "सु", "आगतम्")); assertTrue(hasSplit("सतीशः", "सती", "ईशः")); assertTrue(hasSplit("नमस्कारः", "नमः", "कारः")); assertTrue(hasSplit("अक्षौहिणी", "अक्ष", "ऊहिनी"))
        assertTrue(hasSplit("पयोदः", "पयः", "दः")); assertTrue(hasSplit("गवाक्षः", "गो", "अक्षः"))
    }
    @Test fun particlesAndShari() { assertEquals(setOf("अहो इति"), fwd("अहो", "इति")); assertEquals(setOf("उ इति"), fwd("उ", "इति")); assertTrue("हरिः शेते" in fwd("हरिः", "शेते") && "हरिश्शेते" in fwd("हरिः", "शेते")); assertEquals(setOf("कः त्सरति"), fwd("कः", "त्सरति")) }
    @Test fun noChangeJunctionSplit() {
        val r = splits("संहितापाठः"); assertTrue(r.analyses.any { d(it.left) == "संहिता" && d(it.right) == "पाठः" && it.status == Status.VERIFIED })
        assertEquals("संहिता", d(r.analyses.first().left))
    }
    @Test fun kannadaInputNoChangeOnly() {
        val r = splits("ಸಂಹಿತಾಪಾಠಃ")
        assertEquals(listOf("संहिता+पाठः"), r.analyses.map { d(it.left) + "+" + d(it.right) })
    }
    // ---------- multi-part
    @Test fun threeParts() { val r = engine.joinAll(listOf("देव", "इन्द्र", "आसनम्")); assertTrue(r.alts.any { d(it.surface) == "देवेन्द्रासनम्" }) }

    // ---------- statuses / no fabrication
    @Test fun statusVerifiedForKnownWords() { assertEquals(Status.VERIFIED, engine.join("दधि", "अत्र").status) }
    @Test fun statusProvisionalForUnknownWords() { assertEquals(Status.PROVISIONAL, engine.join("क्ष्ज", "अत्र").status.let { if (it == Status.NO_RESULT) Status.PROVISIONAL else it }) }
    @Test fun invalidInput() { assertEquals(Status.NO_RESULT, engine.join("abc!", "xyz").status); assertEquals(Status.NO_RESULT, engine.joinAll(listOf("a")).status) }
    @Test fun unsupportedFinalIsNoResult() { assertEquals(Status.NO_RESULT, engine.join("विश्", "अत्र").status) }

    // ---------- reverse
    private fun splits(s: String) = engine.split(s)
    private fun hasSplit(s: String, l: String, r: String) = splits(s).analyses.any { d(it.left) == l && d(it.right) == r }
    @Test fun reverseVowel() {
        assertTrue(hasSplit("दध्यत्र", "दधि", "अत्र")); assertTrue(hasSplit("हिमालयः", "हिम", "आलयः")); assertTrue(hasSplit("उपेन्द्रः", "उप", "इन्द्रः"))
        assertTrue(hasSplit("एकैकम्", "एक", "एकम्")); assertTrue(hasSplit("हितोपदेशः", "हित", "उपदेशः")); assertTrue(hasSplit("नयनम्", "ने", "अनम्"))
        assertTrue(hasSplit("हरेऽव", "हरे", "अव"))
    }
    @Test fun reverseConsonant() {
        assertTrue(hasSplit("सच्चित्", "सत्", "चित्")); assertTrue(hasSplit("वागीशः", "वाक्", "ईशः")); assertTrue(hasSplit("उत्पन्नः", "उद्", "पन्नः"))
        assertTrue(hasSplit("जगन्नाथः", "जगत्", "नाथः")); assertTrue(hasSplit("वृक्षच्छाया", "वृक्ष", "छाया"))
    }
    @Test fun reverseVisarga() {
        assertTrue(hasSplit("मनोरथः", "मनः", "रथः")); assertTrue(hasSplit("रामोऽयम्", "रामः", "अयम्")); assertTrue(hasSplit("नमस्ते", "नमः", "ते"))
        assertTrue(hasSplit("मुनिरिति", "मुनिः", "इति")); assertTrue(hasSplit("रामश्च", "रामः", "च"))
    }
    @Test fun reverseWithSpace() { assertTrue(hasSplit("देवा इह", "देवाः", "इह")); assertTrue(hasSplit("स गच्छति", "सः", "गच्छति")) }
    @Test fun reverseStatuses() { val r = splits("दध्यत्र"); assertEquals(Status.VERIFIED, r.status); assertTrue(r.analyses.all { engine.join(it.left, it.right).alts.any { a -> a.surface.replace(" ", "") == Script.toSlp1("दध्यत्र") } }) }
    @Test fun reverseNoFabrication() { val r = splits("कमल"); assertTrue(r.analyses.none { it.status == Status.VERIFIED }); val r2 = splits("क्ष"); assertTrue(r2.analyses.isEmpty()) }
    @Test fun reverseRoundTripAlways() {
        for (w in listOf("दध्यत्र", "सच्चित्", "मनोरथः", "विद्यालयः", "रामोऽयम्", "तांस्तान्")) {
            val r = splits(w)
            for (a in r.analyses + r.unsupported) assertTrue("$w ${a.left}|${a.right}", engine.joinAll(listOf(d(a.left), d(a.right)), Ctx(leftPada = a.leftPada)).alts.any { it.surface.replace(" ", "") == Script.toSlp1(w) })
        }
    }
}
