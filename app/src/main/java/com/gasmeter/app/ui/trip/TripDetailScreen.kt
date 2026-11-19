package com.gasmeter.app.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.data.CarSettings
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.MetricRow
import com.gasmeter.app.ui.describe
import com.gasmeter.app.ui.formatDateTime
import com.gasmeter.app.ui.formatKm
import com.gasmeter.app.ui.formatLitres
import com.gasmeter.app.ui.formatMoney
import com.gasmeter.app.ui.formatRatio
import com.gasmeter.app.ui.formatSignedKm
import com.gasmeter.app.ui.label
import com.gasmeter.core.AnalyzedTrip
import com.gasmeter.core.Rating
import com.gasmeter.core.Verdict

@Composable
fun TripDetailScreen(viewModel: TripDetailViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    GasMeterScaffold(title = "Trip result", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val trip = state.trip
            when {
                state.loading -> Unit
                trip == null -> Text("This trip no longer exists.")
                else -> {
                    TripResult(trip, state.routeName, state.settings)
                    FlagsCard(trip, viewModel)
                    OutlinedButton(onClick = { confirmDelete = true }) { Text("Delete trip") }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this trip?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TripResult(analyzed: AnalyzedTrip, routeName: String?, settings: CarSettings) {
    val m = analyzed.trip.metrics
    val (headline, explanation) = analyzed.verdict.describe()

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(routeName ?: "No route", style = MaterialTheme.typography.titleLarge)
        Text(formatDateTime(analyzed.log.endedAt), style = MaterialTheme.typography.bodyMedium)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = verdictColor(analyzed.verdict)),
    ) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(headline, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("verdictHeadline"))
            Text(explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (m.isIdle) {
                Text("${formatKm(m.rangeUsedKm)} of range used while the car didn't move.")
            } else {
                MetricRow("Distance driven", formatKm(m.distanceKm))
                MetricRow("Range used", "${formatKm(m.rangeUsedKm)} (±${formatKm(m.rangeUncertaintyKm)})")
                MetricRow("Extra range used", formatSignedKm(m.excessKm), emphasize = true)
                m.ratio?.let { MetricRow("Range per km driven", formatRatio(it), emphasize = true) }
            }
            m.litres?.let { MetricRow("Fuel", formatLitres(it)) }
            m.cost?.let { MetricRow("Cost", formatMoney(it, settings.currency)) }
            m.excessCost?.let { MetricRow("Cost of extra range", formatMoney(it, settings.currency)) }
        }
    }

    if (m.tooShortToJudge && !m.isIdle) {
        Text(
            "Short trip: your dashboard rounds the range to ${formatKm(m.rangeUncertaintyKm)}, so this trip " +
                "alone says little. Comparing several runs of the same route evens this out.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    if (m.litres == null) {
        Text(
            "Litres and cost will appear once you set your car's average consumption and the fuel " +
                "price (settings are coming in a later version).",
            style = MaterialTheme.typography.bodySmall,
        )
    } else if (m.cost == null) {
        Text("Add a fuel price to see the cost.", style = MaterialTheme.typography.bodySmall)
    }

    if (analyzed.log.tags.isNotEmpty()) {
        Text("Tags: " + analyzed.log.tags.sortedBy { it.ordinal }.joinToString { it.label() })
    }
    analyzed.log.note?.let { Text("Note: $it") }
}

@Composable
private fun FlagsCard(analyzed: AnalyzedTrip, viewModel: TripDetailViewModel) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SwitchRow(
                label = "Refuelled during this trip",
                checked = analyzed.log.refuelledDuringTrip,
                onCheckedChange = viewModel::setRefuelled,
            )
            SwitchRow(
                label = "Exclude from comparisons",
                checked = analyzed.log.excludedFromStats,
                onCheckedChange = viewModel::setExcluded,
            )
        }
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun verdictColor(verdict: Verdict?): Color {
    val scheme = MaterialTheme.colorScheme
    return when ((verdict as? Verdict.Judged)?.rating) {
        Rating.EXPENSIVE -> scheme.errorContainer
        Rating.EFFICIENT -> scheme.primaryContainer
        Rating.NORMAL -> scheme.secondaryContainer
        null -> scheme.surfaceVariant
    }
}
