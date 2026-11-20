package com.gasmeter.app.data

import androidx.room.withTransaction
import com.gasmeter.core.AnalyzedTrip
import com.gasmeter.core.CarProfile
import com.gasmeter.core.FuelPrice
import com.gasmeter.core.FuelPriceHistory
import com.gasmeter.core.TripHistoryAnalyzer
import com.gasmeter.core.TripIssue
import com.gasmeter.core.TripLog
import com.gasmeter.core.TripReadings
import com.gasmeter.core.TripTag
import com.gasmeter.core.TripValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId

data class Route(val id: Long, val name: String)

data class OpenTrip(
    val id: Long,
    val routeId: Long?,
    val startedAt: Instant,
    val rangeStartKm: Double,
    val odometerStartKm: Double,
)

/** Car settings as entered by the user; null means "not set yet". */
data class CarSettings(
    val consumptionLitresPer100Km: Double? = null,
    val rangeDisplayStepKm: Double? = null,
    val currency: String? = null,
) {
    /** Falls back to a 1 km step when the user hasn't chosen one. */
    fun toProfile() = CarProfile(consumptionLitresPer100Km, rangeDisplayStepKm ?: 1.0)
}

/** The last end readings, used to prefill the next trip's start. */
data class LastReadings(val rangeKm: Double, val odometerKm: Double)

class TripAlreadyOpenException : IllegalStateException("A trip is already in progress")
class NoOpenTripException : IllegalStateException("No trip is in progress")
class RouteNameTakenException(val name: String) :
    IllegalArgumentException("Another route is already called $name")
class InvalidReadingsException(val issues: List<TripIssue>) :
    IllegalArgumentException("Invalid readings: $issues")

