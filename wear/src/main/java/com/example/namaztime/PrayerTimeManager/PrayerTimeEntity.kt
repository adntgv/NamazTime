package com.example.namaztime.PrayerTimeManager

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "prayer_times")
data class PrayerTimeEntity(
    @PrimaryKey(autoGenerate = false) val id: String,
    @ColumnInfo(name = "prayer_times") val prayerTimesResponse: PrayerTimesResponse,
)

class PrayerTimesResponseConverter{
    @TypeConverter
    fun fromPrayerTimesResponse(prayerTimesResponse: PrayerTimesResponse): String {
        return Gson().toJson(prayerTimesResponse)
    }
    @TypeConverter
    fun toPrayerTimesResponse(prayerTimesResponse: String): PrayerTimesResponse {
        val listType = object : TypeToken<PrayerTimesResponse>() {}.type
        return Gson().fromJson(prayerTimesResponse, listType)
    }
}

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = false) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
)

@TypeConverters(PrayerTimesResponseConverter::class)
@Database(entities = [PrayerTimeEntity::class, CityEntity::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun cityDao(): CityDao
}

@Dao
interface PrayerTimesDao {
    @Query("SELECT * FROM prayer_times WHERE id = :cityName LIMIT 1 ")
    fun get(cityName: String): PrayerTimeEntity?

    @Upsert
    fun insert(prayerTimes: PrayerTimeEntity)
}

@Dao
interface CityDao {
    @Query("SELECT * FROM cities Limit 1")
    fun get(): CityEntity?

    @Upsert
    fun set(city: CityEntity)
}