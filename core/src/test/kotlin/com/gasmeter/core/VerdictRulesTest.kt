package com.gasmeter.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class VerdictRulesTest {
    /** Three earlier trips with median ratio 2.0. */
    private val history = listOf(tripWithRatio(1.8), tripWithRatio(2.0), tripWithRatio(2.4))

    private fun ratingFor(ratio: Double): Rating {
        val verdict = VerdictRules.judge(tripWithRatio(ratio), history)
        return assertIs<Verdict.Judged>(verdict).rating
    }

    @Test
    fun `no verdict before three previous trips`() {
        val verdict = VerdictRules.judge(tripWithRatio(2.0), history.take(2))
        assertEquals(Verdict.NotEnoughHistory(previousTrips = 2, required = 3), verdict)
    }

    @Test
    fun `verdict appears with three previous trips`() {
        val verdict = assertIs<Verdict.Judged>(VerdictRules.judge(tripWithRatio(2.0), history))
        assertEquals(Rating.NORMAL, verdict.rating)
        assertEquals(2.0, verdict.routeMedianRatio, 1e-9)
        assertEquals(1.0, verdict.relativeToMedian, 1e-9)
    }

    @Test
    fun `ratings around the plus or minus 15 percent band`() {
        assertEquals(Rating.EFFICIENT, ratingFor(1.69))
        assertEquals(Rating.NORMAL, ratingFor(1.7)) // exactly 0.85, inclusive
        assertEquals(Rating.NORMAL, ratingFor(2.3)) // exactly 1.15, inclusive despite float error
        assertEquals(Rating.EXPENSIVE, ratingFor(2.31))
    }

    @Test
    fun `refuelled and excluded history trips are not counted`() {
        val mixed = listOf(
            tripWithRatio(2.0),
            tripWithRatio(2.0),
            tripWithRatio(9.0, refuelled = true),
            tripWithRatio(9.0, excluded = true),
        )
        assertEquals(Verdict.NotEnoughHistory(2, 3), VerdictRules.judge(tripWithRatio(2.0), mixed))
    }

    @Test
    fun `idle history trips are not counted`() {
        val withIdle = history.take(2) + trip(distanceKm = 0.0, rangeUsedKm = 1.0)
        assertEquals(Verdict.NotEnoughHistory(2, 3), VerdictRules.judge(tripWithRatio(2.0), withIdle))
    }

    @Test
    fun `idle trip is not judgeable`() {
        val verdict = VerdictRules.judge(trip(distanceKm = 0.0, rangeUsedKm = 1.0), history)
        assertEquals(Verdict.NotJudgeable(Verdict.Reason.IDLE), verdict)
    }

    @Test
    fun `trip where range went up is not judgeable`() {
        val verdict = VerdictRules.judge(tripWithRatio(-0.2), history)
        assertEquals(Verdict.NotJudgeable(Verdict.Reason.RANGE_INCREASED), verdict)
    }

    @Test
    fun `refuelled current trip is not judgeable`() {
        val verdict = VerdictRules.judge(tripWithRatio(2.0, refuelled = true), history)
        assertEquals(Verdict.NotJudgeable(Verdict.Reason.NOT_COUNTED), verdict)
    }

    @Test
    fun `non-positive route median gives no verdict`() {
        val weird = listOf(tripWithRatio(-0.5), tripWithRatio(0.0), tripWithRatio(0.5))
        val verdict = VerdictRules.judge(tripWithRatio(1.0), weird)
        assertEquals(Verdict.NotJudgeable(Verdict.Reason.NO_USABLE_BASELINE), verdict)
    }
}
