package com.gasmeter.core

/** Dashboard readings taken at the start and end of a trip. All values in km. */
data class TripReadings(
    val rangeStartKm: Double,
    val rangeEndKm: Double,
    val odometerStartKm: Double,
    val odometerEndKm: Double,
)

/**
 * The car settings needed to turn range-km into fuel and money.
 *
 * @param consumptionLitresPer100Km the car's average consumption.
 * @param rangeDisplayStepKm how coarsely the dashboard rounds the range (1, 5, 10 km...).
 */
data class CarProfile(
    val consumptionLitresPer100Km: Double,
    val rangeDisplayStepKm: Double = 1.0,
) {
    init {
        require(consumptionLitresPer100Km > 0) { "Consumption must be positive" }
        require(rangeDisplayStepKm > 0) { "Range display step must be positive" }
    }
}

/**
 * Everything derived from one trip's readings.
 *
 * @property excessKm range used beyond the distance driven. Negative means the trip
 *   used less than the car's own estimate.
 * @property ratio range used per km driven. 1.0 means the trip matched the car's
 *   estimate. Null for an idle trip (distance 0), where a ratio is meaningless.
 * @property cost null when no fuel price is known.
 * @property rangeUncertaintyKm how far off [rangeUsedKm] can be because of the
 *   dashboard rounding (both readings are rounded, so up to one display step).
 * @property tooShortToJudge true when the rounding error is large compared to the
 *   distance, so this trip on its own says little.
 */
data class TripMetrics(
    val distanceKm: Double,
    val rangeUsedKm: Double,
    val excessKm: Double,
    val ratio: Double?,
    val litres: Double,
    val cost: Double?,
    val excessCost: Double?,
    val rangeUncertaintyKm: Double,
    val tooShortToJudge: Boolean,
) {
    val isIdle: Boolean get() = distanceKm == 0.0
}

object TripCalculator {

    /**
     * Computes the metrics for a trip. Run [TripValidator.validate] first: this
     * throws if the readings contain a blocking error.
     */
    fun calculate(
        readings: TripReadings,
        car: CarProfile,
        fuelPricePerLitre: Double?,
    ): TripMetrics {
        val blocking = TripValidator.validate(readings, car).filter { it.isBlocking }
        require(blocking.isEmpty()) { "Invalid trip readings: $blocking" }

        val distance = readings.odometerEndKm - readings.odometerStartKm
        val rangeUsed = readings.rangeStartKm - readings.rangeEndKm
        val excess = rangeUsed - distance
        val litresPerRangeKm = car.consumptionLitresPer100Km / 100.0

        return TripMetrics(
            distanceKm = distance,
            rangeUsedKm = rangeUsed,
            excessKm = excess,
            ratio = if (distance > 0) rangeUsed / distance else null,
            litres = rangeUsed * litresPerRangeKm,
            cost = fuelPricePerLitre?.let { rangeUsed * litresPerRangeKm * it },
            excessCost = fuelPricePerLitre?.let { excess * litresPerRangeKm * it },
            rangeUncertaintyKm = car.rangeDisplayStepKm,
            tooShortToJudge = distance < 2 * car.rangeDisplayStepKm,
        )
    }
}
