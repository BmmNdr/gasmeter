package com.gasmeter.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [RouteEntity::class, TripEntity::class, FuelPriceEntity::class, CarSettingsEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class GasMeterDatabase : RoomDatabase() {
    abstract fun routeDao(): RouteDao
    abstract fun tripDao(): TripDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        fun create(context: Context): GasMeterDatabase =
            Room.databaseBuilder(context, GasMeterDatabase::class.java, "gasmeter.db").build()
    }
}
