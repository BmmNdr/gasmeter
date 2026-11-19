package com.gasmeter.core

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class FuelPriceHistoryTest {
    private val history = FuelPriceHistory(
        listOf(
            FuelPrice(LocalDate.of(2026, 3, 1), 1.90),
            FuelPrice(LocalDate.of(2026, 1, 1), 1.80),
        ),
    )

    @Test
    fun `uses the price in effect on the trip date`() {
        assertEquals(1.80, history.priceOn(LocalDate.of(2026, 2, 15)))
        assertEquals(1.90, history.priceOn(LocalDate.of(2026, 3, 1)))
        assertEquals(1.90, history.priceOn(LocalDate.of(2026, 12, 31)))
    }

    @Test
    fun `dates before the first price use the earliest price`() {
        assertEquals(1.80, history.priceOn(LocalDate.of(2025, 6, 1)))
    }

    @Test
    fun `no prices means no price`() {
        assertNull(FuelPriceHistory(emptyList()).priceOn(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `price must be positive`() {
        assertFailsWith<IllegalArgumentException> { FuelPrice(LocalDate.of(2026, 1, 1), 0.0) }
    }
}
