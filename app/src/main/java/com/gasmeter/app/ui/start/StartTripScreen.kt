package com.gasmeter.app.ui.start

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
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
import com.gasmeter.app.ui.ChoiceChip
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.LocalGasMeterColors
import com.gasmeter.app.ui.PrimaryButton
import com.gasmeter.app.ui.SectionLabel
import com.gasmeter.app.ui.KmField

private val STEP_OPTIONS = listOf(1.0, 5.0, 10.0)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StartTripScreen(viewModel: StartTripViewModel, onBack: () -> Unit, onStarted: () -> Unit) {
    val routes by viewModel.routes.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var showNewRoute by remember { mutableStateOf(false) }

    GasMeterScaffold(title = "Start trip", onBack = onBack) { padding ->
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
            SectionLabel("Route")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip(
                    selected = viewModel.selectedRouteId == null,
                    onClick = { viewModel.selectedRouteId = null },
                    text = "No route",
                )
                routes.forEach { route ->
                    ChoiceChip(
                        selected = viewModel.selectedRouteId == route.id,
                        onClick = { viewModel.selectedRouteId = route.id },
                        text = route.name,
                    )
                }
                ChoiceChip(
                    selected = showNewRoute,
                    onClick = { showNewRoute = !showNewRoute },
                    text = "+ New route",
                )
            }
            if (showNewRoute) {
                NewRouteRow(onAdd = { name ->
                    showNewRoute = false
                    viewModel.addRoute(name)
                })
            }

            SectionLabel("Dashboard readings now")
            KmField(
                value = viewModel.rangeText,
                onValueChange = { viewModel.rangeText = it },
                label = "Range remaining",
                testTag = "rangeField",
                error = viewModel.rangeError,
            )
            KmField(
                value = viewModel.odometerText,
                onValueChange = { viewModel.odometerText = it },
                label = "Odometer",
                testTag = "odometerField",
                error = viewModel.odometerError,
            )

            // Asked once, before the first trip: needed to judge rounding noise.
            if (settings != null && settings?.rangeDisplayStepKm == null) {
                Text("How does your dashboard round the range?", style = MaterialTheme.typography.titleMedium)
                Text(
                    "If it only shows 300, 295, 290… pick 5 km. This tells the app how much of a short " +
                        "trip's result is just rounding.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalGasMeterColors.current.muted,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    STEP_OPTIONS.forEach { step ->
                        ChoiceChip(
                            selected = viewModel.chosenStepKm == step,
                            onClick = { viewModel.chosenStepKm = step },
                            text = "${step.toInt()} km steps",
                        )
                    }
                }
                viewModel.stepError?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            viewModel.generalError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            PrimaryButton(
                text = "Start",
                onClick = { viewModel.start(onStarted) },
                enabled = !viewModel.saving,
                modifier = Modifier.testTag("startButton"),
            )
        }
    }
}

@Composable
private fun NewRouteRow(onAdd: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Route name, e.g. Home → Work") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
            modifier = Modifier.weight(1f).testTag("routeNameField"),
        )
        TextButton(onClick = { onAdd(name) }, enabled = name.isNotBlank()) { Text("Add") }
    }
}
