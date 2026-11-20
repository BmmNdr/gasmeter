package com.gasmeter.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gasmeter.app.data.StoredFuelPrice
import com.gasmeter.app.ui.ChoiceChip
import com.gasmeter.app.ui.GasMeterScaffold
import com.gasmeter.app.ui.KmField
import com.gasmeter.app.ui.LocalGasMeterColors
import com.gasmeter.app.ui.MonoBody
import com.gasmeter.app.ui.PanelCard
import com.gasmeter.app.ui.PrimaryButton
import com.gasmeter.app.ui.SectionLabel
import com.gasmeter.app.ui.formatDate
import com.gasmeter.app.ui.formatPricePerLitre
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private val STEP_OPTIONS = listOf(1.0, 5.0, 10.0)

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prices by viewModel.prices.collectAsStateWithLifecycle()
    val currency = settings?.currency ?: "€"

    GasMeterScaffold(title = "Settings", onBack = onBack) { padding ->
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
            CarCard(viewModel)
            PriceCard(viewModel, prices, currency)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CarCard(viewModel: SettingsViewModel) {
    val muted = LocalGasMeterColors.current.muted
    PanelCard {
        SectionLabel("Car")
        KmField(
            value = viewModel.consumptionText,
            onValueChange = {
                viewModel.consumptionText = it
                viewModel.onCarFieldChanged()
            },
            label = "Average consumption",
            testTag = "consumptionField",
            suffix = "L/100 km",
            error = viewModel.consumptionError,
            hint = "From your car's trip computer. Turns range-km into litres: " +
                "5 range-km at 6 L/100 km = 0.30 L.",
        )

        SectionLabel("Your dashboard rounds the range to")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            STEP_OPTIONS.forEach { step ->
                ChoiceChip(
                    selected = viewModel.stepKm == step,
                    onClick = {
                        viewModel.stepKm = step
                        viewModel.onCarFieldChanged()
                    },
                    text = "${step.toInt()} km",
                )
            }
        }
        Text(
            "Used for the ± on results and to flag trips that are too short to judge.",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )

        OutlinedTextField(
            value = viewModel.currencyText,
            onValueChange = {
                viewModel.currencyText = it.take(4)
                viewModel.onCarFieldChanged()
            },
            label = { Text("Currency") },
            singleLine = true,
            textStyle = MonoBody,
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = MaterialTheme.colorScheme.outline),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.width(140.dp).testTag("currencyField"),
        )

        PrimaryButton("Save", onClick = viewModel::saveCar, modifier = Modifier.testTag("saveCarButton"))
        if (viewModel.carSaved) {
            Text("Saved. Every trip is recalculated.", color = LocalGasMeterColors.current.efficient)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PriceCard(viewModel: SettingsViewModel, prices: List<StoredFuelPrice>, currency: String) {
    val muted = LocalGasMeterColors.current.muted
    var pickDate by remember { mutableStateOf(false) }

    PanelCard {
        SectionLabel("Fuel price")
        val current = viewModel.currentPrice(prices)
        if (current == null) {
            Text("No price yet", style = MaterialTheme.typography.headlineSmall, color = muted)
        } else {
            Text(
                formatPricePerLitre(current.pricePerLitre, currency),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text("since ${formatDate(current.effectiveFrom)}", style = MonoBody, color = muted)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 4.dp))
        SectionLabel("Add a price")
        KmField(
            value = viewModel.priceText,
            onValueChange = { viewModel.priceText = it },
            label = "Price per litre",
            testTag = "priceField",
            suffix = "$currency/L",
            error = viewModel.priceError,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (viewModel.priceDate == viewModel.today()) "From today" else "From ${formatDate(viewModel.priceDate)}",
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { pickDate = true }) { Text("Change date") }
        }
        PrimaryButton("Add price", onClick = viewModel::addPrice, modifier = Modifier.testTag("addPriceButton"))
        Text(
            "Trips are costed at the price valid on the day they started. Back-date a price to fix past trips.",
            style = MaterialTheme.typography.bodySmall,
            color = muted,
        )

        if (prices.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 4.dp))
            SectionLabel("History")
            prices.forEach { price ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(formatDate(price.effectiveFrom), modifier = Modifier.weight(1f))
                    Text(formatPricePerLitre(price.pricePerLitre, currency), style = MonoBody)
                    IconButton(onClick = { viewModel.deletePrice(price.id) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete price from ${formatDate(price.effectiveFrom)}",
                            tint = muted,
                        )
                    }
                }
            }
        }
    }

    if (pickDate) {
        val todayMillis = viewModel.today().toEpochMillis()
        val state = rememberDatePickerState(
            initialSelectedDateMillis = viewModel.priceDate.toEpochMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { viewModel.priceDate = it.toLocalDate() }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** The date picker works in UTC midnight millis. */
private fun LocalDate.toEpochMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
