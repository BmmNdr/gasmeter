package com.gasmeter.core

enum class Rating { EFFICIENT, NORMAL, EXPENSIVE }

sealed interface Verdict {
    /**
     * @property relativeToMedian this trip's ratio divided by the route's median
     *   ratio; 1.2 means 20% more fuel than usual for this route.
     */
    data class Judged(
        val rating: Rating,
        val relativeToMedian: Double,
        val routeMedianRatio: Double,
    ) : Verdict

    data class NotEnoughHistory(val previousTrips: Int, val required: Int) : Verdict

    data class NotJudgeable(val reason: Reason) : Verdict

    enum class Reason {
        /** Distance was 0, so there is no ratio. */
        IDLE,

        /** The range went up during the trip; rounding noise, nothing to judge. */
        RANGE_INCREASED,

        /** The trip was refuelled during the drive or excluded from stats. */
        NOT_COUNTED,

        /** The route's median ratio is zero or negative, so it's no baseline. */
        NO_USABLE_BASELINE,
    }
}

/**
 * Judges a trip against the same route's earlier trips.
 *
 * A trip is NORMAL when its ratio is within ±15% of the route's median ratio
 * (both bounds inclusive), EFFICIENT below that and EXPENSIVE above it. A verdict
 * needs at least [MIN_PREVIOUS_TRIPS] earlier counted trips, so the first one
 * appears on the route's 4th trip.
 */
object VerdictRules {
    const val MIN_PREVIOUS_TRIPS = 3
    const val NORMAL_LOWER_BOUND = 0.85
    const val NORMAL_UPPER_BOUND = 1.15

    /** Absorbs floating-point error at the bounds (2.3 / 2.0 is not exactly 1.15). */
    private const val EPSILON = 1e-9

    fun judge(current: RecordedTrip, previousTrips: List<RecordedTrip>): Verdict {
        if (!current.countsForStats) return Verdict.NotJudgeable(Verdict.Reason.NOT_COUNTED)
        val ratio = current.metrics.ratio
            ?: return Verdict.NotJudgeable(Verdict.Reason.IDLE)
        if (ratio < 0) return Verdict.NotJudgeable(Verdict.Reason.RANGE_INCREASED)

        val history = previousTrips.mapNotNull { it.comparableRatio }
        if (history.size < MIN_PREVIOUS_TRIPS) {
            return Verdict.NotEnoughHistory(history.size, MIN_PREVIOUS_TRIPS)
        }

        val median = Statistics.median(history)!!
        if (median <= 0) return Verdict.NotJudgeable(Verdict.Reason.NO_USABLE_BASELINE)

        val relative = ratio / median
        val rating = when {
            relative < NORMAL_LOWER_BOUND - EPSILON -> Rating.EFFICIENT
            relative > NORMAL_UPPER_BOUND + EPSILON -> Rating.EXPENSIVE
            else -> Rating.NORMAL
        }
        return Verdict.Judged(rating, relative, median)
    }
}
