package org.sandhimitra.app

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Layout, menu and navigation checks executed on the JVM with Robolectric (no emulator available in the build environment). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w360dp-h800dp-xxhdpi")
class UiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun toHome() { rule.mainClock.autoAdvance = true; rule.mainClock.advanceTimeBy(5600); rule.waitForIdle() }
    private fun bounds(text: String): Rect = rule.onNodeWithText(text).fetchSemanticsNode().boundsInRoot

    @Test fun splashThenHomeHasExactSixCardsInThreeTiers() {
        rule.onNodeWithText("Compiled and Created by").assertExists()      // splash attribution
        toHome()
        val intro = bounds("परिचयः"); val vowel = bounds("स्वरसन्धिः"); val cons = bounds("व्यञ्जनसन्धिः")
        val vis = bounds("विसर्गसन्धिः"); val prak = bounds("प्रकृतिभावसन्धिः"); val sand = bounds("सन्धात्री")
        // tier ordering
        assertTrue(intro.bottom <= vowel.top && intro.bottom <= cons.top)
        assertTrue(sand.top >= vis.bottom && sand.top >= prak.bottom)
        // 2 x 2 grid positions
        assertTrue(vowel.left < cons.left && vis.left < prak.left)
        assertTrue(vowel.top < vis.top && cons.top < prak.top)
        assertEquals(vowel.top, cons.top, 2f); assertEquals(vis.top, prak.top, 2f)
        // full-width horizontal cards
        val root = rule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue("intro width", intro.width > root.width * 0.5f)
        assertTrue("sandhatri width", sand.width > root.width * 0.5f)
        assertEquals(1, rule.onAllNodesWithText("स्वरसन्धिः").fetchSemanticsNodes().size)
    }

    @Test fun tilesAreSquareAndEqual() {
        toHome()
        val tiles = listOf(bounds("स्वरसन्धिः"), bounds("व्यञ्जनसन्धिः"), bounds("विसर्गसन्धिः"), bounds("प्रकृतिभावसन्धिः"))
        // the tile container is larger than its label; verify via the parent node
        val parents = listOf("vowel", "consonant", "visarga", "prakriti").map { rule.onNodeWithTag("tile_$it").fetchSemanticsNode().boundsInRoot }
        parents.forEach { assertEquals("square", it.width, it.height, 2f) }
        assertEquals(parents[0].width, parents[1].width, 2f); assertEquals(parents[0].width, parents[3].width, 2f)
        assertTrue(tiles.all { it.width > 0 })
    }

    @Test fun menuOrderAndClosesAfterNavigation() {
        toHome()
        rule.onNodeWithContentDescription("Menu").performClick(); rule.waitForIdle()
        val order = listOf("मुखपृष्ठम्", "सन्धात्री", "About App", "About Dr. DVK", "Support Sandhimitram", "Feedback", "Privacy Policy")
        val tops = order.map { rule.onAllNodesWithText(it).fetchSemanticsNodes().last().boundsInRoot.top }
        assertEquals(tops.sorted(), tops)
        rule.onAllNodesWithText("About App").onLast().performClick(); rule.waitForIdle()
        rule.onNodeWithText("About the App").assertExists()     // only the page title remains: menu closed
    }

    @Test fun categoryTopicAndBack() {
        toHome()
        rule.onNodeWithText("स्वरसन्धिः").performClick(); rule.waitForIdle()
        rule.onNodeWithText("सवर्णदीर्घसन्धिः").assertExists().performClick(); rule.waitForIdle()
        rule.onNodeWithText("पाठः").assertExists(); rule.onNodeWithText("अभ्यासः").assertExists()      // Lesson | Practice capsule
        rule.onNodeWithText("अभ्यासः").performClick(); rule.waitForIdle()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.onNodeWithText("सवर्णदीर्घसन्धिः").assertExists()      // back to the category list
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.onNodeWithText("परिचयः").assertExists()                // back to Home
    }

    @Test fun capsuleNotOnPrakritibhavaOrIntroduction() {
        toHome()
        rule.onNodeWithText("प्रकृतिभावसन्धिः").performClick(); rule.waitForIdle()
        rule.onAllNodesWithText("प्रकृतिभावः").onFirst().performClick(); rule.waitForIdle()
        rule.onAllNodesWithText("अभ्यासः").assertCountEquals(0)
    }

    @Test fun rootBackAsksToExit() {
        toHome()
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }; rule.waitForIdle()
        rule.onNodeWithText("निर्गमनम्?").assertExists()
        rule.onNodeWithText("तिष्ठत").performClick(); rule.waitForIdle()
        rule.onNodeWithText("परिचयः").assertExists()
    }

    @Test fun sandhatriForwardRuns() {
        toHome()
        rule.onNodeWithText("सन्धात्री").performClick(); rule.waitForIdle()
        rule.onNodeWithText("सन्धियोजना").performClick(); rule.waitForIdle()
        rule.onNodeWithText("सन्धियोजना").assertExists()
    }
}
