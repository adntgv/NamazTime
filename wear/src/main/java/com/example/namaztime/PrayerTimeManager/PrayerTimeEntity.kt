package com.example.namaztime.PrayerTimeManager

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Calendar

@Entity(tableName = "prayer_times")
data class PrayerTime(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "city") val cityName: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "dateTime") val dateTime: Calendar
    )

class CalendarConverter {
    @TypeConverter
    fun fromCalendar(dateTime: Calendar): String {
        return Gson().toJson(dateTime)
    }

    @TypeConverter
    fun toCalendar(dateTime: String): Calendar {
        val type = object : TypeToken<Calendar>() {}.type
        return Gson().fromJson(dateTime, type)
    }
}

@Dao
interface PrayerTimesDao {
    @Query("SELECT * FROM prayer_times WHERE city = :cityName LIMIT 1 ")
    fun get(cityName: String): List<PrayerTime>

    @Upsert
    fun insert(prayerTime: PrayerTime)
}

@Entity(tableName = "cities")
data class City(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "latitude") val latitude: Double,
    @ColumnInfo(name = "longitude") val longitude: Double,
)

@Dao
interface CityDao {
    @Query("SELECT * FROM cities")
    fun all(): List<City>

    @Upsert
    fun set(city: City)
}


@Entity(tableName = "active_city")
data class ActiveCity(
    @PrimaryKey(autoGenerate = false) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
)

@Dao
interface ActiveCityDao {
    @Query("SELECT * FROM active_city LIMIT 1")
    fun get(): ActiveCity?

    @Upsert
    fun set(activeCity: ActiveCity)
}

@TypeConverters(CalendarConverter::class)
@Database(entities = [PrayerTime::class, City::class, ActiveCity::class ], version = 5, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun prayerTimesDao(): PrayerTimesDao
    abstract fun cityDao(): CityDao
    abstract fun activeCityDao(): ActiveCityDao
}