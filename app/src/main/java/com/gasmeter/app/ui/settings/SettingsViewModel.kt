package com.gasmeter.app.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmeter.app.data.CarSettings
import com.gasmeter.app.data.StoredFuelPrice
import com.gasmeter.app.data.TripRepository
import com.gasmeter.app.ui.parseDecimal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class SettingsViewModel(
    private val repository: TripRepository,
    private val todayProvider: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    /** Null until loaded. */
    val settings: StateFlow<CarSettings?> =
        repository.carSettings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Newest first. */
    val prices: StateFlow<List<StoredFuelPrice>> =
        repository.fuelPrices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Car form
    var consumptionText by mutableStateOf("")
    var currencyText by mutableStateOf("")
    var stepKm by mutableStateOf<Double?>(null)
    var consumptionError by mutableStateOf<String?>(null)
        private set
    var carSaved by mutableStateOf(false)
        private set

    // Price form
    var priceText by mutableStateOf("")
    var priceDate by mutableStateOf(todayProvider())
    var priceError by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            val current = repository.carSettings.first()
            consumptionText = current.consumptionLitresPer100Km?.let(::plain).orEmpty()
            currencyText = current.currency
            stepKm = current.rangeDisplayStepKm
        }
    }

    fun onCarFieldChanged() {
        carSaved = false
    }

    /** Empty consumption clears it (litres and costs hide again). */
    fun saveCar() {
        val consumption = if (consumptionText.isBlank()) null else parseDecimal(consumptionText)
        consumptionError = when {
            consumptionText.isNotBlank() && consumption == null -> "Enter a number"
            consumption != null && consumption <= 0 -> "Must be more than 0"
            consumption != null && consumption > 50 -> "That's very high; L/100 km is usually 3–20"
            else -> null
        }
        if (consumptionError != null) return
        viewModelScope.launch {
            repository.setConsumption(consumption)
            repository.setCurrency(currencyText)
            stepKm?.let { repository.setRangeDisplayStep(it) }
            currencyText = repository.carSettings.first().currency
            carSaved = true
        }
    }

    fun addPrice() {
        val price = parseDecimal(priceText)
        priceError = when {
            price == null -> "Enter a price"
            price <= 0 -> "Must be more than 0"
            price > 10 -> "That's very high for one litre"
            else -> null
        }
        if (priceError != null) return
        viewModelScope.launch {
            repository.setFuelPrice(priceDate, price!!)
            priceText = ""
            priceDate = today()
        }
    }

    fun deletePrice(id: Long) {
        viewModelScope.launch { repository.deleteFuelPrice(id) }
    }

    /** The price in effect today: the latest one not after today, else the earliest. */
    fun currentPrice(prices: List<StoredFuelPrice>): StoredFuelPrice? =
        prices.firstOrNull { !it.effectiveFrom.isAfter(today()) } ?: prices.lastOrNull()

    fun today(): LocalDate = todayProvider()

    private fun plain(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
