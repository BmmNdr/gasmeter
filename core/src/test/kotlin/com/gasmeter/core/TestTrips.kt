package com.gasmeter.core

import java.time.Instant

/** 6 L/100 km, range shown in 1 km steps. */
val testCar = CarProfile(consumptionLitresPer100Km = 6.0, rangeDisplayStepKm = 1.0)

/** Builds a trip with the given distance and range used, starting from 300 km range. */
fun trip(
    distanceKm: Double,
    rangeUsedKm: Double,
    day: Long = 0,
    price: Double? = 2.0,
    refuelled: Boolean = false,
    excluded: Boolean = false,
): RecordedTrip {
    val readings = TripReadings(
        rangeStartKm = 300.0,
        rangeEndKm = 300.0 - rangeUsedKm,
        odometerStartKm = 10_000.0,
        odometerEndKm = 10_000.0 + distanceKm,
    )
    return RecordedTrip(
        endedAt = Instant.parse("2026-01-01T08:00:00Z").plusSeconds(day * 86_400),
        metrics = TripCalculator.calculate(readings, testCar, price),
        refuelledDuringTrip = refuelled,
        excludedFromStats = excluded,
    )
}

/** A trip with the given ratio over 10 km. */
fun tripWithRatio(ratio: Double, day: Long = 0, refuelled: Boolean = false, excluded: Boolean = false) =
    trip(distanceKm = 10.0, rangeUsedKm = 10.0 * ratio, day = day, refuelled = refuelled, excluded = excluded)
