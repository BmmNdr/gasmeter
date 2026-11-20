package com.gasmeter.core

import java.time.Instant
import java.time.ZoneId

/** Part of the day a trip started in, in the phone's time zone. */
enum class TimeOfDay {
    /** 05:00–09:59 */
    MORNING,

    /** 10:00–15:59 */
    MIDDAY,

    /** 16:00–20:59 */
    EVENING,

    /** 21:00–04:59 */
    NIGHT,
    ;

    companion object {
        fun of(instant: Instant, zone: ZoneId): TimeOfDay = when (instant.atZone(zone).hour) {
            in 5..9 -> MORNING
            in 10..15 -> MIDDAY
            in 16..20 -> EVENING
            else -> NIGHT
        }
    }
}

/** Median ratio of the route's trips with [tag] compared to those without it. */
data class TagComparison(
    val tag: TripTag,
    val medianWith: Double,
    val tripsWith: Int,
    val medianWithout: Double,
    val tripsWithout: Int,
) {
    /** 0.5 means trips with the tag use 50% more range per km. */
    val relativeChange: Double get() = medianWith / medianWithout - 1
}

data class TimeOfDayGroup(val slot: TimeOfDay, val medianRatio: Double, val trips: Int)

/**
 * Explains a route's variation: how tags and the time of day relate to the
 * ratio. Uses only comparable trips (counted, with a distance). Groups with
 * fewer than [MIN_GROUP_SIZE] trips are left out, since one trip says little.
 */
data class RouteBreakdown(
    val byTag: List<TagComparison>,
    val byTimeOfDay: List<TimeOfDayGroup>,
) {
    val isEmpty: Boolean get() = byTag.isEmpty() && byTimeOfDay.isEmpty()

    companion object {
        const val MIN_GROUP_SIZE = 2

        fun from(trips: List<AnalyzedTrip>, zone: ZoneId): RouteBreakdown {
            val comparable = trips.mapNotNull { analyzed ->
                analyzed.trip.comparableRatio?.let { ratio -> analyzed to ratio }
            }

            val byTag = TripTag.entries.mapNotNull { tag ->
                val (with, without) = comparable.partition { (analyzed, _) -> tag in analyzed.log.tags }
                if (with.size < MIN_GROUP_SIZE || without.size < MIN_GROUP_SIZE) return@mapNotNull null
                TagComparison(
                    tag = tag,
                    medianWith = Statistics.median(with.map { it.second })!!,
                    tripsWith = with.size,
                    medianWithout = Statistics.median(without.map { it.second })!!,
                    tripsWithout = without.size,
                )
            }

            val bySlot = comparable.groupBy { (analyzed, _) -> TimeOfDay.of(analyzed.log.startedAt, zone) }
            val byTimeOfDay = TimeOfDay.entries.mapNotNull { slot ->
                val ratios = bySlot[slot]?.map { it.second } ?: return@mapNotNull null
                if (ratios.size < MIN_GROUP_SIZE) return@mapNotNull null
                TimeOfDayGroup(slot, Statistics.median(ratios)!!, ratios.size)
            }

            return RouteBreakdown(byTag, byTimeOfDay)
        }
    }
}
