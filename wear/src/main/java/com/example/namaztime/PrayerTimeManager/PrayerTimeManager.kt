package com.example.namaztime.PrayerTimeManager

import android.content.Context
import androidx.room.Room
import java.time.LocalDateTime
import java.util.Calendar
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PrayerTimeManager (applicationContext: Context) {
    private val client: MuftiyatKzApiClient = MuftiyatKzApiClient()
    private val db = Room.databaseBuilder(
        applicationContext,
        AppDatabase::class.java, "namaztime"
    )
        .fallbackToDestructiveMigration()
        .build()

    suspend fun updateCurrentCity(latitude: Double, longitude: Double) : City {
        val city = getCity(latitude, longitude)
        val currentYear = LocalDateTime.now().year

        updatePrayerTimesForCity(city, currentYear)

        return city
    }

    private suspend fun updatePrayerTimesForCity(city: City, currentYear: Int): List<PrayerTime> {
        var prayerTimes = db.prayerTimesDao().get(city.name)
        if (prayerTimes.isEmpty()) {
            val res = client.getPrayerTimes(city, currentYear)
            for (prayerTime in res) {
                db.prayerTimesDao().insert(prayerTime)
            }

            prayerTimes = res
        }

        return prayerTimes
    }

    private suspend fun getCity(latitude: Double, longitude: Double): City {
        val city =  getClosestCityTo(latitude, longitude)

        db.activeCityDao().set(ActiveCity(0, city.name))

        return city
    }

    private suspend fun getClosestCityTo(targetLatitude: Double, targetLongitude: Double): City {
        val cities = getAllCities()

        var closestCity = City(0, "unknown", 0.0, 0.0)
        var closestDistance = Double.MAX_VALUE

        for (city in cities) {
            val distance = calculateDistance(targetLatitude, targetLongitude, city.latitude, city.longitude)

            if (distance < closestDistance) {
                closestDistance = distance
                closestCity = city
            }
        }

        return closestCity
    }

    private suspend fun getAllCities(): List<City> {
        val cities = db.cityDao().all()
        if (cities.isEmpty() || cities.size == 1) {
            val res = this.client.getAllCities()
            for (city in res) {
                db.cityDao().set(city)
            }

            return res
        }

        return cities
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Radius of the Earth in kilometers
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    fun getPrayerTimes(): List<PrayerTime> {
        val activeCity = db.activeCityDao().get() ?: return listOf()

        val prayerTimes = db.prayerTimesDao().get(activeCity.name)
        if (prayerTimes.isEmpty()) {
            return listOf()
        }

        return prayerTimes
    }

    fun calculateTimeRemaining(currentTime: Calendar, nextPrayerTime: PrayerTime): Long {
        // remaining time in seconds
        return (nextPrayerTime.dateTime.timeInMillis - currentTime.timeInMillis) / 1000
    }
}

