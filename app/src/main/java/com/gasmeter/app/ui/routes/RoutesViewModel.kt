package com.gasmeter.app.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmeter.app.data.TripRepository
import com.gasmeter.core.RouteStats
import com.gasmeter.core.Verdict
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class RouteListItem(
    val id: Long,
    val name: String,
    val tripCount: Int,
    val medianRatio: Double?,
    val lastVerdict: Verdict?,
    val averageCost: Double?,
    val currency: String,
)

data class RoutesUiState(
    val loading: Boolean = true,
    val routes: List<RouteListItem> = emptyList(),
)

class RoutesViewModel(repository: TripRepository) : ViewModel() {

    val state: StateFlow<RoutesUiState> = combine(
        repository.routes,
        repository.history,
        repository.carSettings,
    ) { routes, history, settings ->
        val byRoute = history.groupBy { it.log.routeId }
        RoutesUiState(
            loading = false,
            routes = routes.map { route ->
                val trips = byRoute[route.id].orEmpty()
                val stats = RouteStats.from(trips.map { it.trip })
                RouteListItem(
                    id = route.id,
                    name = route.name,
                    tripCount = stats.tripCount,
                    medianRatio = stats.medianRatio,
                    lastVerdict = trips.lastOrNull()?.verdict,
                    averageCost = stats.averageCost,
                    currency = settings.currency,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutesUiState())
}
