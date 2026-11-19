package com.gasmeter.app.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.formatDateTime
import com.gasmeter.app.ui.formatKm
import com.gasmeter.app.ui.formatRatio
import com.gasmeter.app.ui.formatSignedKm
import com.gasmeter.app.ui.formatTime
import com.gasmeter.app.ui.shortLabel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onStartTrip: () -> Unit,
    onEndTrip: () -> Unit,
    onOpenTrip: (Long) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDiscard by remember { mutableStateOf(false) }

    GasMeterScaffold(title = "GasMeter") { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                val open = state.openTrip
                when {
                    state.loading -> Unit
                    open != null -> Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Trip in progress", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${state.openTripRouteName ?: "No route"} · started ${formatTime(open.startedAt)}\n" +
                                    "Range ${formatKm(open.rangeStartKm)} · odometer ${formatKm(open.odometerStartKm)}",
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = onEndTrip) { Text("End trip") }
                                OutlinedButton(onClick = { confirmDiscard = true }) { Text("Discard") }
                            }
                        }
                    }
                    else -> Button(onClick = onStartTrip, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                        Text("Start trip", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Text("Recent trips", style = MaterialTheme.typography.titleMedium)
            }

            if (!state.loading && state.recentTrips.isEmpty()) {
                item {
                    Text(
                        "No trips yet. Tap Start trip as you leave, and enter the range and " +
                            "odometer shown on your dashboard.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            items(state.recentTrips, key = { it.id }) { row ->
                TripRowItem(row, onClick = { onOpenTrip(row.id) })
                HorizontalDivider()
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
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(row.routeName ?: "No route", style = MaterialTheme.typography.bodyLarge)
            Text(formatDateTime(row.endedAt), style = MaterialTheme.typography.bodySmall)
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
            Text(summary, style = MaterialTheme.typography.bodyMedium)
        }
        row.verdict.shortLabel()?.let { label ->
            AssistChip(onClick = onClick, label = { Text(label) })
        }
    }
}
