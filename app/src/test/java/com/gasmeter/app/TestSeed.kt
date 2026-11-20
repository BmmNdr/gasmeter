package com.gasmeter.app

import androidx.test.core.app.ApplicationProvider
import com.gasmeter.app.data.TripRepository
import com.gasmeter.core.TripTag
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.ZoneId

/**
 * Seeds the real app database with trips at chosen times (the app's own
 * repository always uses "now").
 */
class TestSeed {
    private val app = ApplicationProvider.getApplicationContext<GasMeterApp>()
    private var now: Instant = Instant.parse("2026-03-02T08:00:00Z")
    val repository = TripRepository(app.database, clock = { now }, zone = { ZoneId.systemDefault() })

    private var range = 400.0
    private var odometer = 41_200.0

    /** Drives one trip starting at [startLocal] ("2026-03-02T08:00") in the default zone. */
    fun drive(
        routeId: Long?,
        startLocal: String,
        distance: Double,
        rangeUsed: Double,
        tags: Set<TripTag> = emptySet(),
    ): Long = runBlocking {
        now = java.time.LocalDateTime.parse(startLocal).atZone(ZoneId.systemDefault()).toInstant()
        repository.startTrip(routeId, range, odometer)
        now = now.plusSeconds(20 * 60)
        val id = repository.finishTrip(range - rangeUsed, odometer + distance, tags = tags)
        range -= rangeUsed
        odometer += distance
        id
    }

    fun route(name: String): Long = runBlocking { repository.addRoute(name) }

    fun step(km: Double) = runBlocking { repository.setRangeDisplayStep(km) }

    /** Consumption and a fuel price, so costs show. */
    fun costs(litresPer100Km: Double = 6.5, pricePerLitre: Double = 1.789) = runBlocking {
        repository.setConsumption(litresPer100Km)
        repository.setFuelPrice(java.time.LocalDate.of(2026, 3, 1), pricePerLitre)
    }

    /**
     * A commute with morning runs and two heavy-traffic evening runs, plus a
     * one-trip "Gym" route and a trip without route.
     */
    fun commuteHistory(): Ids {
        step(1.0)
        val work = route("Home → Work")
        val gym = route("Gym")
        val traffic = setOf(TripTag.HEAVY_TRAFFIC)
        drive(work, "2026-03-02T08:00", 10.0, 10.0)
        drive(gym, "2026-03-02T12:30", 4.0, 5.0)
        drive(work, "2026-03-03T08:10", 10.0, 11.0)
        val evening1 = drive(work, "2026-03-03T18:00", 10.0, 15.0, traffic)
        drive(work, "2026-03-04T08:05", 10.0, 10.0)
        val evening2 = drive(work, "2026-03-04T18:20", 10.0, 16.0, traffic)
        drive(null, "2026-03-05T07:00", 3.0, 4.0)
        drive(work, "2026-03-05T08:00", 10.0, 10.5)
        return Ids(work, gym, evening1, evening2)
    }

    data class Ids(val work: Long, val gym: Long, val eveningTrip1: Long, val eveningTrip2: Long)
}
