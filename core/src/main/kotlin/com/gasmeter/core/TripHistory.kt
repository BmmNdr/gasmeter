package com.gasmeter.core

import java.time.Instant
import java.time.ZoneId

/** Optional context the user can attach to a trip to explain its consumption. */
enum class TripTag {
    HEAVY_TRAFFIC,
    CLIMATE_CONTROL,
    COLD_ENGINE,
    RAIN,
    HEAVY_LOAD,
    HIGHWAY,
}

/** A completed trip as stored by the app: raw readings plus the user's flags. */
data class TripLog(
    val id: Long,
    val routeId: Long?,
    val startedAt: Instant,
    val endedAt: Instant,
    val readings: TripReadings,
    val refuelledDuringTrip: Boolean = false,
    val excludedFromStats: Boolean = false,
    val tags: Set<TripTag> = emptySet(),
    val note: String? = null,
)

/**
 * A logged trip with its computed metrics and verdict.
 *
 * @property verdict null when the trip has no route, since there is nothing to
 *   compare it with.
 */
data class AnalyzedTrip(
    val log: TripLog,
    val trip: RecordedTrip,
    val verdict: Verdict?,
)

object TripHistoryAnalyzer {

    /**
     * Computes metrics for every trip and judges each one against the earlier
     * trips on the same route. Each trip is costed at the fuel price valid on the
     * day it started, in [zone]. Trips whose readings have blocking errors are
     * skipped rather than crashing the whole history.
     *
     * @return trips sorted by end time, oldest first.
     */
    fun analyze(
        logs: List<TripLog>,
        car: CarProfile,
        prices: FuelPriceHistory,
        zone: ZoneId,
    ): List<AnalyzedTrip> {
        val earlierByRoute = mutableMapOf<Long, MutableList<RecordedTrip>>()

        return logs
            .filter { log -> TripValidator.validate(log.readings, car).none { it.isBlocking } }
            .sortedWith(compareBy<TripLog> { it.endedAt }.thenBy { it.id })
            .map { log ->
                val price = prices.priceOn(log.startedAt.atZone(zone).toLocalDate())
                val recorded = RecordedTrip(
                    endedAt = log.endedAt,
                    metrics = TripCalculator.calculate(log.readings, car, price),
                    refuelledDuringTrip = log.refuelledDuringTrip,
                    excludedFromStats = log.excludedFromStats,
                )
                val verdict = log.routeId?.let { routeId ->
                    val earlier = earlierByRoute.getOrPut(routeId) { mutableListOf() }
                    VerdictRules.judge(recorded, earlier.toList()).also { earlier += recorded }
                }
                AnalyzedTrip(log, recorded, verdict)
            }
    }
}
