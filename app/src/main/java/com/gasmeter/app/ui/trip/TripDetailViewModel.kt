package com.gasmeter.app.ui.trip

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmeter.app.data.CarSettings
import com.gasmeter.app.data.TripRepository
import com.gasmeter.core.AnalyzedTrip
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TripDetailState(
    val loading: Boolean = true,
    val trip: AnalyzedTrip? = null,
    val routeName: String? = null,
    val settings: CarSettings = CarSettings(),
)

class TripDetailViewModel(
    private val repository: TripRepository,
    private val tripId: Long,
) : ViewModel() {

    val state: StateFlow<TripDetailState> = combine(
        repository.history,
        repository.routes,
        repository.carSettings,
    ) { history, routes, settings ->
        val trip = history.find { it.log.id == tripId }
        TripDetailState(
            loading = false,
            trip = trip,
            routeName = trip?.log?.routeId?.let { id -> routes.find { it.id == id }?.name },
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TripDetailState())

    fun setExcluded(excluded: Boolean) {
        viewModelScope.launch { repository.setExcluded(tripId, excluded) }
    }

    fun setRefuelled(refuelled: Boolean) {
        viewModelScope.launch { repository.setRefuelled(tripId, refuelled) }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteTrip(tripId)
            onDeleted()
        }
    }
}
