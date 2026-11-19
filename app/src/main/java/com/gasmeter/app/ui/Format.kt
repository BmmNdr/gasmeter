package com.gasmeter.app.ui

import com.gasmeter.core.Rating
import com.gasmeter.core.TripTag
import com.gasmeter.core.Verdict
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private fun locale(): Locale = Locale.getDefault()

/** "5 km" for whole numbers, "2.4 km" otherwise. */
fun formatKm(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    val text = if (rounded == rounded.toLong().toDouble()) {
        String.format(locale(), "%d", rounded.toLong())
    } else {
        String.format(locale(), "%.1f", rounded)
    }
    return "$text km"
}

/** Like [formatKm] but always with a sign, e.g. "+3 km". */
fun formatSignedKm(value: Double): String = (if (value > 0) "+" else "") + formatKm(value)

fun formatRatio(value: Double): String = String.format(locale(), "%.2f×", value)

fun formatLitres(value: Double): String = String.format(locale(), "%.2f L", value)

fun formatMoney(value: Double, currency: String?): String {
    val amount = String.format(locale(), "%.2f", value)
    return if (currency.isNullOrBlank()) amount else "$amount $currency"
}

fun formatPercent(fraction: Double): String = "${(abs(fraction) * 100).roundToInt()}%"

fun formatDateTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale())
        .format(instant.atZone(ZoneId.systemDefault()))

fun formatTime(instant: Instant): String =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(locale())
        .format(instant.atZone(ZoneId.systemDefault()))

fun TripTag.label(): String = when (this) {
    TripTag.HEAVY_TRAFFIC -> "Heavy traffic"
    TripTag.CLIMATE_CONTROL -> "AC / heating"
    TripTag.COLD_ENGINE -> "Cold engine"
    TripTag.RAIN -> "Rain"
    TripTag.HEAVY_LOAD -> "Load / passengers"
    TripTag.HIGHWAY -> "Highway"
}

/** Short label for chips in lists. */
fun Verdict?.shortLabel(): String? = when (this) {
    is Verdict.Judged -> when (rating) {
        Rating.EFFICIENT -> "Efficient"
        Rating.NORMAL -> "Normal"
        Rating.EXPENSIVE -> "Expensive"
    }
    else -> null
}

/** Headline and explanation for the result card. */
fun Verdict?.describe(): Pair<String, String> = when (this) {
    null -> "No route" to "Pick a route when starting a trip to compare it with your other runs."
    is Verdict.Judged -> {
        val change = relativeToMedian - 1
        when (rating) {
            Rating.EFFICIENT -> "Efficient" to "${formatPercent(change)} less range per km than usual on this route."
            Rating.NORMAL -> "Normal" to "Within 15% of your usual ${formatRatio(routeMedianRatio)} on this route."
            Rating.EXPENSIVE -> "Expensive" to "${formatPercent(change)} more range per km than usual on this route."
        }
    }
    is Verdict.NotEnoughHistory ->
        "Not enough history yet" to
            "A verdict needs $required earlier trips on this route; there ${if (previousTrips == 1) "is" else "are"} $previousTrips so far."
    is Verdict.NotJudgeable -> when (reason) {
        Verdict.Reason.IDLE -> "No verdict" to "The car didn't move, so there's nothing to compare."
        Verdict.Reason.RANGE_INCREASED ->
            "No verdict" to "The range went up during the trip. That's the display rounding, not real savings."
        Verdict.Reason.NOT_COUNTED -> "Not counted" to "This trip is marked as refuelled or excluded, so it isn't judged."
        Verdict.Reason.NO_USABLE_BASELINE ->
            "No verdict" to "This route's earlier trips don't give a usable baseline yet."
    }
}
