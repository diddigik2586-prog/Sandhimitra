package org.sandhimitra.app.engine

enum class Category { VOWEL, CONSONANT, VISARGA, PRAKRITIBHAVA }
enum class Status { VERIFIED, PROVISIONAL, NO_RESULT }

/**
 * One entry of the shared rule registry. Forward processing (SandhiEngine.join) and reverse analysis
 * (SandhiEngine.split) both refer to rules by [id]; reverse candidates are only accepted when the
 * forward engine reproduces the input, so both directions share a single implementation.
 *
 * [sutraDeva] and [name] are Devanagari; [ref] uses Pāṇinian numbering as printed in the supplied
 * Sanskrit source document. [verified] means: sūtra wording and number appear in the supplied source
 * (topic [topic]) and the rule has positive and negative unit tests; subset-implemented rules are marked false.
 */
data class RuleInfo(
    val id: String, val category: Category, val name: String, val sutraDeva: String, val ref: String,
    val topic: String, val verified: Boolean, val scope: String
)

object Rules {
    val all: List<RuleInfo> = listOf(
        RuleInfo("V01", Category.VOWEL, "सवर्णदीर्घः", "अकः सवर्णे दीर्घः", "6.1.101", "t08", true, "a/ā i/ī u/ū ṛ/ṝ/ḷ with savarṇa vowel; ṛ-ḷ vārttika option"),
        RuleInfo("V02", Category.VOWEL, "गुणः", "आद्गुणः", "6.1.87", "t09", true, "a/ā + i u ṛ ḷ → e o ar al (with 1.1.51)"),
        RuleInfo("V03", Category.VOWEL, "वृद्धिः", "वृद्धिरेचि", "6.1.88", "t10", true, "a/ā + e o ai au → ai au"),
        RuleInfo("V04", Category.VOWEL, "एत्येधत्यूठ्सु / उपसर्गादृति धातौ", "उपसर्गादृति धातौ", "6.1.91", "t10", true, "upasarga + ṛ-initial root → ār; i/edh roots → vṛddhi"),
        RuleInfo("V05", Category.VOWEL, "यण्", "इको यणचि", "6.1.77", "t11", true, "i u ṛ ḷ + dissimilar vowel → y v r l"),
        RuleInfo("V06", Category.VOWEL, "अयादिः", "एचोऽयवायावः", "6.1.78", "t12", true, "e o ai au + vowel → ay av āy āv"),
        RuleInfo("V07", Category.VOWEL, "पूर्वरूपम्", "एङः पदान्तादति", "6.1.109", "t13", true, "word-final e/o + a → e/o + avagraha; go options 6.1.122-123"),
        RuleInfo("V08", Category.VOWEL, "पररूपम्", "एङि पररूपम्", "6.1.94", "t14", true, "upasarga ending in a/ā + e/o-initial root"),
        RuleInfo("V09", Category.VOWEL, "शाकल्यलोपः", "लोपः शाकल्यस्य", "8.3.19", "t39", true, "optional loss of y/v after ayādi (and of y after visarga)"),
        RuleInfo("C01", Category.CONSONANT, "पदान्तजश्त्वम्", "झलां जशोऽन्ते", "8.2.39", "t19", true, "word-final stops → unaspirated voiced stop"),
        RuleInfo("C02", Category.CONSONANT, "श्चुत्वम्", "स्तोः श्चुना श्चुः", "8.4.40", "t17", true, "s/t-varga ↔ ś/c-varga; blocked by 8.4.44"),
        RuleInfo("C03", Category.CONSONANT, "ष्टुत्वम्", "ष्टुना ष्टुः", "8.4.41", "t18", true, "s/t-varga ↔ ṣ/ṭ-varga; blocked by 8.4.42-43"),
        RuleInfo("C04", Category.CONSONANT, "चर्त्वम्", "खरि च", "8.4.55", "t20", true, "jhal → car before khar"),
        RuleInfo("C05", Category.CONSONANT, "अनुनासिकः", "यरोऽनुनासिकेऽनुनासिको वा", "8.4.45", "t21", true, "stop → nasal of its class before a nasal (optional)"),
        RuleInfo("C06", Category.CONSONANT, "अनुस्वारः", "मोऽनुस्वारः", "8.3.23", "t22", true, "word-final m + consonant → anusvāra"),
        RuleInfo("C07", Category.CONSONANT, "परसवर्णः", "अनुस्वारस्य ययि परसवर्णः", "8.4.58", "t23", true, "anusvāra before yay → homorganic nasal; optional at word end (8.4.59)"),
        RuleInfo("C08", Category.CONSONANT, "तुगागमः", "छे च", "6.1.73", "t24", true, "tuk before ch after short vowel; optional after long vowel (6.1.76)"),
        RuleInfo("C09", Category.CONSONANT, "ङमुडागमः", "ङमो ह्रस्वादचि ङमुण्नित्यम्", "8.3.32", "t25", true, "final ṅ ṇ n after a short vowel before a vowel are doubled"),
        RuleInfo("C10", Category.CONSONANT, "छत्वम्", "शश्छोऽटि", "8.4.63", "t26", true, "ś → ch after jhay before aṭ (optional)"),
        RuleInfo("C11", Category.CONSONANT, "पूर्वसवर्णः", "झयो होऽन्यतरस्याम्", "8.4.62", "t27", true, "h → savarṇa of preceding jhay (optional); ud + sthā/stambh (8.4.61)"),
        RuleInfo("C12", Category.CONSONANT, "नश्छव्यप्रशान्", "नश्छव्यप्रशान्", "8.3.7", "t28", true, "n + c ch ṭ ṭh t th → anusvāra + s/ś/ṣ; not in praśān"),
        RuleInfo("C13", Category.CONSONANT, "तोर्लि", "तोर्लि", "8.4.60", "t23", true, "dental + l → l (n → nasalised l)"),
        RuleInfo("H01", Category.VISARGA, "रुत्वम्", "ससजुषो रुः", "8.2.66", "t30", true, "word-final s → ru"),
        RuleInfo("H02", Category.VISARGA, "विसर्गः", "खरवसानयोर्विसर्जनीयः", "8.3.15", "t30", true, "ru → visarga before khar"),
        RuleInfo("H03", Category.VISARGA, "सत्वम्", "विसर्जनीयस्य सः", "8.3.34", "t34", true, "visarga → s before c ṭ t (then ś ṣ s by ścutva/ṣṭutva)"),
        RuleInfo("H04", Category.VISARGA, "शरि विकल्पः", "वा शरि", "8.3.36", "t34", true, "optional before ś ṣ s; retained before śar + khar (8.3.35)"),
        RuleInfo("H05", Category.VISARGA, "जिह्वामूलीयोपध्मानीयौ", "कुप्वोः ≍क ≍पौ च", "8.3.37", "t35", true, "visarga retained (jihvāmūlīya/upadhmānīya) before k kh p ph"),
        RuleInfo("H06", Category.VISARGA, "उत्वम्", "अतो रोरप्लुतादप्लुते", "6.1.113", "t32", true, "aḥ + a → o + avagraha"),
        RuleInfo("H07", Category.VISARGA, "हशि उत्वम्", "हशि च", "6.1.114", "t32", true, "aḥ + voiced consonant → o"),
        RuleInfo("H08", Category.VISARGA, "यत्वम्", "भोभगोअघोअपूर्वस्य योऽशि", "8.3.17", "t31", true, "ru after a/ā before aś → y (then 8.3.19/8.3.22)"),
        RuleInfo("H09", Category.VISARGA, "हलि लोपः", "हलि सर्वेषाम्", "8.3.22", "t31", true, "loss of y before consonants"),
        RuleInfo("H10", Category.VISARGA, "रोरि लोपः", "रो रि", "8.3.14", "t33", true, "r before r is deleted; preceding a i u lengthened (6.3.111)"),
        RuleInfo("H11", Category.VISARGA, "सुलोपः", "एतत्तदोः सुलोपोऽकोरनञ्समासे हलि", "6.1.132", "t31", true, "sa/eṣa lose visarga before a consonant"),
        RuleInfo("H12", Category.VISARGA, "रेफः", "ससजुषो रुः", "8.2.66", "t33", true, "ru remains r before vowels/voiced sounds after i u e o…"),
        RuleInfo("P01", Category.PRAKRITIBHAVA, "प्लुतप्रगृह्याः", "प्लुतप्रगृह्या अचि नित्यम्", "6.1.125", "t37", true, "pluta and pragṛhya vowels unchanged before a vowel"),
        RuleInfo("P02", Category.PRAKRITIBHAVA, "द्विवचनप्रगृह्यम्", "ईदूदेद्द्विवचनं प्रगृह्यम्", "1.1.11", "t37", true, "dual forms in ī ū e (declared by the user)"),
        RuleInfo("P03", Category.PRAKRITIBHAVA, "अदसो मात्", "अदसो मात्", "1.1.12", "t37", true, "amī, amū"),
        RuleInfo("P04", Category.PRAKRITIBHAVA, "निपातप्रगृह्यम्", "निपात एकाजनाङ्", "1.1.14", "t37", true, "small built-in list of single-vowel and o-ending particles"),
    )
    private val byId = all.associateBy { it.id }
    fun get(id: String): RuleInfo = byId.getValue(id)
    fun find(id: String?): RuleInfo? = if (id == null) null else byId[id]
}
