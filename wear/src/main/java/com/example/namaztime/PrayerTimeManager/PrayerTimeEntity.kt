package com.example.namaztime.PrayerTimeManager

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "prayer_times")
data class PrayerTimeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "city_name") val cityName: String,
    @ColumnInfo(name = "prayer_name") val prayerName: String,
    @ColumnInfo(name = "prayer_time") val prayerTime: Long,
)

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = false) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
)

@Database(entities = [PrayerTimeEntity::class, CityEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun cityDao(): CityDao
}

@Dao
interface PrayerTimesDao {
    @Query("SELECT * FROM prayer_times")
    fun getAll(): List<PrayerTimeEntity>

    @Query("SELECT * FROM prayer_times WHERE city_name = :cityName LIMIT 1 ")
    fun first(cityName: String): PrayerTimeEntity?

    @Insert
    fun insert( prayerTimes: PrayerTimeEntity)

    @Query("SELECT * FROM prayer_times WHERE city_name = :cityName and prayer_time > :currentTime ORDER BY prayer_time ASC LIMIT 1")
    fun closest(cityName: String, currentTime: Long): PrayerTimeEntity?
}

@Dao
interface CityDao {
    @Query("SELECT * FROM cities Limit 1")
    fun get(): CityEntity?

    @Upsert
    fun set(city: CityEntity)
}