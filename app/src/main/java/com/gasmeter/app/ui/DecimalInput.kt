package com.gasmeter.app.ui

/**
 * Parses a number typed by the user, accepting both "12.5" and "12,5".
 *
 * - With both separators present, the last one is the decimal separator and the
 *   other one is a thousands separator ("12.345,6" and "12,345.6" are 12345.6).
 * - A separator that appears more than once is a thousands separator ("1.234.567").
 * - A single separator is a decimal separator ("12,5" is 12.5).
 *
 * Returns null for empty or unparseable text.
 */
fun parseDecimal(text: String): Double? {
    val compact = text.trim().filterNot { it == ' ' || it == ' ' || it == ' ' || it == '\'' }
    if (compact.isEmpty()) return null

    val lastDot = compact.lastIndexOf('.')
    val lastComma = compact.lastIndexOf(',')
    val normalized = when {
        lastDot >= 0 && lastComma >= 0 -> {
            val decimal = if (lastDot > lastComma) '.' else ','
            val thousands = if (decimal == '.') ',' else '.'
            compact.replace(thousands.toString(), "").replace(decimal, '.')
        }
        lastDot >= 0 || lastComma >= 0 -> {
            val separator = if (lastDot >= 0) '.' else ','
            if (compact.count { it == separator } > 1) {
                compact.replace(separator.toString(), "")
            } else {
                compact.replace(separator, '.')
            }
        }
        else -> compact
    }
    if (!normalized.all { it.isDigit() || it == '.' || it == '-' }) return null
    return normalized.toDoubleOrNull()?.takeIf { it.isFinite() }
}
