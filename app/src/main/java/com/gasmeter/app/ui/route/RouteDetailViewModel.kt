package com.gasmeter.app.ui.route

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmeter.app.data.CarSettings
import com.gasmeter.app.data.Route
import com.gasmeter.app.data.RouteNameTakenException
import com.gasmeter.app.data.TripRepository
import com.gasmeter.app.ui.ChartPoint
import com.gasmeter.app.ui.TripRow
import com.gasmeter.app.ui.toRow
import com.gasmeter.core.RouteBreakdown
import com.gasmeter.core.RouteStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

data class RouteDetailState(
    val loading: Boolean = true,
    val route: Route? = null,
    val stats: RouteStats? = null,
    val breakdown: RouteBreakdown? = null,
    val chart: List<ChartPoint> = emptyList(),
    /** Newest first. */
    val trips: List<TripRow> = emptyList(),
    val settings: CarSettings = CarSettings(),
)

class RouteDetailViewModel(
    private val repository: TripRepository,
    private val routeId: Long,
    zone: () -> ZoneId = ZoneId::systemDefault,
) : ViewModel() {

    val state: StateFlow<RouteDetailState> = combine(
        repository.routes,
        repository.history,
        repository.carSettings,
    ) { routes, history, settings ->
        val route = routes.find { it.id == routeId }
        val trips = history.filter { it.log.routeId == routeId }
        val names = routes.associate { it.id to it.name }
        RouteDetailState(
            loading = false,
            route = route,
            stats = RouteStats.from(trips.map { it.trip }),
            breakdown = RouteBreakdown.from(trips, zone()),
            chart = trips.mapNotNull { t -> t.trip.comparableRatio?.let { ChartPoint(it, t.verdict) } },
            trips = trips.asReversed().map { it.toRow(names) },
            settings = settings,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RouteDetailState())

    var renaming by mutableStateOf(false)
        private set
    var renameText by mutableStateOf("")
    var renameError by mutableStateOf<String?>(null)
        private set

    fun startRename() {
        renameText = state.value.route?.name.orEmpty()
        renameError = null
        renaming = true
    }

    fun cancelRename() {
        renaming = false
        renameError = null
    }

    fun saveRename() {
        if (renameText.isBlank()) {
            renameError = "Enter a name"
            return
        }
        viewModelScope.launch {
            try {
                repository.renameRoute(routeId, renameText)
                renaming = false
                renameError = null
            } catch (e: RouteNameTakenException) {
                renameError = "Another route is already called \"${e.name}\""
            }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteRoute(routeId)
            onDeleted()
        }
    }
}
