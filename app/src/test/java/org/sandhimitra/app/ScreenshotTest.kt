package org.sandhimitra.app

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders key screens to PNG files (app/build/screenshots) for visual QA; also fails if a screen cannot be composed. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val out = File("build/screenshots").apply { mkdirs() }
    private fun shot(name: String, idle: Boolean = true) {
        if (idle) rule.waitForIdle()
        val v = rule.activity.window.decorView
        val bmp = android.graphics.Bitmap.createBitmap(1080, 2400, android.graphics.Bitmap.Config.ARGB_8888)
        v.measure(android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY), android.view.View.MeasureSpec.makeMeasureSpec(2400, android.view.View.MeasureSpec.EXACTLY)); v.layout(0, 0, 1080, 2400)
        v.draw(android.graphics.Canvas(bmp))
        File(out, "$name.png").outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun toHome() { rule.mainClock.advanceTimeBy(5600); rule.waitForIdle() }
    private val app get() = rule.activity.application as SandhimitraApp

    @Test fun screens() {
        rule.mainClock.autoAdvance = false; rule.mainClock.advanceTimeBy(2500); shot("00_splash", idle = false)
        rule.mainClock.autoAdvance = true
        toHome(); shot("01_home_sa_light")
        app.prefs.chooseLang("kn"); shot("02_home_kn_light")
        app.prefs.chooseLang("en"); shot("03_home_en_light")
        app.prefs.chooseDark(true); shot("04_home_en_dark")
        app.prefs.chooseDark(false); app.prefs.chooseLang("sa")
        rule.onNodeWithContentDescription("Menu").performClick(); shot("05_menu_sa")
        rule.onNodeWithText("✕").performClick()
        rule.onNodeWithText("स्वरसन्धिः").performClick(); shot("06_vowel_list_sa")
        rule.onNodeWithText("गुणसन्धिः").performClick(); shot("07_guna_lesson_sa")
        app.prefs.chooseLang("kn"); shot("08_guna_lesson_kn")
        app.prefs.chooseLang("en"); shot("09_guna_lesson_en")
        rule.onNodeWithText("Practice").performClick(); shot("10_guna_practice_en")
        app.prefs.chooseLang("sa")
    }

    @Test fun engineScreens() {
        toHome()
        rule.onNodeWithText("सन्धात्री").performClick(); shot("11_sandhatri_home_sa")
        rule.onNodeWithText("सन्धियोजना").performClick()
        rule.onAllNodesWithText("प्रथमं पदम्").onFirst().performClick(); shot("12_yojana_keyboard_sa")
    }

    @Test fun infoScreens() {
        toHome()
        for ((label, name) in listOf("Support Sandhimitra" to "20_support", "Privacy Policy" to "21_privacy", "About App" to "22_about")) {
            rule.onNodeWithContentDescription("Menu").performClick(); rule.waitForIdle()
            rule.onAllNodesWithText(label).onLast().performClick(); shot(name)
        }
    }
}
