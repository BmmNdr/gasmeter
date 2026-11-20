package com.gasmeter.app

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Settings through the real UI: consumption, rounding step, fuel price, and the costs they unlock. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class SettingsFlowTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    private fun waitForText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text, substring = true, ignoreCase = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun goBack() {
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
    }

    private fun openSettings() {
        compose.onNodeWithContentDescription("Settings").performClick()
        waitForText("Average consumption")
    }

    @Test
    fun settingConsumptionAndPrice_showsCostsOnTrip() {
        // Your spec example: 5 range-km used on a 2 km trip, with a 1 km display step.
        val seed = TestSeed()
        seed.step(1.0)
        seed.drive(null, "2026-03-02T08:00", distance = 2.0, rangeUsed = 5.0)
        waitForText("used 5 km")

        openSettings()
        compose.onNodeWithTag("consumptionField").performTextInput("6")
        compose.onNodeWithText("5 km").performClick() // the rounding step can change after trips exist
        compose.onNodeWithTag("saveCarButton").performScrollTo().performClick()
        waitForText("Saved")

        compose.onNodeWithText("No price yet").assertExists()
        compose.onNodeWithTag("priceField").performScrollTo().performTextInput("1,80")
        compose.onNodeWithTag("addPriceButton").performScrollTo().performClick()
        waitForText("1.800 €/L")

        goBack()
        waitForText("· 0.54 €") // the cost now shows on the trip row
        compose.onNodeWithText("used 5 km", substring = true).performClick()
        waitForText("Trip result")
        compose.onNodeWithText("0.30 L").assertExists()
        compose.onNodeWithText("0.54 €").assertExists()
        compose.onNodeWithText("0.32 €").assertExists()
        compose.onNodeWithText("5 km (±5 km)").assertExists()
    }

    @Test
    fun tripResult_linksToSettingsWhenCostIsMissing() {
        TestSeed().apply { step(1.0) }.drive(null, "2026-03-02T08:00", 2.0, 5.0)
        waitForText("used 5 km")
        compose.onNodeWithText("used 5 km", substring = true).performClick()
        waitForText("Set your car's consumption in Settings")
        compose.onNodeWithText("Open settings").performScrollTo().performClick()
        waitForText("Average consumption")
    }

    @Test
    fun invalidConsumption_isRejected() {
        openSettings()
        compose.onNodeWithTag("consumptionField").performTextInput("0")
        compose.onNodeWithTag("saveCarButton").performScrollTo().performClick()
        waitForText("Must be more than 0")

        compose.onNodeWithTag("consumptionField").performTextClearance()
        compose.onNodeWithTag("consumptionField").performTextInput("abc")
        compose.onNodeWithTag("saveCarButton").performScrollTo().performClick()
        waitForText("Enter a number")
    }

    @Test
    fun deletingPrice_returnsToNoPrice() {
        openSettings()
        compose.onNodeWithTag("priceField").performScrollTo().performTextInput("1.799")
        compose.onNodeWithTag("addPriceButton").performScrollTo().performClick()
        waitForText("1.799 €/L")

        compose.onNodeWithContentDescription("Delete price", substring = true).performScrollTo().performClick()
        waitForText("No price yet")
    }
}
