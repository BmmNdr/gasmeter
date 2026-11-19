package com.gasmeter.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.LocalGasMeterColors
import com.gasmeter.app.ui.MonoBody
import com.gasmeter.app.ui.PanelCard
import com.gasmeter.app.ui.SectionLabel
import com.gasmeter.app.ui.TagPill
import com.gasmeter.app.ui.formatDateTime
import com.gasmeter.app.ui.formatKm
import com.gasmeter.app.ui.formatRatio
import com.gasmeter.app.ui.formatSignedKm
import com.gasmeter.app.ui.formatTime
import com.gasmeter.app.ui.shortLabel
import com.gasmeter.app.ui.verdictColor

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }
    val muted = LocalGasMeterColors.current.muted

    GasMeterScaffold(title = "GasMeter") { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val open = state.openTrip
                when {
                    state.loading -> Unit
                    open != null -> PanelCard(accent = MaterialTheme.colorScheme.primary) {
                        SectionLabel("Trip in progress")
                        Text(
                            "${state.openTripRouteName ?: "No route"} · started ${formatTime(open.startedAt)}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Range ${formatKm(open.rangeStartKm)} · odometer ${formatKm(open.odometerStartKm)}",
                            style = MonoBody.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onEndTrip, shape = MaterialTheme.shapes.medium) { Text("End trip") }
                            OutlinedButton(onClick = { confirmDiscard = true }, shape = MaterialTheme.shapes.medium) {
                                Text("Discard")
                            }
                        }
                    }
                    else -> Button(
                        onClick = onStartTrip,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                    ) {
                        Text("Start trip", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                SectionLabel("Recent trips")
            }

            if (!state.loading && state.recentTrips.isEmpty()) {
                item {
                    Text(
                        "No trips yet. Tap Start trip as you leave, and enter the range and " +
                            "odometer shown on your dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = muted,
                    )
                }
            }

            items(state.recentTrips, key = { it.id }) { row ->
                TripRowItem(row, onClick = { onOpenTrip(row.id) })
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard this trip?") },
            text = { Text("The start readings will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    viewModel.discardOpenTrip()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun TripRowItem(row: TripRow, onClick: () -> Unit) {
    val muted = LocalGasMeterColors.current.muted
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.routeName ?: "No route", style = MaterialTheme.typography.titleMedium)
            Text(formatDateTime(row.endedAt), style = MaterialTheme.typography.bodySmall, color = muted)
            val m = row.metrics
            val summary = buildString {
                append(formatKm(m.distanceKm))
                append(" · used ")
                append(formatKm(m.rangeUsedKm))
                append(" · ")
                append(formatSignedKm(m.excessKm))
                m.ratio?.let { append(" · ").append(formatRatio(it)) }
                if (!row.counted) append(" · not counted")
            }
            Text(summary, style = MonoBody.copy(fontSize = 13.sp))
        }
        row.verdict.shortLabel()?.let { label ->
            TagPill(label, color = verdictColor(row.verdict))
        }
    }
}
