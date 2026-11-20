package com.gasmeter.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gasmeter.core.VerdictRules
import com.gasmeter.core.Verdict
import kotlin.math.max
import kotlin.math.min

data class ChartPoint(val ratio: Double, val verdict: Verdict?)

/**
 * Ratio per trip in trip order, with the route's median as a dashed line and the
 * ±15% "normal" band shaded around it. Dots take the verdict colour of each trip.
 */
@Composable
fun RatioChart(points: List<ChartPoint>, median: Double, modifier: Modifier = Modifier) {
    val extras = LocalGasMeterColors.current
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outline
    val labelStyle = MonoBody.copy(fontSize = 11.sp, color = extras.muted)
    val measurer = rememberTextMeasurer()
    val dotColors = points.map { verdictColorFor(it.verdict, extras) }

    val bandLow = median * VerdictRules.NORMAL_LOWER_BOUND
    val bandHigh = median * VerdictRules.NORMAL_UPPER_BOUND
    val dataMin = min(points.minOf { it.ratio }, bandLow)
    val dataMax = max(points.maxOf { it.ratio }, bandHigh)
    val pad = (dataMax - dataMin).coerceAtLeast(0.1) * 0.12
    val yMin = dataMin - pad
    val yMax = dataMax + pad

    Canvas(
        modifier
            .fillMaxWidth()
            .height(190.dp)
            .testTag("ratioChart")
            .semantics { contentDescription = "Range per km for ${points.size} trips" },
    ) {
        val gutter = 52.dp.toPx()
        val right = size.width - 10.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        fun y(v: Double) = (bottom - (v - yMin) / (yMax - yMin) * (bottom - top)).toFloat()
        fun x(i: Int) = if (points.size == 1) (gutter + right) / 2 else gutter + i * (right - gutter) / (points.size - 1)

        // Normal band and median line.
        drawRect(
            color = extras.normal.copy(alpha = 0.12f),
            topLeft = Offset(gutter, y(bandHigh)),
            size = Size(right - gutter, y(bandLow) - y(bandHigh)),
        )
        drawLine(
            color = extras.muted,
            start = Offset(gutter, y(median)),
            end = Offset(right, y(median)),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx())),
        )
        drawLine(grid, Offset(gutter, top), Offset(gutter, bottom), strokeWidth = 1.dp.toPx())

        // Y labels: median always; max and min of the data unless they'd overlap it.
        val medianLabel = measurer.measure(formatRatio(median), labelStyle)
        val minGap = medianLabel.size.height.toFloat()
        listOf(median, points.maxOf { it.ratio }, points.minOf { it.ratio })
            .filterIndexed { i, value -> i == 0 || kotlin.math.abs(y(value) - y(median)) >= minGap }
            .distinctBy { "%.2f".format(it) }
            .forEach { value ->
                val text = measurer.measure(formatRatio(value), labelStyle)
                val ty = (y(value) - text.size.height / 2).coerceIn(0f, size.height - text.size.height)
                drawText(text, topLeft = Offset(gutter - text.size.width - 6.dp.toPx(), ty))
            }

        // Line through the trips, then the dots on top.
        if (points.size > 1) {
            val path = Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.ratio)) else lineTo(x(i), y(p.ratio)) }
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        points.forEachIndexed { i, p ->
            drawCircle(Color.Black.copy(alpha = 0.25f), radius = 6.dp.toPx(), center = Offset(x(i), y(p.ratio)))
            drawCircle(dotColors[i], radius = 4.5.dp.toPx(), center = Offset(x(i), y(p.ratio)))
        }
    }
}
