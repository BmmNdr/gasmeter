package com.gasmeter.app.data

import androidx.room.TypeConverter
import com.gasmeter.core.TripTag
import java.time.Instant
import java.time.LocalDate

class Converters {
    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    /** Tags are stored as comma-separated enum names; unknown names are ignored. */
    @TypeConverter
    fun tagsToString(value: Set<TripTag>): String = value.sortedBy { it.ordinal }.joinToString(",") { it.name }

    @TypeConverter
    fun stringToTags(value: String): Set<TripTag> =
        value.split(',')
            .filter { it.isNotBlank() }
            .mapNotNull { name -> TripTag.entries.find { it.name == name } }
            .toSet()
}