class TripRepository(
    private val db: GasMeterDatabase,
    private val clock: () -> Instant = Instant::now,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
) {
    private val routeDao = db.routeDao()
    private val tripDao = db.tripDao()
    private val settingsDao = db.settingsDao()

    val routes: Flow<List<Route>> =
        routeDao.observeAll().map { list -> list.map { Route(it.id, it.name) } }

    val openTrip: Flow<OpenTrip?> = tripDao.observeOpen().map { it?.toOpenTrip() }

    val carSettings: Flow<CarSettings> = settingsDao.observeCarSettings().map { it.toSettings() }

    /** All completed trips with metrics and verdicts, oldest first. */
    val history: Flow<List<AnalyzedTrip>> = combine(
        tripDao.observeCompleted(),
        carSettings,
        settingsDao.observeFuelPrices(),
    ) { trips, settings, prices ->
        TripHistoryAnalyzer.analyze(
            logs = trips.mapNotNull { it.toTripLog() },
            car = settings.toProfile(),
            prices = FuelPriceHistory(prices.map { FuelPrice(it.effectiveFrom, it.pricePerLitre) }),
            zone = zone(),
        )
    }.flowOn(Dispatchers.Default)

    suspend fun lastEndReadings(): LastReadings? =
        tripDao.getLatestCompleted()?.let { trip ->
            LastReadings(trip.rangeEndKm ?: return null, trip.odometerEndKm ?: return null)
        }

    /** Adds a route, or returns the id of an existing route with the same name (ignoring case). */
    suspend fun addRoute(name: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Route name must not be empty" }
        return db.withTransaction {
            routeDao.findByName(trimmed)?.id
                ?: routeDao.insert(RouteEntity(name = trimmed, createdAt = clock()))
        }
    }

    /**
     * Renames a route. Throws [RouteNameTakenException] if another route already
     * has that name (ignoring case); changing only the case of its own name is fine.
     */
    suspend fun renameRoute(routeId: Long, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Route name must not be empty" }
        db.withTransaction {
            val route = routeDao.getById(routeId) ?: return@withTransaction
            val clash = routeDao.findByName(trimmed)
            if (clash != null && clash.id != routeId) throw RouteNameTakenException(trimmed)
            routeDao.update(route.copy(name = trimmed))
        }
    }

    /** Deletes a route. Its trips (including an open one) are kept, without a route. */
    suspend fun deleteRoute(routeId: Long) {
        db.withTransaction {
            tripDao.clearRoute(routeId)
            routeDao.delete(routeId)
        }
    }

    /** Moves a trip to another route, or to none when [routeId] is null. */
    suspend fun setTripRoute(tripId: Long, routeId: Long?) = updateTrip(tripId) {
        it.copy(routeId = routeId)
    }

    /** Starts a trip. Throws [TripAlreadyOpenException] if one is already in progress. */
    suspend fun startTrip(routeId: Long?, rangeKm: Double, odometerKm: Double): Long {
        require(rangeKm >= 0 && odometerKm >= 0) { "Readings must not be negative" }
        return db.withTransaction {
            if (tripDao.getOpen() != null) throw TripAlreadyOpenException()
            tripDao.insert(
                TripEntity(
                    routeId = routeId,
                    startedAt = clock(),
                    rangeStartKm = rangeKm,
                    odometerStartKm = odometerKm,
                ),
            )
        }
    }

    /**
     * Ends the open trip and returns its id. Throws [InvalidReadingsException] for
     * blocking errors (warnings are the caller's job to show before calling).
     */
    suspend fun finishTrip(
        rangeKm: Double,
        odometerKm: Double,
        refuelled: Boolean = false,
        tags: Set<TripTag> = emptySet(),
        note: String? = null,
    ): Long = db.withTransaction {
        val open = tripDao.getOpen() ?: throw NoOpenTripException()
        val readings = TripReadings(open.rangeStartKm, rangeKm, open.odometerStartKm, odometerKm)
        val profile = settingsDao.getCarSettings().toSettings().toProfile()
        val blocking = TripValidator.validate(readings, profile).filter { it.isBlocking }
        if (blocking.isNotEmpty()) throw InvalidReadingsException(blocking)

        tripDao.update(
            open.copy(
                endedAt = clock(),
                rangeEndKm = rangeKm,
                odometerEndKm = odometerKm,
                refuelledDuringTrip = refuelled,
                tags = tags,
                note = note?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
        open.id
    }

    suspend fun discardOpenTrip() {
        db.withTransaction { tripDao.getOpen()?.let { tripDao.delete(it.id) } }
    }

    suspend fun setExcluded(tripId: Long, excluded: Boolean) = updateTrip(tripId) {
        it.copy(excludedFromStats = excluded)
    }

    suspend fun setRefuelled(tripId: Long, refuelled: Boolean) = updateTrip(tripId) {
        it.copy(refuelledDuringTrip = refuelled)
    }

    suspend fun deleteTrip(tripId: Long) = tripDao.delete(tripId)

    suspend fun setRangeDisplayStep(stepKm: Double) {
        require(stepKm > 0) { "Step must be positive" }
        db.withTransaction {
            val current = settingsDao.getCarSettings() ?: CarSettingsEntity()
            settingsDao.upsertCarSettings(current.copy(rangeDisplayStepKm = stepKm))
        }
    }

    private suspend fun updateTrip(tripId: Long, change: (TripEntity) -> TripEntity) {
        db.withTransaction {
            tripDao.getById(tripId)?.let { tripDao.update(change(it)) }
        }
    }
}

private fun TripEntity.toOpenTrip() = OpenTrip(id, routeId, startedAt, rangeStartKm, odometerStartKm)

private fun CarSettingsEntity?.toSettings() = CarSettings(
    consumptionLitresPer100Km = this?.consumptionLitresPer100Km,
    rangeDisplayStepKm = this?.rangeDisplayStepKm,
    currency = this?.currency,
)

private fun TripEntity.toTripLog(): TripLog? {
    val end = endedAt ?: return null
    return TripLog(
        id = id,
        routeId = routeId,
        startedAt = startedAt,
        endedAt = end,
        readings = TripReadings(
            rangeStartKm = rangeStartKm,
            rangeEndKm = rangeEndKm ?: return null,
            odometerStartKm = odometerStartKm,
            odometerEndKm = odometerEndKm ?: return null,
        ),
        refuelledDuringTrip = refuelledDuringTrip,
        excludedFromStats = excludedFromStats,
        tags = tags,
        note = note,
    )
}
