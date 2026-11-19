package com.gasmeter.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TripValidatorTest {

    private fun issues(rangeStart: Double, rangeEnd: Double, odoStart: Double, odoEnd: Double, car: CarProfile = testCar) =
        TripValidator.validate(TripReadings(rangeStart, rangeEnd, odoStart, odoEnd), car)

    @Test
    fun `normal trip has no issues`() {
        assertEquals(emptyList(), issues(300.0, 290.0, 1000.0, 1008.0))
    }

    @Test
    fun `odometer going backwards is blocking`() {
        val result = issues(300.0, 290.0, 1000.0, 999.0)
        assertTrue(TripIssue.OdometerWentBackwards in result)
        assertTrue(result.single().isBlocking)
    }

    @Test
    fun `negative reading is blocking`() {
        val result = issues(300.0, -5.0, 1000.0, 1005.0)
        assertTrue(TripIssue.NegativeReading in result)
    }

    @Test
    fun `very long distance is a warning`() {
        val result = issues(600.0, 0.0, 1000.0, 3000.0)
        val issue = result.single()
        assertEquals(TripIssue.ImplausibleDistance(2000.0), issue)
        assertTrue(!issue.isBlocking)
    }

    @Test
    fun `small range increase is allowed silently`() {
        assertEquals(emptyList(), issues(295.0, 300.0, 1000.0, 1002.0))
    }

    @Test
    fun `large range increase suggests a refuel`() {
        val result = issues(80.0, 520.0, 1000.0, 1010.0)
        assertEquals(listOf<TripIssue>(TripIssue.PossibleRefuel(440.0)), result)
    }

    @Test
    fun `refuel threshold grows with a coarse display step`() {
        val coarse = CarProfile(6.0, rangeDisplayStepKm = 20.0)
        // 40 km gain: over the 30 km default but under 3 x 20 km steps.
        assertEquals(emptyList(), issues(100.0, 140.0, 0.0, 5.0, coarse))
        assertEquals(1, issues(100.0, 170.0, 0.0, 5.0, coarse).size)
    }
}
