package com.gasmeter.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Routes tab, route page, rename, change a trip's route, delete: through the real UI. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class RoutesFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitUntilGone(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun openRoutesTab() {
        compose.onNodeWithText("ROUTES").performClick()
        waitForText("Your routes")
    }

    @Test
    fun routesTab_listsRoutesWithStats() {
        TestSeed().commuteHistory()
        openRoutesTab()
        waitForText("6 trips · usual")
        compose.onNodeWithText("1 trip", substring = true).assertExists()

        // Back to the Trips tab keeps working.
        compose.onNodeWithText("TRIPS").performClick()
        waitForText("Recent trips")
    }

    @Test
    fun routePage_showsStatsChartAndBreakdown() {
        TestSeed().commuteHistory()
        openRoutesTab()
        waitForText("6 trips")
        compose.onNodeWithText("Home → Work").performClick()

        waitForText("Usual range per km")
        compose.onNodeWithTag("ratioChart").assertExists()
        compose.onNodeWithText("Trips counted", substring = true).assertExists()
        // Two heavy-traffic evening trips vs four plain morning trips.
        compose.onNodeWithText("Heavy traffic").assertExists()
        compose.onNodeWithText("2 vs 4 trips", substring = true).assertExists()
        compose.onNodeWithText("Morning (5–10) · 4", substring = true).assertExists()
        compose.onNodeWithText("Evening (16–21) · 2", substring = true).assertExists()
        // The trend needs more than 5 trips: 6 here.
        compose.onNodeWithText("Last 5 trips", substring = true).assertExists()
    }

    @Test
    fun renameRoute_inline() {
        val seed = TestSeed()
        val ids = seed.commuteHistory()
        openRoutesTab()
        waitForText("6 trips")
        compose.onNodeWithText("Home → Work").performClick()
        waitForText("Usual range per km")

        compose.onNodeWithContentDescription("Route options").performClick()
        compose.onNodeWithText("Rename").performClick()
        compose.onNodeWithTag("renameField").performTextClearance()
        compose.onNodeWithTag("renameField").performTextInput("gym")
        compose.onNodeWithText("Save name").performClick()
        waitForText("Another route is already called")

        compose.onNodeWithTag("renameField").performTextClearance()
        compose.onNodeWithTag("renameField").performTextInput("Commute")
        compose.onNodeWithText("Save name").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithTag("renameField").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("COMMUTE").assertExists()

        val names = runBlocking { seed.repository.routes.first().associate { it.id to it.name } }
        assertEquals("Commute", names[ids.work])
    }

    @Test
    fun changeTripRoute_fromTripResult() {
        val seed = TestSeed()
        val ids = seed.commuteHistory()
        openRoutesTab()
        waitForText("6 trips")
        compose.onNodeWithText("Home → Work").performClick()
        waitForText("Trips on this route")

        // The 1.60× evening trip.
        compose.onNodeWithText("+6 km · 1.60×", substring = true).performScrollTo().performClick()
        waitForText("Trip result")
        compose.onNodeWithText("Change route").performClick()
        compose.onNodeWithText("Gym").performClick()

        compose.waitUntil(10_000) {
            runBlocking { seed.repository.history.first().single { it.log.id == ids.eveningTrip2 }.log.routeId } == ids.gym
        }
        compose.onNodeWithText("No route").performClick()
        compose.waitUntil(10_000) {
            runBlocking { seed.repository.history.first().single { it.log.id == ids.eveningTrip2 }.log.routeId } == null
        }
    }

    @Test
    fun deleteRoute_keepsTrips() {
        val seed = TestSeed()
        val ids = seed.commuteHistory()
        openRoutesTab()
        waitForText("6 trips")
        compose.onNodeWithText("Home → Work").performClick()
        waitForText("Usual range per km")

        compose.onNodeWithContentDescription("Route options").performClick()
        compose.onNodeWithText("Delete route").performClick()
        waitForText("will stay in your history")
        compose.onNodeWithText("Delete").performClick()

        waitForText("Your routes")
        waitUntilGone("Home → Work")
        val history = runBlocking { seed.repository.history.first() }
        assertEquals(8, history.size)
        assertTrue(history.none { it.log.routeId == ids.work })
        assertNull(history.single { it.log.id == ids.eveningTrip1 }.log.routeId)
    }
}
