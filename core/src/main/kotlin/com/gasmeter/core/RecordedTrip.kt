package com.gasmeter.core

import java.time.Instant

/** A finished trip as used for comparisons and statistics. */
data class RecordedTrip(
    val endedAt: Instant,
    val metrics: TripMetrics,
    val refuelledDuringTrip: Boolean = false,
    val excludedFromStats: Boolean = false,
) {
    /** Refuelled or manually excluded trips are kept but never counted. */
    val countsForStats: Boolean get() = !refuelledDuringTrip && !excludedFromStats

    /** The ratio to use in comparisons, or null if this trip can't be compared. */
    val comparableRatio: Double? get() = if (countsForStats) metrics.ratio else null
}
