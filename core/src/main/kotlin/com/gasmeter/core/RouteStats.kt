package com.gasmeter.core

/** How the latest trips on a route compare to the route's whole history. */
data class Trend(
    val recentMedianRatio: Double,
    val overallMedianRatio: Double,
    val recentTripCount: Int,
) {
    /** -0.1 means recent trips use 10% less range per km than usual. */
    val relativeChange: Double get() = recentMedianRatio / overallMedianRatio - 1

    val direction: Direction
        get() = when {
            relativeChange < -STABLE_BAND -> Direction.IMPROVING
            relativeChange > STABLE_BAND -> Direction.WORSENING
            else -> Direction.STABLE
        }

    enum class Direction { IMPROVING, STABLE, WORSENING }

    companion object {
        /** Changes within ±5% count as stable. */
        const val STABLE_BAND = 0.05
    }
}

/**
 * Statistics for one route. Refuelled and excluded trips are left out of
 * everything except [ignoredTripCount]. Idle trips (distance 0) count towards
 * totals and costs but have no ratio.
 *
 * Trips where the range went up (negative ratio) are rounding noise: they stay
 * in the median and mean, where rounding errors cancel out, but can never be
 * [bestTrip] or [worstTrip].
 */
data class RouteStats(
    val tripCount: Int,
    val ignoredTripCount: Int,
    val medianRatio: Double?,
    val meanRatio: Double?,
    val bestTrip: RecordedTrip?,
    val worstTrip: RecordedTrip?,
    val totalDistanceKm: Double,
    val totalRangeUsedKm: Double,
    val totalExcessKm: Double,
    val totalCost: Double?,
    val averageCost: Double?,
    val totalExcessCost: Double?,
    val trend: Trend?,
) {
    companion object {
        /** Number of latest trips compared against the whole history for [trend]. */
        const val TREND_WINDOW = 5

        fun from(trips: List<RecordedTrip>): RouteStats {
            val counted = trips.filter { it.countsForStats }.sortedBy { it.endedAt }
            val withRatio = counted.filter { it.metrics.ratio != null }
            val ratios = withRatio.map { it.metrics.ratio!! }
            val rankable = withRatio.filter { it.metrics.ratio!! >= 0 }
            val costs = counted.mapNotNull { it.metrics.cost }
            val excessCosts = counted.mapNotNull { it.metrics.excessCost }

            val overallMedian = Statistics.median(ratios)
            val trend = if (ratios.size > TREND_WINDOW && overallMedian != null && overallMedian > 0) {
                Trend(
                    recentMedianRatio = Statistics.median(ratios.takeLast(TREND_WINDOW))!!,
                    overallMedianRatio = overallMedian,
                    recentTripCount = TREND_WINDOW,
                )
            } else {
                null
            }

            return RouteStats(
                tripCount = counted.size,
                ignoredTripCount = trips.size - counted.size,
                medianRatio = overallMedian,
                meanRatio = ratios.takeIf { it.isNotEmpty() }?.average(),
                bestTrip = rankable.minByOrNull { it.metrics.ratio!! },
                worstTrip = rankable.maxByOrNull { it.metrics.ratio!! },
                totalDistanceKm = counted.sumOf { it.metrics.distanceKm },
                totalRangeUsedKm = counted.sumOf { it.metrics.rangeUsedKm },
                totalExcessKm = counted.sumOf { it.metrics.excessKm },
                totalCost = costs.takeIf { it.isNotEmpty() }?.sum(),
                averageCost = costs.takeIf { it.isNotEmpty() }?.average(),
                totalExcessCost = excessCosts.takeIf { it.isNotEmpty() }?.sum(),
                trend = trend,
            )
        }
    }
}
