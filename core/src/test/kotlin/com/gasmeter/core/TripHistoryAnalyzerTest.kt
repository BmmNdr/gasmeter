package com.gasmeter.core

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TripHistoryAnalyzerTest {
    private val utc: ZoneId = ZoneOffset.UTC
    private val noPrices = FuelPriceHistory(emptyList())

    private fun log(
        id: Long,
        routeId: Long?,
        day: Long,
        distance: Double,
        rangeUsed: Double,
        refuelled: Boolean = false,
    ): TripLog {
        val start = Instant.parse("2026-01-01T08:00:00Z").plusSeconds(day * 86_400)
        return TripLog(
            id = id,
            routeId = routeId,
            startedAt = start,
            endedAt = start.plusSeconds(900),
            readings = TripReadings(300.0, 300.0 - rangeUsed, 1000.0, 1000.0 + distance),
            refuelledDuringTrip = refuelled,
        )
    }

    @Test
    fun `verdict compares only with earlier trips on the same route`() {
        val logs = listOf(
            log(1, routeId = 1, day = 0, distance = 10.0, rangeUsed = 10.0),
            log(2, routeId = 1, day = 1, distance = 10.0, rangeUsed = 10.0),
            log(3, routeId = 2, day = 2, distance = 10.0, rangeUsed = 30.0), // other route
            log(4, routeId = 1, day = 3, distance = 10.0, rangeUsed = 10.0),
            log(5, routeId = 1, day = 4, distance = 10.0, rangeUsed = 13.0), // 1.3x vs median 1.0
        )
        val result = TripHistoryAnalyzer.analyze(logs.shuffled(kotlin.random.Random(3)), testCar, noPrices, utc)

        assertEquals(listOf(1L, 2L, 3L, 4L, 5L), result.map { it.log.id })
        assertEquals(Verdict.NotEnoughHistory(0, 3), result[0].verdict)
        assertEquals(Verdict.NotEnoughHistory(2, 3), result[3].verdict)
        assertEquals(Verdict.NotEnoughHistory(0, 3), result[2].verdict)
        val judged = assertIs<Verdict.Judged>(result[4].verdict)
        assertEquals(Rating.EXPENSIVE, judged.rating)
    }

    @Test
    fun `trips without a route get no verdict`() {
        val result = TripHistoryAnalyzer.analyze(listOf(log(1, null, 0, 2.0, 5.0)), testCar, noPrices, utc)
        assertNull(result.single().verdict)
        assertEquals(3.0, result.single().trip.metrics.excessKm, 1e-9)
    }

    @Test
    fun `refuelled trips do not count as history`() {
        val logs = listOf(
            log(1, 1, 0, 10.0, 10.0),
            log(2, 1, 1, 10.0, 10.0),
            log(3, 1, 2, 10.0, 10.0, refuelled = true),
            log(4, 1, 3, 10.0, 10.0),
        )
        val result = TripHistoryAnalyzer.analyze(logs, testCar, noPrices, utc)
        assertEquals(Verdict.NotJudgeable(Verdict.Reason.NOT_COUNTED), result[2].verdict)
        assertEquals(Verdict.NotEnoughHistory(2, 3), result[3].verdict)
    }

    @Test
    fun `each trip uses the fuel price of its start day`() {
        val prices = FuelPriceHistory(
            listOf(
                FuelPrice(LocalDate.of(2026, 1, 1), 1.0),
                FuelPrice(LocalDate.of(2026, 1, 2), 2.0),
            ),
        )
        val logs = listOf(log(1, null, 0, 10.0, 10.0), log(2, null, 1, 10.0, 10.0))
        val result = TripHistoryAnalyzer.analyze(logs, testCar, prices, utc)
        // 10 range-km x 0.06 L = 0.6 L
        assertEquals(0.6, result[0].trip.metrics.cost!!, 1e-9)
        assertEquals(1.2, result[1].trip.metrics.cost!!, 1e-9)
    }

    @Test
    fun `trips with blocking errors are skipped`() {
        val broken = log(1, 1, 0, 10.0, 10.0).let {
            it.copy(readings = it.readings.copy(odometerEndKm = 900.0))
        }
        val result = TripHistoryAnalyzer.analyze(listOf(broken, log(2, 1, 1, 10.0, 10.0)), testCar, noPrices, utc)
        assertEquals(listOf(2L), result.map { it.log.id })
    }
}
