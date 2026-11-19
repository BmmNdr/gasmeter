package com.gasmeter.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DecimalInputTest {
    private fun assertParses(expected: Double, text: String) =
        assertEquals("parsing '$text'", expected, parseDecimal(text)!!, 1e-9)

    @Test
    fun plainNumbers() {
        assertParses(300.0, "300")
        assertParses(300.0, " 300 ")
        assertParses(12.5, "12.5")
    }

    @Test
    fun commaAsDecimalSeparator() {
        assertParses(12.5, "12,5")
        assertParses(0.5, ",5")
    }

    @Test
    fun thousandsSeparators() {
        assertParses(12345.6, "12.345,6")
        assertParses(12345.6, "12,345.6")
        assertParses(1234567.0, "1.234.567")
        assertParses(1234567.0, "1,234,567")
        assertParses(12345.0, "12 345")
        assertParses(12345.0, "12 345")
    }

    @Test
    fun negativeNumbersParseSoValidationCanRejectThem() {
        assertParses(-5.0, "-5")
    }

    @Test
    fun invalidInput() {
        assertNull(parseDecimal(""))
        assertNull(parseDecimal("   "))
        assertNull(parseDecimal("abc"))
        assertNull(parseDecimal("12km"))
        assertNull(parseDecimal("NaN"))
        assertNull(parseDecimal("1e5"))
        assertNull(parseDecimal("."))
    }
}
