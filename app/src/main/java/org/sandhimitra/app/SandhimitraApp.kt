package org.sandhimitra.app

import android.app.Application
import org.sandhimitra.app.data.ContentRepo
import org.sandhimitra.app.data.LexiconDb
import org.sandhimitra.app.data.Prefs
import org.sandhimitra.app.data.Progress
import org.sandhimitra.app.engine.SandhiEngine

/** Application-level singletons (no DI framework needed for this size). */
class SandhimitraApp : Application() {
    lateinit var prefs: Prefs
    lateinit var progress: Progress
    lateinit var content: ContentRepo
    val lexicon: LexiconDb by lazy { LexiconDb(this) }
    val engine: SandhiEngine by lazy { SandhiEngine(lexicon) }
    val quiz: org.sandhimitra.app.data.QuizBank by lazy { org.sandhimitra.app.data.QuizBank(this) }

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this); progress = Progress(this); content = ContentRepo(this)
    }
}
