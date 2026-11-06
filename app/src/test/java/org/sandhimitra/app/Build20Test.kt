package org.sandhimitra.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.sandhimitra.app.data.Progress
import org.sandhimitra.app.data.QuizBank
import org.sandhimitra.app.ui.QuizSession

/** Build 20 checks: localized name, Home fits the viewport, Sandhicakram landing and quiz, session persistence, bookmarks, schema migration. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class Build20Test {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val app get() = ApplicationProvider.getApplicationContext<SandhimitraApp>()
    private fun toHome() { rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(5600); rule.waitForIdle() }
    private fun back() { rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle() }

    @Test fun launcherLabelIsLocalized() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Sandhimitram", ctx.getString(R.string.app_name))
        val kn = ctx.createConfigurationContext(android.content.res.Configuration(ctx.resources.configuration).apply { setLocale(java.util.Locale("kn")) })
        assertEquals("ಸಂಧಿಮಿತ್ರಂ", kn.getString(R.string.app_name))
        assertEquals("Sandhimitram", ctx.packageManager.getApplicationLabel(ctx.applicationInfo).toString())
        assertEquals("org.sandhimitra.app", ctx.packageName)
    }

    @Test fun homeFitsWithoutScrollingAndGamesCardIsLast() {
        toHome()
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val games = rule.onNodeWithTag("bar_games").fetchSemanticsNode().boundsInRoot
        val sand = rule.onNodeWithText("सन्धात्री").fetchSemanticsNode().boundsInRoot
        val attr = rule.onNodeWithText("Compiled and Created by").fetchSemanticsNode().boundsInRoot
        assertTrue("games below sandhatri", games.top >= sand.bottom)
        assertTrue("games card inside the viewport", games.bottom <= root.bottom && attr.bottom <= root.bottom)
        assertTrue("footer below games", attr.top >= games.bottom)
        rule.onNodeWithText("सन्धिचक्रम्").assertExists()
        // the joystick icon sits before the title (to its left)
        val title = rule.onNodeWithText("सन्धिचक्रम्").fetchSemanticsNode().boundsInRoot
        val icon = rule.onNodeWithTag("games_icon", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertTrue("icon before the title", icon.left >= games.left && icon.left < (title.left + title.right) / 2 && icon.top >= games.top && icon.bottom <= games.bottom)
        rule.onAllNodesWithText("/ ", substring = true).fetchSemanticsNodes().forEach { n -> assertTrue("no game progress on Home", n.boundsInRoot.bottom <= games.top) }
        // header: bookmark icon beside the menu icon
        val bm = rule.onNodeWithContentDescription("Bookmarks").fetchSemanticsNode().boundsInRoot
        val menu = rule.onNodeWithContentDescription("Menu").fetchSemanticsNode().boundsInRoot
        assertTrue("bookmark icon beside the menu icon", bm.left < menu.left && bm.right > root.width / 2 && bm.right <= menu.right)
    }

    @Test fun gamesLandingHasFourSquareTilesWithProgress() {
        toHome(); rule.onNodeWithTag("bar_games").performClick(); rule.waitForIdle()
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        val tiles = listOf("vowel", "consonant", "visarga", "prakriti").map { rule.onNodeWithTag("game_$it").fetchSemanticsNode().boundsInRoot }
        tiles.forEach { assertEquals("square", it.width, it.height, 2f); assertTrue(it.bottom <= root.bottom) }
        assertTrue(tiles[0].left < tiles[1].left && tiles[0].top < tiles[2].top)
        val total = app.quiz.count("vowel")
        rule.onNodeWithText("0 / $total").assertExists()
    }

    @Test fun quizAnswersPersistAndRestore() {
        // a fresh mixed round alternates the two directions
        val all = app.quiz.questions("vowel")
        val dirs = QuizSession.fresh("vowel", all, 7L).seq.map { app.quiz.byId(it)!!.dir }
        assertTrue(dirs.filter { it != "O" }.windowed(2).all { it[0] != it[1] }); assertTrue(dirs.count { it == "O" } == all.count { it.dir == "O" })
        assertTrue("other formats spread, not bunched at the end", dirs.take(dirs.size / 2).count { it == "O" } > 0)
        // a controlled round of plain (non-vikalpa) questions: reverse, forward, reverse
        val plain = all.filter { !it.vikalpa }
        val s0 = QuizSession("vowel", listOf(plain.first { it.dir == "R" }.id, plain.first { it.dir == "F" }.id, plain.last { it.dir == "R" }.id))
        app.progress.saveQuizSession("vowel", s0.toJson())
        toHome(); rule.onNodeWithTag("bar_games").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("game_vowel").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("quiz_prompt").assertExists()
        val q = app.quiz.byId(s0.seq[0])!!
        // choose a wrong option: Wrong Answer with the two learning links; nothing advances
        val wrong = (0..3).first { it !in q.answers }
        rule.onNodeWithTag("opt_$wrong").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("verdict_wrong").assertExists(); rule.onNodeWithText("पाठं पठत").assertExists(); rule.onNodeWithText("अभ्यासं पश्यत").assertExists()
        rule.onAllNodesWithTag("opt_${q.answers[0]}").onFirst().performClick(); rule.waitForIdle()   // locked after an answer
        val s1 = QuizSession.fromJson(app.progress.quizSession("vowel")!!)!!
        assertEquals(1, s1.history.length()); assertEquals(0, s1.pos); assertEquals(1, s1.wrong)
        // read the lesson and come back: same question, same state
        rule.onNodeWithText("पाठं पठत").performClick(); rule.waitForIdle()
        rule.onNodeWithText(app.content.topic(q.lesson).sa.title).assertExists()
        back()
        rule.onNodeWithTag("quiz_prompt").assertExists(); rule.onNodeWithTag("verdict_wrong").assertExists()
        assertEquals(0, QuizSession.fromJson(app.progress.quizSession("vowel")!!)!!.pos)
        rule.onNodeWithText("अग्रिमः प्रश्नः").performClick(); rule.waitForIdle()
        val s2 = QuizSession.fromJson(app.progress.quizSession("vowel")!!)!!
        assertEquals(1, s2.pos)
        // the correct answer of the second question advances and counts as completed
        val q2 = app.quiz.byId(s2.seq[1])!!
        rule.onNodeWithTag("quiz_prompt").assertExists()
        rule.onNodeWithTag("opt_${q2.answers[0]}").assertExists().performClick(); rule.waitForIdle()
        if (rule.onAllNodesWithTag("verdict_ok").fetchSemanticsNodes().isEmpty()) { rule.onRoot().printToLog("QUIZ"); fail("no ok verdict after clicking opt_${q2.answers[0]} (q2=${q2.id} answers=${q2.answers}); session=" + app.progress.quizSession("vowel")) }
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        val s3 = QuizSession.fromJson(app.progress.quizSession("vowel")!!)!!
        assertEquals(1, s3.correct); assertTrue(s3.pos >= 1); assertEquals(1, app.progress.quizDoneCount("vowel"))
        // leaving and re-entering restores the same position
        back(); rule.onNodeWithTag("game_vowel").performClick(); rule.waitForIdle()
        assertEquals(s3.pos, QuizSession.fromJson(app.progress.quizSession("vowel")!!)!!.pos)
        rule.onNodeWithText("${s3.pos + 1} / ${s3.seq.size}", substring = true).assertExists()
    }

    @Test fun vikalpaQuestionNeedsBothForms() {
        val qs = app.quiz.questions("consonant"); val v = qs.first { it.vikalpa }
        val s = QuizSession("consonant", listOf(v.id) + qs.filter { it.id != v.id }.take(2).map { it.id })
        app.progress.saveQuizSession("consonant", s.toJson())
        toHome(); rule.onNodeWithTag("bar_games").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("game_consonant").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("opt_${v.answers[0]}").performClick(); rule.waitForIdle()
        rule.onNodeWithText("समीचीनम्! इदानीम् अन्यत् वैकल्पिकं रूपं चिनुत").assertExists()
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        rule.onNodeWithTag("opt_${v.answers[0]}").performClick(); rule.waitForIdle()      // same alternative again → try again
        rule.onNodeWithText("असमीचीनम् — पुनः प्रयतध्वम्", substring = true).assertExists()
        rule.onNodeWithTag("opt_${v.answers[1]}").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("verdict_ok").assertExists()
        rule.mainClock.advanceTimeBy(1500); rule.waitForIdle()
        val st = QuizSession.fromJson(app.progress.quizSession("consonant")!!)!!
        assertEquals(1, st.pos); assertEquals(setOf(v.answers[0], v.answers[1]), st.vik[v.id])
    }

    @Test fun bookmarkToggleListAndRemove() {
        toHome()
        rule.onNodeWithText("स्वरसन्धिः").performClick(); rule.waitForIdle()
        rule.onNodeWithText("सवर्णदीर्घसन्धिः").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("bm_t08").performClick(); rule.waitForIdle()
        rule.onNodeWithTag("bm_t08").performClick(); rule.waitForIdle(); rule.onNodeWithTag("bm_t08").performClick(); rule.waitForIdle()   // toggled thrice: on
        assertEquals(listOf("t08"), app.progress.bookmarks())
        app.progress.setBookmark("t08", true); assertEquals(1, app.progress.bookmarks().size)          // no duplicates
        rule.onNodeWithContentDescription("Bookmarks").performClick(); rule.waitForIdle()
        rule.onNodeWithText("मम पुस्तकचिह्नानि").assertExists(); rule.onNodeWithText("सवर्णदीर्घसन्धिः").assertExists(); rule.onNodeWithText("स्वरसन्धिः").assertExists()
        rule.onNodeWithText("सवर्णदीर्घसन्धिः").performClick(); rule.waitForIdle()
        rule.onNodeWithText("पाठः").assertExists()                                                     // the exact lesson opened
        back(); rule.onNodeWithTag("bm_t08").performClick(); rule.waitForIdle()                        // removed from the list
        assertTrue(app.progress.bookmarks().isEmpty())
        rule.onNodeWithText("पुस्तकचिह्नानि अद्यापि न सन्ति", substring = true).assertExists()
    }

    @Test fun schemaMigrationKeepsBuild19Data() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        ctx.deleteDatabase("mig.db")
        object : SQLiteOpenHelper(ctx, "mig.db", null, 1) {
            override fun onCreate(db: SQLiteDatabase) {
                db.execSQL("CREATE TABLE lesson(topic TEXT PRIMARY KEY, done INTEGER NOT NULL, ts INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE practice(topic TEXT NOT NULL, item TEXT NOT NULL, correct INTEGER NOT NULL, ts INTEGER NOT NULL, PRIMARY KEY(topic,item))")
                db.execSQL("CREATE TABLE bookmark(topic TEXT PRIMARY KEY, ts INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, kind TEXT NOT NULL, input TEXT NOT NULL, ts INTEGER NOT NULL)")
                db.insert("lesson", null, ContentValues().apply { put("topic", "t09"); put("done", 1); put("ts", 1L) })
                db.insert("bookmark", null, ContentValues().apply { put("topic", "t10"); put("ts", 2L) })
                db.insert("history", null, ContentValues().apply { put("kind", "join"); put("input", "राम+अयम्"); put("ts", 3L) })
            }
            override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) {}
        }.writableDatabase.close()
        // real upgrade path: open mig.db with the Build 20 schema version through Progress' onUpgrade
        val helper = object : SQLiteOpenHelper(ctx, "mig.db", null, 2) {
            override fun onCreate(db: SQLiteDatabase) = fail("must upgrade, not create")
            override fun onUpgrade(db: SQLiteDatabase, o: Int, n: Int) { assertEquals(1, o); Progress.upgrade(db, o) }
        }
        val db = helper.writableDatabase
        assertEquals(1, db.rawQuery("SELECT done FROM lesson WHERE topic='t09'", null).use { it.moveToFirst(); it.getInt(0) })
        assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM bookmark", null).use { it.moveToFirst(); it.getInt(0) })
        assertEquals(1, db.rawQuery("SELECT COUNT(*) FROM history", null).use { it.moveToFirst(); it.getInt(0) })
        assertEquals(0, db.rawQuery("SELECT COUNT(*) FROM quiz_session", null).use { it.moveToFirst(); it.getInt(0) })
        assertEquals(0, db.rawQuery("SELECT COUNT(*) FROM quiz_done", null).use { it.moveToFirst(); it.getInt(0) })
        db.close()
        // the app's own database also carries the new tables after the upgrade
        assertEquals(0, Progress(ctx).quizDoneCount("vowel"))
    }

    @Test fun noOldNameInInterfaceStrings() {
        val src = java.io.File("src/main/java/org/sandhimitra/app/ui/Strings.kt").readText()
        assertFalse(Regex("Sandhimitra(?!m)").containsMatchIn(src))
        for (f in listOf("Info.kt", "MainApp.kt", "Components.kt", "Screens1.kt", "Screens2.kt", "Sandhatri.kt", "Games.kt", "PdfExport.kt"))
            java.io.File("src/main/java/org/sandhimitra/app/ui/$f").readText().lines().filter { it.contains("\"") && Regex("Sandhimitra(?![mA])").containsMatchIn(it) }
                .forEach { fail("old name in $f: $it") }
    }

    @Test fun questionBankLoadsThroughAssets() {
        val b = QuizBank(ApplicationProvider.getApplicationContext())
        assertTrue(b.count("vowel") >= 150); assertTrue(b.count("consonant") >= 150); assertTrue(b.count("visarga") >= 150); assertTrue(b.count("prakriti") > 0)
        val q = b.questions("vowel").first(); assertNotNull(b.byId(q.id))
        JSONObject(QuizSession.fresh("vowel", b.questions("vowel"), 1L).toJson())
    }
}
