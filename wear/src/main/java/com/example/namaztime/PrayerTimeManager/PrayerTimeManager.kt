package com.example.namaztime.PrayerTimeManager

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.time.LocalDateTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class PrayerTimeManager {
    suspend fun updateCurrentCity(latitude: Double, longitude: Double) : City {
        val city = MuftiyatKzApiClient().getClosestCityTo(latitude, longitude)
//        val prayeTimes = MuftiyatKzApiClient().getPrayerTimes(city)

        return city
    }
}

data class City (
    val name: String,
    val latitude: Double,
    val longitude: Double,
    )

interface MuftiyatKzAPI {
    @GET("prayer-times/{year}/{lat}/{lng}")
    suspend fun getPrayerTimes(
        @Path("year") year: Int,
        @Path("lat") lat: Double,
        @Path("lng") lng: Double
    ): PrayerTimesResponse

    @GET("cities?page=1&limit=30")
    suspend fun getCities(@Query("page") page: Int): CitiesResponse
}

class MuftiyatKzApiClient {
    private val BASE_URL = "https://api.muftyat.kz/"

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val client: MuftiyatKzAPI = retrofit.create(MuftiyatKzAPI::class.java)

    suspend fun getClosestCityTo(targetLatitude: Double, targetLongitude: Double): City {
        val limit = 30

        var res = this.client.getCities(1)
        val citiesCount = res.count

        var closestCity = City("unknown", 0.0, 0.0)
        var closestDistance = Double.MAX_VALUE

        for (page in 1 .. citiesCount / limit) {
            res = this.client.getCities(page)
            for (city in res.results) {
                val cityLatitude = city.lat.toDouble()
                val cityLongitude = city.lng.toDouble()
                val distance = calculateDistance(targetLatitude, targetLongitude, cityLatitude, cityLongitude)

                if (distance < closestDistance) {
                    closestDistance = distance
                    closestCity = City(city.title, cityLatitude, cityLongitude)
                }
            }
        }

        return closestCity
    }

    private fun calculateDistance(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val R = 6371 // Radius of the Earth in kilometers
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    suspend fun getPrayerTimes(city: City): PrayerTimesResponse {
        val currentYear = LocalDateTime.now().year
        return this.client.getPrayerTimes(currentYear, city.latitude, city.longitude)
    }
}

data class PrayerTimesResponse(
    val success: Boolean,
    val longitude: Double,
    val city_name: String,
    val year: Int,
    val latitude: Double,
    val timezone: String,
    val result: List<PrayerTimesResponseResultItem>
)

data class PrayerTimesResponseResultItem(
    val Fajr: String,
    val Dhuhr: String,
    val Asr: String,
    val Maghrib: String,
    val Isha: String,
    val date: String
)

data class CitiesResponse(
    val count: Int,
    val next: String,
    val previous: String,
    val results: List<CitiesResponseResultItem>
)

data class CitiesResponseResultItem(
    val title: String,
    val lng: String,
    val lat: String,
    val timezone: String,
    val slug: String
)
