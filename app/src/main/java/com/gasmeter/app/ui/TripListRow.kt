package com.gasmeter.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gasmeter.core.AnalyzedTrip
import com.gasmeter.core.TripMetrics
import com.gasmeter.core.Verdict
import java.time.Instant

/** One trip in a list (Home's recent trips, a route's trips). */
data class TripRow(
    val id: Long,
    val endedAt: Instant,
    val routeName: String?,
    val metrics: TripMetrics,
    val verdict: Verdict?,
    val counted: Boolean,
    val currency: String,
)

fun AnalyzedTrip.toRow(routeNames: Map<Long, String>, currency: String) = TripRow(
    id = log.id,
    endedAt = log.endedAt,
    routeName = log.routeId?.let(routeNames::get),
    metrics = trip.metrics,
    verdict = verdict,
    counted = trip.countsForStats,
    currency = currency,
)

@Composable
fun TripListRow(row: TripRow, onClick: () -> Unit, showRoute: Boolean = true) {
    val muted = LocalGasMeterColors.current.muted
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (showRoute) {
                Text(row.routeName ?: "No route", style = MaterialTheme.typography.titleMedium)
            }
            Text(formatDateTime(row.endedAt), style = MaterialTheme.typography.bodySmall, color = muted)
            val m = row.metrics
            val summary = buildString {
                append(formatKm(m.distanceKm))
                append(" · used ")
                append(formatKm(m.rangeUsedKm))
                append(" · ")
                append(formatSignedKm(m.excessKm))
                m.ratio?.let { append(" · ").append(formatRatio(it)) }
                m.cost?.let { append(" · ").append(formatMoney(it, row.currency)) }
                if (!row.counted) append(" · not counted")
            }
            Text(summary, style = MonoBody.copy(fontSize = 13.sp))
        }
        row.verdict.shortLabel()?.let { label ->
            TagPill(label, color = verdictColor(row.verdict))
        }
    }
}
