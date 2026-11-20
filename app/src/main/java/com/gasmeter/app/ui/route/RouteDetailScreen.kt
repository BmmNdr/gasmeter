package com.gasmeter.app.ui.route

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.ui.BigStat
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.LocalGasMeterColors
import com.gasmeter.app.ui.MetricRow
import com.gasmeter.app.ui.PanelCard
import com.gasmeter.app.ui.PrimaryButton
import com.gasmeter.app.ui.RatioChart
import com.gasmeter.app.ui.SectionLabel
import com.gasmeter.app.ui.TripListRow
import com.gasmeter.app.ui.describe
import com.gasmeter.app.ui.formatDate
import com.gasmeter.app.ui.formatKm
import com.gasmeter.app.ui.formatMoney
import com.gasmeter.app.ui.formatPercent
import com.gasmeter.app.ui.formatRatio
import com.gasmeter.app.ui.formatSignedKm
import com.gasmeter.app.ui.hours
import com.gasmeter.app.ui.label
import com.gasmeter.app.ui.tripCount
import com.gasmeter.core.RouteBreakdown
import com.gasmeter.core.RouteStats
import com.gasmeter.core.Trend
import com.gasmeter.app.data.CarSettings

@Composable
fun RouteDetailScreen(viewModel: RouteDetailViewModel, onBack: () -> Unit, onOpenTrip: (Long) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val route = state.route

    GasMeterScaffold(
        title = route?.name ?: "Route",
        onBack = onBack,
        actions = {
            if (route != null) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Route options")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            onClick = {
                                menuOpen = false
                                viewModel.startRename()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete route", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuOpen = false
                                confirmDelete = true
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val stats = state.stats
            when {
                state.loading -> Unit
                route == null || stats == null -> Text("This route no longer exists.")
                else -> {
                    if (viewModel.renaming) RenameRow(viewModel)
                    Overview(stats)
                    ChartCard(state, stats)
                    StatsCard(stats, state.settings)
                    state.breakdown?.let { BreakdownCard(it) }
                    TripsList(state, onOpenTrip)
                }
            }
        }
    }

    if (confirmDelete && route != null) {
        val trips = state.trips.size
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete \"${route.name}\"?") },
            text = {
                Text(
                    if (trips == 0) {
                        "The route will be removed."
                    } else {
                        "Its ${tripCount(trips)} will stay in your history, as No route."
                    },
                )
            },
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
private fun RenameRow(viewModel: RouteDetailViewModel) {
    PanelCard(accent = MaterialTheme.colorScheme.primary) {
        SectionLabel("Rename route")
        OutlinedTextField(
            value = viewModel.renameText,
            onValueChange = { viewModel.renameText = it },
            singleLine = true,
            isError = viewModel.renameError != null,
            supportingText = viewModel.renameError?.let { { Text(it) } },
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth().testTag("renameField"),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Save name", onClick = viewModel::saveRename, modifier = Modifier.weight(1f))
            TextButton(onClick = viewModel::cancelRename) { Text("Cancel") }
        }
    }
}

@Composable
private fun Overview(stats: RouteStats) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) {
            BigStat(stats.medianRatio?.let(::formatRatio) ?: "–", "Usual range per km", Modifier.weight(1f))
            BigStat(
                stats.tripCount.toString(),
                if (stats.tripCount == 1) "Trip" else "Trips",
                Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        stats.trend?.let { trend ->
            val extras = LocalGasMeterColors.current
            val color = when (trend.direction) {
                Trend.Direction.IMPROVING -> extras.efficient
                Trend.Direction.WORSENING -> extras.expensive
                Trend.Direction.STABLE -> extras.muted
            }
            Text(trend.describe(), color = color, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ChartCard(state: RouteDetailState, stats: RouteStats) {
    PanelCard {
        SectionLabel("Range per km, trip by trip")
        val median = stats.medianRatio
        if (state.chart.size >= 2 && median != null && median > 0) {
            RatioChart(state.chart, median)
            Text(
                "Shaded band: normal (±15% of usual). Dashed line: usual.",
                style = MaterialTheme.typography.bodySmall,
                color = LocalGasMeterColors.current.muted,
            )
        } else {
            Text(
                "Chart appears after 2 trips on this route.",
                style = MaterialTheme.typography.bodyMedium,
                color = LocalGasMeterColors.current.muted,
            )
        }
    }
}

@Composable
private fun StatsCard(stats: RouteStats, settings: CarSettings) {
    PanelCard {
        SectionLabel("Stats")
        MetricRow(
            "Trips counted",
            stats.tripCount.toString() + if (stats.ignoredTripCount > 0) " (+${stats.ignoredTripCount} ignored)" else "",
        )
        stats.medianRatio?.let { MetricRow("Usual (median)", formatRatio(it)) }
        stats.meanRatio?.let { MetricRow("Average", formatRatio(it)) }
        stats.bestTrip?.let { best ->
            MetricRow("Best", "${formatRatio(best.metrics.ratio!!)} · ${formatDate(best.endedAt)}")
        }
        stats.worstTrip?.let { worst ->
            MetricRow("Worst", "${formatRatio(worst.metrics.ratio!!)} · ${formatDate(worst.endedAt)}")
        }
        MetricRow("Distance driven", formatKm(stats.totalDistanceKm))
        MetricRow("Extra range, total", formatSignedKm(stats.totalExcessKm), emphasize = true)
        stats.averageCost?.let { MetricRow("Average cost", formatMoney(it, settings.currency)) }
        stats.totalCost?.let { MetricRow("Total cost", formatMoney(it, settings.currency)) }
        stats.totalExcessCost?.let {
            MetricRow("Cost of extra range", formatMoney(it, settings.currency), emphasize = true)
        }
    }
}

@Composable
private fun BreakdownCard(breakdown: RouteBreakdown) {
    val muted = LocalGasMeterColors.current.muted
    PanelCard {
        SectionLabel("What makes a difference")
        if (breakdown.isEmpty) {
            Text(
                "Not enough trips to compare yet. Tag unusual trips (traffic, AC, cold engine…) when you end " +
                    "them; groups appear once they have ${RouteBreakdown.MIN_GROUP_SIZE} trips.",
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
        }
        breakdown.byTag.forEach { c ->
            MetricRow(c.tag.label(), "${formatRatio(c.medianWith)} vs ${formatRatio(c.medianWithout)}")
            Text(
                (if (c.relativeChange >= 0) "${formatPercent(c.relativeChange)} more" else "${formatPercent(c.relativeChange)} less") +
                    " range per km with it · ${c.tripsWith} vs ${c.tripsWithout} trips",
                style = MaterialTheme.typography.bodySmall,
                color = muted,
            )
        }
        if (breakdown.byTag.isNotEmpty() && breakdown.byTimeOfDay.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 4.dp))
        }
        breakdown.byTimeOfDay.forEach { g ->
            MetricRow("${g.slot.label()} (${g.slot.hours()}) · ${g.trips}", formatRatio(g.medianRatio))
        }
    }
}

@Composable
private fun TripsList(state: RouteDetailState, onOpenTrip: (Long) -> Unit) {
    Column {
        SectionLabel("Trips on this route")
        state.trips.forEach { row ->
            TripListRow(row, onClick = { onOpenTrip(row.id) }, showRoute = false)
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        }
    }
}
