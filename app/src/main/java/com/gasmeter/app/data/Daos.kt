package com.gasmeter.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RouteDao {
    @Query("SELECT * FROM routes ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RouteEntity>>

    @Query("SELECT * FROM routes WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): RouteEntity?

    @Query("SELECT * FROM routes WHERE id = :id")
    suspend fun getById(id: Long): RouteEntity?

    @Insert
    suspend fun insert(route: RouteEntity): Long

    @Update
    suspend fun update(route: RouteEntity)

    @Query("DELETE FROM routes WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TripDao {
    @Query("SELECT * FROM trips WHERE ended_at IS NULL LIMIT 1")
    fun observeOpen(): Flow<TripEntity?>

    @Query("SELECT * FROM trips WHERE ended_at IS NULL LIMIT 1")
    suspend fun getOpen(): TripEntity?

    @Query("SELECT * FROM trips WHERE ended_at IS NOT NULL ORDER BY ended_at")
    fun observeCompleted(): Flow<List<TripEntity>>

    @Query("SELECT * FROM trips WHERE ended_at IS NOT NULL ORDER BY ended_at DESC, id DESC LIMIT 1")
    suspend fun getLatestCompleted(): TripEntity?

    @Query("SELECT * FROM trips WHERE id = :id")
    suspend fun getById(id: Long): TripEntity?

    @Insert
    suspend fun insert(trip: TripEntity): Long

    @Update
    suspend fun update(trip: TripEntity)

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun delete(id: Long)

    /** Detaches every trip (open or completed) from a route. */
    @Query("UPDATE trips SET route_id = NULL WHERE route_id = :routeId")
    suspend fun clearRoute(routeId: Long)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM car_settings WHERE id = ${CarSettingsEntity.SINGLETON_ID}")
    fun observeCarSettings(): Flow<CarSettingsEntity?>

    @Query("SELECT * FROM car_settings WHERE id = ${CarSettingsEntity.SINGLETON_ID}")
    suspend fun getCarSettings(): CarSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCarSettings(settings: CarSettingsEntity)

    @Query("SELECT * FROM fuel_prices ORDER BY effective_from")
    fun observeFuelPrices(): Flow<List<FuelPriceEntity>>

    /** One price per date: replaces the existing price for the same date. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFuelPrice(price: FuelPriceEntity)

    @Query("DELETE FROM fuel_prices WHERE id = :id")
    suspend fun deleteFuelPrice(id: Long)
}
