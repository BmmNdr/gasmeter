package com.gasmeter.app.ui.end

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.data.OpenTrip
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.KmField
import com.gasmeter.app.ui.MetricRow
import com.gasmeter.app.ui.formatKm
import com.gasmeter.app.ui.formatSignedKm
import com.gasmeter.app.ui.formatTime
import com.gasmeter.app.ui.label
import com.gasmeter.core.TripIssue
import com.gasmeter.core.TripTag

@Composable
fun EndTripScreen(viewModel: EndTripViewModel, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val ctx by viewModel.context.collectAsStateWithLifecycle()

    GasMeterScaffold(title = "End trip", onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val context = ctx
            val open = context?.openTrip
            when {
                context == null -> Unit
                open == null -> Text("There's no trip in progress.")
                else -> EndTripForm(viewModel, context, open, onSaved)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EndTripForm(
    viewModel: EndTripViewModel,
    context: EndTripContext,
    open: OpenTrip,
    onSaved: (Long) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(context.routeName ?: "No route", style = MaterialTheme.typography.titleMedium)
                Text("Started ${formatTime(open.startedAt)}")
                Text("Start range ${formatKm(open.rangeStartKm)} · odometer ${formatKm(open.odometerStartKm)}")
            }
        }

        Text("Dashboard readings now", style = MaterialTheme.typography.titleMedium)
        KmField(
            value = viewModel.rangeText,
            onValueChange = { viewModel.rangeText = it },
            label = "Range remaining",
            testTag = "endRangeField",
            error = viewModel.rangeError,
        )
        KmField(
            value = viewModel.odometerText,
            onValueChange = { viewModel.odometerText = it },
            label = "Odometer",
            testTag = "endOdometerField",
            error = viewModel.odometerError,
        )

        val preview = viewModel.preview(context)
        if (preview != null) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricRow("Distance", formatKm(preview.distanceKm))
                    MetricRow("Range used", formatKm(preview.rangeUsedKm))
                    MetricRow("Difference", formatSignedKm(preview.rangeUsedKm - preview.distanceKm))
                }
            }
            preview.issues.forEach { issue ->
                Text(
                    issue.message(),
                    color = if (issue.isBlocking) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                )
            }
        }

        val suggestRefuel = preview?.issues?.any { it is TripIssue.PossibleRefuel } == true
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = viewModel.refuelled, onCheckedChange = { viewModel.refuelled = it })
            Text(
                if (suggestRefuel) "Yes, I refuelled during this trip" else "I refuelled during this trip",
            )
        }
        Text(
            "Refuelled trips are kept but left out of comparisons.",
            style = MaterialTheme.typography.bodySmall,
        )

        Text("Anything unusual? (optional)", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TripTag.entries.forEach { tag ->
                FilterChip(
                    selected = tag in viewModel.tags,
                    onClick = { viewModel.toggleTag(tag) },
                    label = { Text(tag.label()) },
                )
            }
        }
        OutlinedTextField(
            value = viewModel.note,
            onValueChange = { viewModel.note = it },
            label = { Text("Note (optional)") },
            modifier = Modifier.fillMaxWidth(),
        )

        viewModel.generalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Button(
            onClick = { viewModel.save(onSaved) },
            enabled = !viewModel.saving && preview?.blocking != true,
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("saveTripButton"),
        ) { Text("Save trip") }
    }
}
