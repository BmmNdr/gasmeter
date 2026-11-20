package com.gasmeter.app.ui.trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.data.CarSettings
import com.gasmeter.app.ui.BigStat
import com.gasmeter.app.ui.ChoiceChip
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.LocalGasMeterColors
import com.gasmeter.app.ui.MetricRow
import com.gasmeter.app.ui.MonoBody
import com.gasmeter.app.ui.PanelCard
import com.gasmeter.app.ui.SectionLabel
import com.gasmeter.app.ui.TagPill
import com.gasmeter.app.ui.VerdictBanner
import com.gasmeter.app.ui.describe
import com.gasmeter.app.ui.formatDateTime
import com.gasmeter.app.ui.formatKm
import com.gasmeter.app.ui.formatLitres
import com.gasmeter.app.ui.formatMoney
import com.gasmeter.app.ui.formatRatio
import com.gasmeter.app.ui.formatSignedKm
import com.gasmeter.app.ui.label
import com.gasmeter.app.ui.verdictColor
import com.gasmeter.core.AnalyzedTrip

@Composable
fun TripDetailScreen(viewModel: TripDetailViewModel, onBack: () -> Unit, onOpenSettings: () -> Unit) {
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
                    TripResult(trip, state.routeName, state.settings, onOpenSettings) { RouteChanger(trip, viewModel) }
                    FlagsCard(trip, viewModel)
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        shape = MaterialTheme.shapes.medium,
                    ) { Text("Delete trip", color = MaterialTheme.colorScheme.error) }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TripResult(
    analyzed: AnalyzedTrip,
    routeName: String?,
    settings: CarSettings,
    onOpenSettings: () -> Unit,
    routeChanger: @Composable () -> Unit,
) {
    val m = analyzed.trip.metrics
    val (headline, explanation) = analyzed.verdict.describe()
    val muted = LocalGasMeterColors.current.muted

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(routeName ?: "No route", style = MaterialTheme.typography.headlineSmall)
        Text(formatDateTime(analyzed.log.endedAt), style = MonoBody, color = muted)
        routeChanger()
    }

    VerdictBanner(headline, explanation, verdictColor(analyzed.verdict), headlineTag = "verdictHeadline")

    // The two numbers that matter, instrument-style.
    if (!m.isIdle) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            m.ratio?.let { BigStat(formatRatio(it), "Range per km", Modifier.weight(1f)) }
            BigStat(formatSignedKm(m.excessKm), "Extra range", Modifier.weight(1f))
        }
    }

    PanelCard {
        if (m.isIdle) {
            Text("${formatKm(m.rangeUsedKm)} of range used while the car didn't move.")
        } else {
            MetricRow("Distance driven", formatKm(m.distanceKm))
            MetricRow("Range used", "${formatKm(m.rangeUsedKm)} (±${formatKm(m.rangeUncertaintyKm)})")
        }
        m.litres?.let { MetricRow("Fuel", formatLitres(it)) }
        m.cost?.let { MetricRow("Cost", formatMoney(it, settings.currency)) }
        m.excessCost?.let { MetricRow("Cost of extra range", formatMoney(it, settings.currency), emphasize = true) }
    }

    if (m.tooShortToJudge && !m.isIdle) {
        Text(
            "Short trip: your dashboard rounds the range to ${formatKm(m.rangeUncertaintyKm)}, so this trip " +
                "alone says little. Comparing several runs of the same route evens this out.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
        )
    }
    val missing = when {
        m.litres == null -> "Set your car's consumption in Settings to see litres and cost."
        m.cost == null -> "Add a fuel price in Settings to see the cost."
        else -> null
    }
    if (missing != null) {
        Column {
            Text(missing, style = MaterialTheme.typography.bodySmall, color = muted)
            TextButton(onClick = onOpenSettings, contentPadding = PaddingValues(0.dp)) { Text("Open settings") }
        }
    }

    if (analyzed.log.tags.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("Tags")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                analyzed.log.tags.sortedBy { it.ordinal }.forEach { TagPill(it.label()) }
            }
        }
    }
    analyzed.log.note?.let { note ->
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SectionLabel("Note")
            Text(note)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RouteChanger(analyzed: AnalyzedTrip, viewModel: TripDetailViewModel) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    var open by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { open = !open }, contentPadding = PaddingValues(0.dp)) {
            Text(if (open) "Done" else "Change route")
        }
        if (open) {
            SectionLabel("Move this trip to")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(
                    selected = analyzed.log.routeId == null,
                    onClick = { viewModel.setRoute(null) },
                    text = "No route",
                )
                routes.forEach { route ->
                    ChoiceChip(
                        selected = analyzed.log.routeId == route.id,
                        onClick = { viewModel.setRoute(route.id) },
                        text = route.name,
                    )
                }
            }
            Text(
                "The verdict is recalculated against the new route's trips.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalGasMeterColors.current.muted,
            )
        }
    }
}

@Composable
private fun FlagsCard(analyzed: AnalyzedTrip, viewModel: TripDetailViewModel) {
    PanelCard {
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

@Composable
private fun SwitchRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = LocalGasMeterColors.current.muted,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = LocalGasMeterColors.current.muted,
            ),
        )
    }
}
