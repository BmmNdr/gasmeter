package com.gasmeter.core

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteBreakdownTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private var nextId = 1L

    /** A trip over 10 km starting at [time] (UTC, "HH:MM") on day [day]. */
    private fun analyzed(
        ratio: Double,
        time: String = "08:00",
        day: Long = 0,
        tags: Set<TripTag> = emptySet(),
        excluded: Boolean = false,
        idle: Boolean = false,
    ): AnalyzedTrip {
        val start = Instant.parse("2026-01-01T${time}:00Z").plusSeconds(day * 86_400)
        val distance = if (idle) 0.0 else 10.0
        val readings = TripReadings(300.0, 300.0 - 10.0 * ratio, 1000.0, 1000.0 + distance)
        val log = TripLog(nextId++, 1, start, start.plusSeconds(600), readings, excludedFromStats = excluded, tags = tags)
        val recorded = RecordedTrip(log.endedAt, TripCalculator.calculate(readings, testCar, null), excludedFromStats = excluded)
        return AnalyzedTrip(log, recorded, verdict = null)
    }

    @Test
    fun `tag comparison needs two trips on each side`() {
        val traffic = setOf(TripTag.HEAVY_TRAFFIC)
        val trips = listOf(
            analyzed(1.8, tags = traffic),
            analyzed(2.0, tags = traffic),
            analyzed(1.0),
            analyzed(1.2),
            analyzed(1.1),
            analyzed(1.5, tags = setOf(TripTag.RAIN)), // only one rainy trip
        )
        val breakdown = RouteBreakdown.from(trips, utc)

        val comparison = breakdown.byTag.single()
        assertEquals(TripTag.HEAVY_TRAFFIC, comparison.tag)
        assertEquals(1.9, comparison.medianWith, 1e-9)
        assertEquals(2, comparison.tripsWith)
        assertEquals(1.15, comparison.medianWithout, 1e-9) // 1.0, 1.1, 1.2, 1.5
        assertEquals(4, comparison.tripsWithout)
        assertEquals(1.9 / 1.15 - 1, comparison.relativeChange, 1e-9)
    }

    @Test
    fun `excluded and idle trips are ignored`() {
        val traffic = setOf(TripTag.HEAVY_TRAFFIC)
        val trips = listOf(
            analyzed(1.8, tags = traffic),
            analyzed(9.0, tags = traffic, excluded = true),
            analyzed(1.0, tags = traffic, idle = true),
            analyzed(1.0),
            analyzed(1.2),
        )
        val breakdown = RouteBreakdown.from(trips, utc)
        assertTrue(breakdown.byTag.isEmpty(), "only one comparable trip has the tag")
    }

    @Test
    fun `time of day groups with at least two trips`() {
        val trips = listOf(
            analyzed(1.0, "07:30"),
            analyzed(1.2, "09:59"),
            analyzed(1.6, "17:45"),
            analyzed(1.8, "18:10"),
            analyzed(1.4, "12:00"), // midday: only one trip
        )
        val breakdown = RouteBreakdown.from(trips, utc)

        assertEquals(
            listOf(
                TimeOfDayGroup(TimeOfDay.MORNING, 1.1, 2),
                TimeOfDayGroup(TimeOfDay.EVENING, 1.7, 2),
            ),
            breakdown.byTimeOfDay.map { it.copy(medianRatio = Math.round(it.medianRatio * 100) / 100.0) },
        )
    }

    @Test
    fun `slot boundaries`() {
        fun slot(time: String) = TimeOfDay.of(Instant.parse("2026-01-01T${time}:00Z"), utc)
        assertEquals(TimeOfDay.NIGHT, slot("04:59"))
        assertEquals(TimeOfDay.MORNING, slot("05:00"))
        assertEquals(TimeOfDay.MORNING, slot("09:59"))
        assertEquals(TimeOfDay.MIDDAY, slot("10:00"))
        assertEquals(TimeOfDay.MIDDAY, slot("15:59"))
        assertEquals(TimeOfDay.EVENING, slot("16:00"))
        assertEquals(TimeOfDay.EVENING, slot("20:59"))
        assertEquals(TimeOfDay.NIGHT, slot("21:00"))
        assertEquals(TimeOfDay.NIGHT, slot("00:00"))
    }

    @Test
    fun `slots follow the given time zone`() {
        // 06:00 UTC is 08:00 in UTC+2 (morning) but 01:00 in UTC-5 (night).
        val instant = Instant.parse("2026-01-01T06:00:00Z")
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(instant, ZoneOffset.ofHours(2)))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(instant, ZoneOffset.ofHours(-5)))
    }

    @Test
    fun `empty route gives empty breakdown`() {
        assertTrue(RouteBreakdown.from(emptyList(), utc).isEmpty)
    }
}
