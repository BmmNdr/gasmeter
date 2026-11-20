package com.gasmeter.app

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the main screens to PNG files. Runs only when the
 * `gasmeter.screenshotDir` system property is set (see app/build.gradle.kts).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    private val outDir: File? = System.getProperty("gasmeter.screenshotDir")?.let(::File)

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun shot(name: String, mode: String) {
        compose.waitForIdle()
        var bitmap: Bitmap? = null
        compose.activityRule.scenario.onActivity { activity ->
            val view = activity.window.decorView
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also {
                view.draw(android.graphics.Canvas(it))
            }
        }
        outDir!!.mkdirs()
        File(outDir, "$mode-$name.png").outputStream().use { bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun lightMode() = mainScreens("light")

    @Test
    @Config(qualifiers = "+night")
    fun darkMode() = mainScreens("dark")

    private fun mainScreens(mode: String) {
        assumeTrue("screenshot dir not set", outDir != null)

        // Seed: a commute with morning runs and heavy-traffic evenings; the flow
        // below adds one more, expensive run.
        TestSeed().apply {
            commuteHistory()
            costs()
        }

        waitForText("Recent trips")
        waitForText("Home → Work")
        shot("1-home", mode)

        compose.onNodeWithText("Start trip").performClick()
        waitForText("Dashboard readings now")
        compose.onNodeWithText("Home → Work").performClick()
        shot("2-start-trip", mode)
        compose.onNodeWithTag("startButton").performScrollTo().performClick()

        waitForText("Trip in progress")
        shot("3-trip-in-progress", mode)
        compose.onNodeWithText("End trip").performClick()
        waitForText("Dashboard readings now")
        compose.onNodeWithTag("endRangeField").performTextInput("300.5")
        compose.onNodeWithTag("endOdometerField").performTextInput("41278")
        compose.onNodeWithText("Heavy traffic").performClick()
        shot("4-end-trip", mode)

        compose.onNodeWithTag("saveTripButton").performScrollTo().performClick()
        waitForText("Trip result")
        waitForText("Expensive")
        shot("5-result", mode)

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("Recent trips")
        compose.onNodeWithText("ROUTES").performClick()
        waitForText("7 trips")
        shot("6-routes", mode)

        compose.onNodeWithText("Home → Work").performClick()
        waitForText("Usual range per km")
        shot("7-route-detail", mode)
        compose.onAllNodesWithText("used", substring = true).onLast().performScrollTo()
        shot("8-route-trips", mode)

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("Your routes")
        compose.onNodeWithContentDescription("Settings").performClick()
        waitForText("Average consumption")
        shot("9-settings", mode)
        compose.onNodeWithContentDescription("Delete price", substring = true).performScrollTo()
        shot("10-settings-prices", mode)
    }
}
