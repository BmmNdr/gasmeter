package com.gasmeter.app.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.gasmeter.core.TripTag
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "routes", indices = [Index(value = ["name"], unique = true)])
data class RouteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)

/**
 * A trip. While it is in progress, [endedAt], [rangeEndKm] and [odometerEndKm] are
 * null; at most one trip is open at a time (enforced by [TripRepository]).
 */
@Entity(
    tableName = "trips",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["route_id"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("route_id"), Index("ended_at")],
)
data class TripEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "route_id") val routeId: Long?,
    @ColumnInfo(name = "started_at") val startedAt: Instant,
    @ColumnInfo(name = "ended_at") val endedAt: Instant? = null,
    @ColumnInfo(name = "range_start_km") val rangeStartKm: Double,
    @ColumnInfo(name = "odometer_start_km") val odometerStartKm: Double,
    @ColumnInfo(name = "range_end_km") val rangeEndKm: Double? = null,
    @ColumnInfo(name = "odometer_end_km") val odometerEndKm: Double? = null,
    @ColumnInfo(name = "refuelled") val refuelledDuringTrip: Boolean = false,
    @ColumnInfo(name = "excluded") val excludedFromStats: Boolean = false,
    val tags: Set<TripTag> = emptySet(),
    val note: String? = null,
)

@Entity(tableName = "fuel_prices", indices = [Index(value = ["effective_from"], unique = true)])
data class FuelPriceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "effective_from") val effectiveFrom: LocalDate,
    @ColumnInfo(name = "price_per_litre") val pricePerLitre: Double,
)

/** Single-row table (id is always [SINGLETON_ID]). Null values mean "not set yet". */
@Entity(tableName = "car_settings")
data class CarSettingsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "consumption_l_per_100km") val consumptionLitresPer100Km: Double? = null,
    @ColumnInfo(name = "range_step_km") val rangeDisplayStepKm: Double? = null,
    val currency: String? = null,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
