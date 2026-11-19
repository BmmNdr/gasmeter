package com.gasmeter.core

/** A problem with a trip's readings. Blocking issues must be fixed before saving. */
sealed interface TripIssue {
    val isBlocking: Boolean

    /** A reading below zero, almost certainly a typo. */
    data object NegativeReading : TripIssue {
        override val isBlocking = true
    }

    /** The end odometer is lower than the start odometer. */
    data object OdometerWentBackwards : TripIssue {
        override val isBlocking = true
    }

    /** A very long distance for a daily trip; probably a typo, but allowed. */
    data class ImplausibleDistance(val distanceKm: Double) : TripIssue {
        override val isBlocking = false
    }

    /**
     * The range went up a lot during the trip, which usually means a refuel.
     * The app should ask whether to mark the trip as refuelled.
     */
    data class PossibleRefuel(val rangeGainKm: Double) : TripIssue {
        override val isBlocking = false
    }
}

object TripValidator {
    /** Distances above this get an [TripIssue.ImplausibleDistance] warning. */
    const val MAX_PLAUSIBLE_DISTANCE_KM = 1500.0

    /** A range gain above this (or 3 display steps, if larger) suggests a refuel. */
    const val MIN_REFUEL_GAIN_KM = 30.0

    fun validate(readings: TripReadings, car: CarProfile): List<TripIssue> {
        val issues = mutableListOf<TripIssue>()
        val values = with(readings) { listOf(rangeStartKm, rangeEndKm, odometerStartKm, odometerEndKm) }
        if (values.any { it < 0 }) issues += TripIssue.NegativeReading

        val distance = readings.odometerEndKm - readings.odometerStartKm
        if (distance < 0) issues += TripIssue.OdometerWentBackwards
        if (distance > MAX_PLAUSIBLE_DISTANCE_KM) issues += TripIssue.ImplausibleDistance(distance)

        val rangeGain = readings.rangeEndKm - readings.rangeStartKm
        val refuelThreshold = maxOf(MIN_REFUEL_GAIN_KM, 3 * car.rangeDisplayStepKm)
        if (rangeGain > refuelThreshold) issues += TripIssue.PossibleRefuel(rangeGain)

        return issues
    }
}
