package com.gasmeter.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TripCalculatorTest {
    private val tolerance = 1e-9

    @Test
    fun `spec example - 300 to 295 km range on a 2 km trip`() {
        val readings = TripReadings(
            rangeStartKm = 300.0,
            rangeEndKm = 295.0,
            odometerStartKm = 12_000.0,
            odometerEndKm = 12_002.0,
        )
        val m = TripCalculator.calculate(readings, testCar, fuelPricePerLitre = 1.80)

        assertEquals(2.0, m.distanceKm, tolerance)
        assertEquals(5.0, m.rangeUsedKm, tolerance)
        assertEquals(3.0, m.excessKm, tolerance)
        assertEquals(2.5, m.ratio!!, tolerance)
        // 5 range-km at 6 L/100 km = 0.3 L; at 1.80/L = 0.54
        assertEquals(0.3, m.litres!!, tolerance)
        assertEquals(0.54, m.cost!!, tolerance)
        // 3 excess km = 0.18 L = 0.324
        assertEquals(0.324, m.excessCost!!, tolerance)
        assertFalse(m.isIdle)
    }

    @Test
    fun `no fuel price means no money values but km values still computed`() {
        val m = trip(distanceKm = 10.0, rangeUsedKm = 12.0, price = null).metrics
        assertNull(m.cost)
        assertNull(m.excessCost)
        assertEquals(1.2, m.ratio!!, tolerance)
        assertEquals(0.72, m.litres!!, tolerance)
    }

    @Test
    fun `unknown consumption means no litres or money values`() {
        val car = CarProfile(consumptionLitresPer100Km = null)
        val m = TripCalculator.calculate(TripReadings(300.0, 295.0, 0.0, 2.0), car, fuelPricePerLitre = 1.80)
        assertNull(m.litres)
        assertNull(m.cost)
        assertNull(m.excessCost)
        assertEquals(2.5, m.ratio!!, tolerance)
    }

    @Test
    fun `idle trip has no ratio`() {
        val m = trip(distanceKm = 0.0, rangeUsedKm = 2.0).metrics
        assertTrue(m.isIdle)
        assertNull(m.ratio)
        assertEquals(2.0, m.excessKm, tolerance)
        assertTrue(m.tooShortToJudge)
    }

    @Test
    fun `range going up gives negative range used and negative ratio`() {
        val m = trip(distanceKm = 4.0, rangeUsedKm = -2.0).metrics
        assertEquals(-2.0, m.rangeUsedKm, tolerance)
        assertEquals(-6.0, m.excessKm, tolerance)
        assertEquals(-0.5, m.ratio!!, tolerance)
    }

    @Test
    fun `trip is too short to judge when distance is under two display steps`() {
        val coarse = CarProfile(consumptionLitresPer100Km = 6.0, rangeDisplayStepKm = 5.0)
        fun metrics(distance: Double) = TripCalculator.calculate(
            TripReadings(300.0, 295.0, 0.0, distance), coarse, null,
        )
        assertTrue(metrics(2.0).tooShortToJudge)
        assertTrue(metrics(9.9).tooShortToJudge)
        assertFalse(metrics(10.0).tooShortToJudge)
        assertEquals(5.0, metrics(2.0).rangeUncertaintyKm, tolerance)
    }

    @Test
    fun `calculate refuses blocking errors`() {
        assertFailsWith<IllegalArgumentException> {
            TripCalculator.calculate(TripReadings(300.0, 295.0, 100.0, 90.0), testCar, null)
        }
    }

    @Test
    fun `car profile rejects non-positive values`() {
        assertFailsWith<IllegalArgumentException> { CarProfile(0.0) }
        assertFailsWith<IllegalArgumentException> { CarProfile(6.0, rangeDisplayStepKm = 0.0) }
    }
}
