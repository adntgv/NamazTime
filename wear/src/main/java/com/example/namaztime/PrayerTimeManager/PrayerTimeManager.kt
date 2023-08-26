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

        val prayerTimes = MuftiyatKzApiClient().getPrayerTimes(city, currentYear)

        if (prayerTimes != null) {
            db.prayerTimesDao().insert(PrayerTimeEntity(id = city.name,   prayerTimesResponse = prayerTimes))
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
            city = getClosestCityTo(latitude, longitude)
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

    private suspend fun getClosestCityTo(targetLatitude: Double, targetLongitude: Double): City {
        val cities = getAllCities()

        var closestCity = City("unknown", 0.0, 0.0)
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
        if (cities.isEmpty()) {
            val res = this.client.getAllCities()
            var id = 1
            for (city in res) {
                db.cityDao().set(CityEntity(
                    id = id,
                    name = city.name,
                    latitude = city.latitude,
                    longitude = city.longitude,
                ))
                id++
            }

            return res
        }

        return cities.map { City(it.name, it.latitude, it.longitude) }
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
        val cityEntity = db.cityDao().get()
        val res = cityEntity?.let { db.prayerTimesDao().get(it.name) }

        val prayerTimes = mutableListOf<PrayerTime>()

        if (res != null) {
            for (prayerTime in res.prayerTimesResponse.result) {
                val prayers = listOf(
                    prayerTime.fajr,
                    prayerTime.dhuhr,
                    prayerTime.asr,
                    prayerTime.maghrib,
                    prayerTime.isha
                )
                for (prayer in prayers) {
                    val dateTime = Calendar.getInstance()
                    val date = prayerTime.date.trim().split("-")
                    dateTime.set(Calendar.YEAR, date[0].trim().toInt())
                    dateTime.set(Calendar.MONTH, date[1].trim().toInt() - 1)
                    dateTime.set(Calendar.DAY_OF_MONTH, date[2].trim().toInt())
                    val pt = prayer.trim().split(":")
                    dateTime.set(Calendar.HOUR_OF_DAY, pt[0].trim().toInt())
                    dateTime.set(Calendar.MINUTE, pt[1].trim().toInt())
                    var name = ""
                    when (prayer) {
                        prayerTime.fajr -> name = "Fajr"
                        prayerTime.dhuhr -> name = "Dhuhr"
                        prayerTime.asr -> name = "Asr"
                        prayerTime.maghrib -> name = "Maghrib"
                        prayerTime.isha -> name = "Isha"
                    }

                    prayerTimes.add(PrayerTime(name, dateTime))
                }
            }
        }

        // sort by date in ascending order
        prayerTimes.sortBy { it.dateTime }

        return prayerTimes
    }

    fun calculateTimeRemaining(currentTime: Calendar, nextPrayerTime: PrayerTime): Long {
        // remaining time in seconds
        return (nextPrayerTime.dateTime.timeInMillis - currentTime.timeInMillis) / 1000
    }
}

data class City (
    val name: String,
    val latitude: Double,
    val longitude: Double,
    )


data class PrayerTime(
    val name: String = "",
    val dateTime: Calendar
)
