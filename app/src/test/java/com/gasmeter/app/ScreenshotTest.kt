package com.gasmeter.app

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
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

        // Seed: three normal runs of a route, then the flow below adds an expensive one.
        val repo = ApplicationProvider.getApplicationContext<GasMeterApp>().repository
        runBlocking {
            repo.setRangeDisplayStep(1.0)
            val route = repo.addRoute("Home → Work")
            repo.addRoute("Gym")
            var range = 320.0
            var odo = 41_200.0
            listOf(12.0, 13.0, 12.5).forEach { used ->
                repo.startTrip(route, range, odo)
                repo.finishTrip(range - used, odo + 11.0)
                range -= used
                odo += 11.0
            }
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
        compose.onNodeWithTag("endRangeField").performTextInput("265")
        compose.onNodeWithTag("endOdometerField").performTextInput("41244")
        compose.onNodeWithText("Heavy traffic").performClick()
        shot("4-end-trip", mode)

        compose.onNodeWithTag("saveTripButton").performScrollTo().performClick()
        waitForText("Trip result")
        waitForText("Expensive")
        shot("5-result", mode)
    }
}
