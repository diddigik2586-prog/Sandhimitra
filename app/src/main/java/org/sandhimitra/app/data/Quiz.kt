package org.sandhimitra.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * One playable Sandhicakram question. Every record in assets/quiz/questions.json was produced by the
 * bank builder (test tool QuizBankBuild) from an example or exercise row of the supplied documents and
 * verified by the Sandhātrī engine: each accepted answer is reproduced by the forward engine, and every
 * distractor is proven NOT to be reproducible. Unverified rows never enter this file.
 *
 * Fields are Devanagari; the UI renders them in the interface script with Script.render.
 */
data class QuizQuestion(
    val id: String,            // stable: <topic>-<table><row>-<F|R>
    val cat: String,           // vowel | consonant | visarga | prakriti
    val dir: String,           // F = join (yojana), R = separate (viccheda), O = one of the additional formats (see type)
    val type: String,          // F | R | NAME (identify the Sandhi) | SUTRA (identify the rule) | EXAMPLE (select an example of the named Sandhi)
    val lesson: String,        // topic id of the exact lesson
    val practice: String,      // topic id whose practice section applies
    val prompt: String,        // F: "L + R" ; R: the joined form
    val options: List<String>, // four options
    val answers: List<Int>,    // indices of all verified valid options (more than one = vikalpa)
    val exSa: String, val exKn: String, val exEn: String,
    val rules: List<String>,   // rule ids of the registry
    val source: String,        // provenance, e.g. "t08 ex row 3"
    val optionSources: List<String> = emptyList()   // EXAMPLE: the documented "L + R" behind each option
) { val vikalpa get() = answers.size > 1 }

/** Reads the bundled question bank (assets/quiz/questions.json). */
class QuizBank(private val ctx: Context) {
    val cats = listOf("vowel", "consonant", "visarga", "prakriti")
    private val all: List<QuizQuestion> by lazy { parse(ctx.assets.open("quiz/questions.json").bufferedReader(Charsets.UTF_8).readText()) }
    fun questions(cat: String): List<QuizQuestion> = all.filter { it.cat == cat }
    fun byId(id: String): QuizQuestion? = all.firstOrNull { it.id == id }
    fun count(cat: String) = questions(cat).size

    companion object {
        fun parse(json: String): List<QuizQuestion> {
            val arr = JSONObject(json).getJSONArray("questions")
            return (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                QuizQuestion(o.getString("id"), o.getString("cat"), o.getString("dir"), o.optString("type", o.getString("dir")), o.getString("lesson"), o.getString("practice"), o.getString("prompt"),
                    strs(o.getJSONArray("options")), ints(o.getJSONArray("answers")), o.getString("ex_sa"), o.getString("ex_kn"), o.getString("ex_en"),
                    strs(o.getJSONArray("rules")), o.getString("src"), o.optJSONArray("osrc")?.let { strs(it) } ?: emptyList())
            }
        }
        private fun strs(a: JSONArray) = (0 until a.length()).map { a.getString(it) }
        private fun ints(a: JSONArray) = (0 until a.length()).map { a.getInt(it) }
    }
}
