package com.gasmeter.app.ui.end

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmeter.app.data.CarSettings
import com.gasmeter.app.data.InvalidReadingsException
import com.gasmeter.app.data.NoOpenTripException
import com.gasmeter.app.data.OpenTrip
import com.gasmeter.app.data.Route
import com.gasmeter.app.data.TripRepository
import com.gasmeter.app.ui.parseDecimal
import com.gasmeter.core.TripIssue
import com.gasmeter.core.TripReadings
import com.gasmeter.core.TripTag
import com.gasmeter.core.TripValidator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EndTripContext(
    val openTrip: OpenTrip?,
    val routeName: String?,
    val settings: CarSettings,
)

/** What the typed end readings mean, shown live while the user types. */
data class EndPreview(
    val distanceKm: Double,
    val rangeUsedKm: Double,
    val issues: List<TripIssue>,
) {
    val blocking: Boolean get() = issues.any { it.isBlocking }
}

class EndTripViewModel(private val repository: TripRepository) : ViewModel() {

    /** Null until loaded. */
    val context: StateFlow<EndTripContext?> = combine(
        repository.openTrip,
        repository.routes,
        repository.carSettings,
    ) { open, routes: List<Route>, settings ->
        EndTripContext(open, routes.find { it.id == open?.routeId }?.name, settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    var rangeText by mutableStateOf("")
    var odometerText by mutableStateOf("")
    var refuelled by mutableStateOf(false)
    var tags by mutableStateOf(emptySet<TripTag>())
    var note by mutableStateOf("")

    var rangeError by mutableStateOf<String?>(null)
        private set
    var odometerError by mutableStateOf<String?>(null)
        private set
    var generalError by mutableStateOf<String?>(null)
        private set
    var saving by mutableStateOf(false)
        private set

    fun toggleTag(tag: TripTag) {
        tags = if (tag in tags) tags - tag else tags + tag
    }

    /** Null until both end readings parse. */
    fun preview(ctx: EndTripContext): EndPreview? {
        val open = ctx.openTrip ?: return null
        val range = parseDecimal(rangeText) ?: return null
        val odometer = parseDecimal(odometerText) ?: return null
        val readings = TripReadings(open.rangeStartKm, range, open.odometerStartKm, odometer)
        return EndPreview(
            distanceKm = odometer - open.odometerStartKm,
            rangeUsedKm = open.rangeStartKm - range,
            issues = TripValidator.validate(readings, ctx.settings.toProfile()),
        )
    }

    fun save(onSaved: (tripId: Long) -> Unit) {
        val range = parseDecimal(rangeText)
        val odometer = parseDecimal(odometerText)
        rangeError = if (range == null) "Enter a number" else null
        odometerError = if (odometer == null) "Enter a number" else null
        if (range == null || odometer == null || saving) return

        saving = true
        generalError = null
        viewModelScope.launch {
            try {
                val id = repository.finishTrip(range, odometer, refuelled, tags, note)
                onSaved(id)
            } catch (e: InvalidReadingsException) {
                generalError = e.issues.joinToString("\n") { it.message() }
            } catch (e: NoOpenTripException) {
                generalError = "There's no trip in progress."
            } finally {
                saving = false
            }
        }
    }
}

fun TripIssue.message(): String = when (this) {
    TripIssue.NegativeReading -> "Readings can't be negative."
    TripIssue.OdometerWentBackwards -> "The end odometer is lower than the start odometer."
    is TripIssue.ImplausibleDistance -> "That's a very long trip. Double-check the odometer."
    is TripIssue.PossibleRefuel -> "The range went up a lot. Did you refuel during the trip?"
}
