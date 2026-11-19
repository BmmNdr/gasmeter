package com.gasmeter.app

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Drives the real app (real database, real navigation) through a full trip. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class TripFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun startTrip(range: String, odometer: String, newRoute: String? = null) {
        waitForText("Start trip")
        compose.onNodeWithText("Start trip").performClick()
        waitForText("Dashboard readings now")
        if (newRoute != null) {
            compose.onNodeWithText("+ New route").performClick()
            compose.onNodeWithTag("routeNameField").performTextInput(newRoute)
            compose.onNodeWithText("Add").performClick()
            waitForText(newRoute)
        }
        compose.onNodeWithTag("rangeField").performTextInput(range)
        compose.onNodeWithTag("odometerField").performTextInput(odometer)
    }

    @Test
    fun fullTrip_showsSpecExampleOnResultCard() {
        startTrip(range = "300", odometer = "12000", newRoute = "Home → Work")

        // First trip ever: the app asks how the dashboard rounds the range.
        compose.onNodeWithText("5 km steps").performScrollTo().performClick()
        compose.onNodeWithTag("startButton").performScrollTo().performClick()

        waitForText("Trip in progress")
        compose.onNodeWithText("End trip").performClick()

        waitForText("Start range 300 km")
        compose.onNodeWithTag("endRangeField").performTextInput("295")
        compose.onNodeWithTag("endOdometerField").performTextInput("12002")
        compose.onNodeWithTag("saveTripButton").performScrollTo().performClick()

        waitForText("Trip result")
        waitForText("Not enough history yet")
        compose.onNodeWithText("+3 km").assertExists()
        compose.onNodeWithText("2.50×").assertExists()
        compose.onNodeWithText("5 km (±5 km)").assertExists()
        compose.onNodeWithText("Short trip", substring = true).assertExists()

        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("Recent trips")
        waitForText("2 km · used 5 km · +3 km · 2.50×")
        compose.onNodeWithText("Start trip").assertExists()
    }

    @Test
    fun odometerGoingBackwards_blocksSaving() {
        startTrip(range = "300", odometer = "12000")
        compose.onNodeWithText("1 km steps").performScrollTo().performClick()
        compose.onNodeWithTag("startButton").performScrollTo().performClick()
        waitForText("Trip in progress")
        compose.onNodeWithText("End trip").performClick()

        waitForText("Start range 300 km")
        compose.onNodeWithTag("endRangeField").performTextInput("295")
        compose.onNodeWithTag("endOdometerField").performTextInput("11990")

        waitForText("The end odometer is lower than the start odometer.")
        compose.onNodeWithTag("saveTripButton").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun commaDecimalsAreAccepted() {
        startTrip(range = "300", odometer = "12000,5")
        compose.onNodeWithText("1 km steps").performScrollTo().performClick()
        compose.onNodeWithTag("startButton").performScrollTo().performClick()
        waitForText("Trip in progress")
        waitForText("odometer 12000.5 km")
    }
}
