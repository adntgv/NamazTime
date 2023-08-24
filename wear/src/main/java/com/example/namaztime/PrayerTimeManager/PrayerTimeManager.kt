package com.example.namaztime.PrayerTimeManager

import android.content.Context
import androidx.room.Room
import java.util.Date

class PrayerTimeManager (applicationContext: Context) {
    private val db = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java, "namaztime"
    )
        .fallbackToDestructiveMigration()
        .build()

    suspend fun updateCurrentCity(latitude: Double, longitude: Double) : City {
        val city = getCity(latitude, longitude)
        val prayerTimes = MuftiyatKzApiClient().getPrayerTimes(city)

        val check = db.prayerTimesDao().first(city.name)

        if (check == null) {
            prayerTimes.map {
                db.prayerTimesDao().insert(it)
            }
        }

        return city
    }

    private suspend fun getCity(latitude: Double, longitude: Double): City {
        val city: City
        var cityEntity = db.cityDao().get()

        if (cityEntity != null) {
            city = City(
                name = cityEntity.name,
                latitude = cityEntity.latitude,
                longitude = cityEntity.longitude,
            )
        } else {
            city = MuftiyatKzApiClient().getClosestCityTo(latitude, longitude)
            cityEntity = CityEntity(
                id = 0,
                name = city.name,
                latitude = city.latitude,
                longitude = city.longitude,
            )
            db.cityDao().set(cityEntity)
        }

        return city
    }

    private fun getFirstCity(): City? {
        val cityEntity: CityEntity = db.cityDao().get() ?: return null

        return City(
            name = cityEntity.name,
            latitude = cityEntity.latitude,
            longitude = cityEntity.longitude,
        )
    }

    fun getClosestPrayerTime(): PrayerTimeEntity? {
        val city = getFirstCity()
            ?: return null

        return db.prayerTimesDao().closest(city.name, Date().time)
    }
}

data class City (
    val name: String,
    val latitude: Double,
    val longitude: Double,
    )
