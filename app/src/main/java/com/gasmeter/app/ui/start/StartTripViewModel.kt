package com.gasmeter.app.ui.start

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmeter.app.data.CarSettings
import com.gasmeter.app.data.Route
import com.gasmeter.app.data.TripAlreadyOpenException
import com.gasmeter.app.data.TripRepository
import com.gasmeter.app.ui.parseDecimal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StartTripViewModel(private val repository: TripRepository) : ViewModel() {

    val routes: StateFlow<List<Route>> =
        repository.routes.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Null until loaded. */
    val settings: StateFlow<CarSettings?> =
        repository.carSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var selectedRouteId by mutableStateOf<Long?>(null)
    var rangeText by mutableStateOf("")
    var odometerText by mutableStateOf("")

    /** The display step picked on this screen, used only while none is saved yet. */
    var chosenStepKm by mutableStateOf<Double?>(null)

    var rangeError by mutableStateOf<String?>(null)
        private set
    var odometerError by mutableStateOf<String?>(null)
        private set
    var stepError by mutableStateOf<String?>(null)
        private set
    var generalError by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch {
            repository.lastEndReadings()?.let { last ->
                if (rangeText.isEmpty()) rangeText = plain(last.rangeKm)
                if (odometerText.isEmpty()) odometerText = plain(last.odometerKm)
            }
        }
    }

    fun addRoute(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { selectedRouteId = repository.addRoute(name) }
    }

    fun start(onStarted: () -> Unit) {
        val range = parseDecimal(rangeText)
        val odometer = parseDecimal(odometerText)
        rangeError = numberError(range)
        odometerError = numberError(odometer)
        val loaded = settings.value
        val needsStep = loaded != null && loaded.rangeDisplayStepKm == null
        stepError = if (needsStep && chosenStepKm == null) "Pick how your dashboard rounds the range" else null
        if (rangeError != null || odometerError != null || stepError != null || saving) return

        saving = true
        generalError = null
        viewModelScope.launch {
            try {
                if (needsStep) repository.setRangeDisplayStep(chosenStepKm!!)
                repository.startTrip(selectedRouteId, range!!, odometer!!)
                onStarted()
            } catch (e: TripAlreadyOpenException) {
                generalError = "A trip is already in progress. End or discard it first."
            } finally {
                saving = false
            }
        }
    }

    private fun numberError(value: Double?): String? = when {
        value == null -> "Enter a number"
        value < 0 -> "Can't be negative"
        else -> null
    }

    /** Shows 300.0 as "300" so the prefilled field looks like what the user types. */
    private fun plain(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
