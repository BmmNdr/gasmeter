package com.gasmeter.core

import java.time.LocalDate

data class FuelPrice(val effectiveFrom: LocalDate, val pricePerLitre: Double) {
    init {
        require(pricePerLitre > 0) { "Fuel price must be positive" }
    }
}

/** Fuel prices over time, so each trip is costed at the price valid on its date. */
class FuelPriceHistory(prices: List<FuelPrice>) {
    private val sorted = prices.sortedBy { it.effectiveFrom }

    /**
     * The price in effect on [date]. For a date before the first recorded price,
     * the earliest price is used. Null only when no price has been entered.
     */
    fun priceOn(date: LocalDate): Double? =
        (sorted.lastOrNull { !it.effectiveFrom.isAfter(date) } ?: sorted.firstOrNull())
            ?.pricePerLitre
}
