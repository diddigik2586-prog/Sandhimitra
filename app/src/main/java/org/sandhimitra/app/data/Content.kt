package org.sandhimitra.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class TopicRef(val id: String, val sa: String, val kn: String, val en: String, val hasLp: Boolean) {
    fun title(lang: String) = when (lang) { "kn" -> kn; "en" -> en; else -> sa }
}
data class Section(val n: Int, val paras: List<String>)
data class LangContent(val title: String, val subtitle: String, val sections: List<Section>, val ex: List<List<String>>, val pr: List<List<String>>)
data class EnRule(val sutra: String, val ref: String, val gloss: String)
data class EnContent(val title: String, val overview: String, val rules: List<EnRule>, val notes: List<String>)
data class Topic(val id: String, val group: String, val en: EnContent, val sa: LangContent, val kn: LangContent) {
    fun lc(lang: String) = if (lang == "kn") kn else sa
}

/** Loads the bundled lesson content (assets/content). Source text is kept verbatim except the documented display fix below. */
class ContentRepo(private val ctx: Context) {
    private val cache = HashMap<String, Topic>()
    private val index: JSONObject by lazy { JSONObject(ctx.assets.open("content/index.json").bufferedReader(Charsets.UTF_8).readText()) }
    val groups = listOf("intro", "vowel", "consonant", "visarga", "prakriti", "practice")

    fun topics(group: String): List<TopicRef> {
        val arr = index.getJSONObject("groups").getJSONArray(group)
        return (0 until arr.length()).map { val o = arr.getJSONObject(it); TopicRef(o.getString("id"), o.getString("sa"), fix(o.getString("kn")), o.getString("en"), o.getBoolean("has_lp")) }
    }

    fun topic(id: String): Topic = cache.getOrPut(id) {
        val o = JSONObject(ctx.assets.open("content/topics/$id.json").bufferedReader(Charsets.UTF_8).readText())
        val e = o.getJSONObject("en")
        Topic(id, o.getString("group"),
            EnContent(e.getString("title"), e.getString("overview"),
                e.getJSONArray("rules").let { a -> (0 until a.length()).map { i -> a.getJSONObject(i).let { r -> EnRule(r.getString("sutra"), r.getString("ref"), r.getString("gloss")) } } },
                strList(e.getJSONArray("notes"))),
            lang(o.getJSONObject("sa")), lang(o.getJSONObject("kn")))
    }

    private fun lang(o: JSONObject): LangContent = LangContent(fix(o.getString("title")), fix(o.getString("subtitle")),
        o.getJSONArray("sections").let { a -> (0 until a.length()).map { i -> a.getJSONObject(i).let { s -> Section(s.getInt("n"), strList(s.getJSONArray("p")).map(::fix)) } } },
        table(o.opt("ex")), table(o.opt("pr")))

    private fun table(v: Any?): List<List<String>> {
        if (v !is JSONArray) return emptyList()
        return (0 until v.length()).map { r -> strList(v.getJSONArray(r)).map(::fix) }
    }

    private fun strList(a: JSONArray) = (0 until a.length()).map { a.getString(it) }

    /** Display fix (logged in docs/SOURCE_DISCREPANCIES.md): the Kannada source contains Bengali ঙ্ (U+0999 U+09CD) where Kannada ಙ್ is meant. */
    private fun fix(s: String) = if (s.indexOf('ঙ') >= 0) s.replace("ঙ্", "ಙ್") else s
}

/** Splits a "question — answer" paragraph of the self-study section. Markers are those used by the two source documents. */
fun splitQa(p: String): Pair<String, String>? {
    val qm = listOf("सिद्धान्तोत्तरम् —", "ಉತ್ತರ —")
    for (m in qm) {
        val i = p.indexOf(m)
        if (i > 0) {
            val q = p.substring(0, i).trim().removePrefix("प्रश्नः —").removePrefix("ಪ್ರಶ್ನೆ —").trim()
            return q to p.substring(i + m.length).trim()
        }
    }
    return null
}
