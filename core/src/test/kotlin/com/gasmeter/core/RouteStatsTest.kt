package com.gasmeter.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class RouteStatsTest {
    private val tolerance = 1e-9

    @Test
    fun `empty route`() {
        val stats = RouteStats.from(emptyList())
        assertEquals(0, stats.tripCount)
        assertNull(stats.medianRatio)
        assertNull(stats.meanRatio)
        assertNull(stats.bestTrip)
        assertNull(stats.totalCost)
        assertNull(stats.trend)
    }

    @Test
    fun `basic route statistics`() {
        val a = trip(distanceKm = 10.0, rangeUsedKm = 12.0, day = 0) // ratio 1.2
        val b = trip(distanceKm = 10.0, rangeUsedKm = 10.0, day = 1) // ratio 1.0
        val c = trip(distanceKm = 10.0, rangeUsedKm = 20.0, day = 2) // ratio 2.0
        val stats = RouteStats.from(listOf(c, a, b))

        assertEquals(3, stats.tripCount)
        assertEquals(1.2, stats.medianRatio!!, tolerance)
        assertEquals(1.4, stats.meanRatio!!, tolerance)
        assertSame(b, stats.bestTrip)
        assertSame(c, stats.worstTrip)
        assertEquals(30.0, stats.totalDistanceKm, tolerance)
        assertEquals(42.0, stats.totalRangeUsedKm, tolerance)
        assertEquals(12.0, stats.totalExcessKm, tolerance)
        // 42 range-km x 0.06 L x 2.0/L = 5.04
        assertEquals(5.04, stats.totalCost!!, tolerance)
        assertEquals(1.68, stats.averageCost!!, tolerance)
        // 12 excess km x 0.06 x 2.0 = 1.44
        assertEquals(1.44, stats.totalExcessCost!!, tolerance)
    }

    @Test
    fun `refuelled and excluded trips are ignored but counted as ignored`() {
        val stats = RouteStats.from(
            listOf(
                tripWithRatio(1.0),
                tripWithRatio(5.0, refuelled = true),
                tripWithRatio(5.0, excluded = true),
            ),
        )
        assertEquals(1, stats.tripCount)
        assertEquals(2, stats.ignoredTripCount)
        assertEquals(1.0, stats.medianRatio!!, tolerance)
        assertEquals(10.0, stats.totalDistanceKm, tolerance)
    }

    @Test
    fun `idle trips add to totals but not to ratios`() {
        val stats = RouteStats.from(listOf(tripWithRatio(1.5), trip(distanceKm = 0.0, rangeUsedKm = 2.0)))
        assertEquals(2, stats.tripCount)
        assertEquals(1.5, stats.medianRatio!!, tolerance)
        assertEquals(17.0, stats.totalRangeUsedKm, tolerance)
    }

    @Test
    fun `trip where range went up is never the best trip but stays in the median`() {
        val noise = tripWithRatio(-0.4, day = 0)
        val good = tripWithRatio(1.1, day = 1)
        val bad = tripWithRatio(1.5, day = 2)
        val stats = RouteStats.from(listOf(noise, good, bad))

        assertSame(good, stats.bestTrip)
        assertSame(bad, stats.worstTrip)
        assertEquals(1.1, stats.medianRatio!!, tolerance)
    }

    @Test
    fun `trend needs more trips than the window`() {
        val five = (0L until 5L).map { tripWithRatio(1.0, day = it) }
        assertNull(RouteStats.from(five).trend)
    }

    @Test
    fun `trend compares the latest five trips to the whole history`() {
        val old = (0L until 5L).map { tripWithRatio(2.0, day = it) }
        val recent = (5L until 10L).map { tripWithRatio(1.0, day = it) }
        // Shuffled input: the trend must use endedAt order, not list order.
        val trend = RouteStats.from((recent + old).shuffled(kotlin.random.Random(1))).trend!!

        assertEquals(1.0, trend.recentMedianRatio, tolerance)
        assertEquals(1.5, trend.overallMedianRatio, tolerance)
        assertEquals(-1.0 / 3.0, trend.relativeChange, tolerance)
        assertEquals(Trend.Direction.IMPROVING, trend.direction)
    }

    @Test
    fun `trend direction bands`() {
        assertEquals(Trend.Direction.STABLE, Trend(1.04, 1.0, 5).direction)
        assertEquals(Trend.Direction.WORSENING, Trend(1.1, 1.0, 5).direction)
    }

    @Test
    fun `median of even count averages the middle values`() {
        assertEquals(2.5, Statistics.median(listOf(4.0, 1.0, 3.0, 2.0))!!, tolerance)
        assertNull(Statistics.median(emptyList()))
    }
}
