package com.gasmeter.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gasmeter.app.data.OpenTrip
import com.gasmeter.app.data.TripRepository
import com.gasmeter.core.TripMetrics
import com.gasmeter.core.Verdict
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant

data class TripRow(
    val id: Long,
    val endedAt: Instant,
    val routeName: String?,
    val metrics: TripMetrics,
    val verdict: Verdict?,
    val counted: Boolean,
)

data class HomeUiState(
    val loading: Boolean = true,
    val openTrip: OpenTrip? = null,
    val openTripRouteName: String? = null,
    val recentTrips: List<TripRow> = emptyList(),
)

class HomeViewModel(private val repository: TripRepository) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repository.openTrip,
        repository.history,
        repository.routes,
    ) { open, history, routes ->
        val names = routes.associate { it.id to it.name }
        HomeUiState(
            loading = false,
            openTrip = open,
            openTripRouteName = open?.routeId?.let(names::get),
            recentTrips = history.asReversed().take(RECENT_LIMIT).map { analyzed ->
                TripRow(
                    id = analyzed.log.id,
                    endedAt = analyzed.log.endedAt,
                    routeName = analyzed.log.routeId?.let(names::get),
                    metrics = analyzed.trip.metrics,
                    verdict = analyzed.verdict,
                    counted = analyzed.trip.countsForStats,
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun discardOpenTrip() {
        viewModelScope.launch { repository.discardOpenTrip() }
    }

    private companion object {
        const val RECENT_LIMIT = 30
    }
}
