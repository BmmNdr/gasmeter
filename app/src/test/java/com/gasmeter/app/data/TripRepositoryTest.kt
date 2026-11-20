package com.gasmeter.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.gasmeter.core.Rating
import com.gasmeter.core.TripIssue
import com.gasmeter.core.TripTag
import com.gasmeter.core.Verdict
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class TripRepositoryTest {
    private lateinit var db: GasMeterDatabase
    private lateinit var repo: TripRepository
    private var now = Instant.parse("2026-03-02T08:00:00Z")

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext<Context>(),
            GasMeterDatabase::class.java,
        ).allowMainThreadQueries().build()
        repo = TripRepository(db, clock = { now }, zone = { ZoneOffset.UTC })
    }

    @After
    fun tearDown() = db.close()

    private suspend fun drive(routeId: Long?, distance: Double, rangeUsed: Double): Long {
        val last = repo.lastEndReadings() ?: LastReadings(300.0, 10_000.0)
        repo.startTrip(routeId, last.rangeKm, last.odometerKm)
        now = now.plusSeconds(600)
        val id = repo.finishTrip(last.rangeKm - rangeUsed, last.odometerKm + distance)
        now = now.plusSeconds(3600)
        return id
    }

    @Test
    fun startAndFinishTrip_producesSpecExample() = runTest {
        repo.startTrip(null, rangeKm = 300.0, odometerKm = 12_000.0)
        assertNotNull(repo.openTrip.first())

        repo.finishTrip(rangeKm = 295.0, odometerKm = 12_002.0, tags = setOf(TripTag.COLD_ENGINE), note = "  ")

        assertNull(repo.openTrip.first())
        val trip = repo.history.first().single()
        assertEquals(3.0, trip.trip.metrics.excessKm, 1e-9)
        assertEquals(2.5, trip.trip.metrics.ratio!!, 1e-9)
        assertEquals(setOf(TripTag.COLD_ENGINE), trip.log.tags)
        assertNull("blank note is dropped", trip.log.note)
        assertNull("no consumption set, so no litres", trip.trip.metrics.litres)
        assertNull("no route, so no verdict", trip.verdict)
    }

    @Test(expected = TripAlreadyOpenException::class)
    fun onlyOneOpenTripAtATime() = runTest {
        repo.startTrip(null, 300.0, 1000.0)
        repo.startTrip(null, 300.0, 1000.0)
    }

    @Test
    fun finishWithoutOpenTripFails() = runTest {
        val error = runCatching { repo.finishTrip(290.0, 1010.0) }.exceptionOrNull()
        assertTrue(error is NoOpenTripException)
    }

    @Test
    fun odometerGoingBackwardsIsRejectedAndTripStaysOpen() = runTest {
        repo.startTrip(null, 300.0, 1000.0)
        val error = runCatching { repo.finishTrip(295.0, 999.0) }.exceptionOrNull()
        assertTrue(error is InvalidReadingsException)
        assertEquals(listOf<TripIssue>(TripIssue.OdometerWentBackwards), (error as InvalidReadingsException).issues)
        assertNotNull(repo.openTrip.first())
    }

    @Test
    fun nextTripIsPrefilledFromLastEndReadings() = runTest {
        assertNull(repo.lastEndReadings())
        drive(null, distance = 12.0, rangeUsed = 15.0)
        assertEquals(LastReadings(285.0, 10_012.0), repo.lastEndReadings())
    }

    @Test
    fun discardRemovesOpenTrip() = runTest {
        repo.startTrip(null, 300.0, 1000.0)
        repo.discardOpenTrip()
        assertNull(repo.openTrip.first())
        assertTrue(repo.history.first().isEmpty())
    }

    @Test
    fun verdictAppearsOnFourthTripOfRoute() = runTest {
        val route = repo.addRoute("Home → Work")
        repeat(3) { drive(route, distance = 10.0, rangeUsed = 10.0) }
        val fourth = drive(route, distance = 10.0, rangeUsed = 13.0)

        val history = repo.history.first()
        assertEquals(Verdict.NotEnoughHistory(2, 3), history[2].verdict)
        val verdict = history.single { it.log.id == fourth }.verdict as Verdict.Judged
        assertEquals(Rating.EXPENSIVE, verdict.rating)
    }

    @Test
    fun excludingATripRemovesItFromHistoryComparison() = runTest {
        val route = repo.addRoute("Gym")
        val first = drive(route, 10.0, 10.0)
        repeat(2) { drive(route, 10.0, 10.0) }
        repo.setExcluded(first, true)
        drive(route, 10.0, 10.0)

        val history = repo.history.first()
        assertEquals(Verdict.NotJudgeable(Verdict.Reason.NOT_COUNTED), history[0].verdict)
        assertEquals(Verdict.NotEnoughHistory(2, 3), history[3].verdict)
    }

    @Test
    fun addRouteReusesExistingNameIgnoringCase() = runTest {
        val a = repo.addRoute("Home → Work")
        val b = repo.addRoute("  home → work ")
        assertEquals(a, b)
        assertEquals(1, repo.routes.first().size)
    }

    @Test
    fun deletingTripRemovesIt() = runTest {
        val id = drive(null, 5.0, 5.0)
        repo.deleteTrip(id)
        assertTrue(repo.history.first().isEmpty())
    }

    @Test
    fun renameRoute_changesNameEverywhere() = runTest {
        val id = repo.addRoute("Home → Work")
        repo.renameRoute(id, "  Commute ")
        assertEquals(listOf(Route(id, "Commute")), repo.routes.first())
    }

    @Test
    fun renameRoute_rejectsNameOfAnotherRoute() = runTest {
        repo.addRoute("Gym")
        val id = repo.addRoute("Home → Work")
        val error = runCatching { repo.renameRoute(id, "gym") }.exceptionOrNull()
        assertTrue(error is RouteNameTakenException)
        assertEquals("Home → Work", repo.routes.first().single { it.id == id }.name)
    }

    @Test
    fun renameRoute_allowsChangingCaseOfOwnName() = runTest {
        val id = repo.addRoute("gym")
        repo.renameRoute(id, "Gym")
        assertEquals("Gym", repo.routes.first().single().name)
    }

    @Test
    fun deleteRoute_keepsTripsWithoutRoute() = runTest {
        val route = repo.addRoute("Gym")
        val other = repo.addRoute("Work")
        val done = drive(route, 10.0, 10.0)
        drive(other, 10.0, 10.0)
        repo.startTrip(route, 280.0, 10_020.0)

        repo.deleteRoute(route)

        assertEquals(listOf("Work"), repo.routes.first().map { it.name })
        val history = repo.history.first()
        assertEquals(2, history.size)
        assertNull(history.single { it.log.id == done }.log.routeId)
        assertEquals(other, history.single { it.log.id != done }.log.routeId)
        assertNull("open trip loses its route too", repo.openTrip.first()!!.routeId)
    }

    @Test
    fun setTripRoute_movesTripAndChangesComparisons() = runTest {
        val route = repo.addRoute("Work")
        repeat(2) { drive(route, 10.0, 10.0) }
        val stray = drive(null, 10.0, 10.0)
        val fourth = drive(route, 10.0, 13.0)
        assertEquals(Verdict.NotEnoughHistory(2, 3), repo.history.first().single { it.log.id == fourth }.verdict)

        repo.setTripRoute(stray, route)

        val history = repo.history.first()
        assertEquals(route, history.single { it.log.id == stray }.log.routeId)
        val verdict = history.single { it.log.id == fourth }.verdict as Verdict.Judged
        assertEquals(Rating.EXPENSIVE, verdict.rating)

        repo.setTripRoute(stray, null)
        assertNull(repo.history.first().single { it.log.id == stray }.log.routeId)
    }

    @Test
    fun rangeStepIsStored() = runTest {
        assertNull(repo.carSettings.first().rangeDisplayStepKm)
        repo.setRangeDisplayStep(5.0)
        assertEquals(5.0, repo.carSettings.first().rangeDisplayStepKm!!, 0.0)
    }
}
