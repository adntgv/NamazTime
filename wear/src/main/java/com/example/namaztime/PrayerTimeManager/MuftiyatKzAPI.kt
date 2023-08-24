package com.example.namaztime.PrayerTimeManager

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

interface MuftiyatKzAPI {
    @GET("prayer-times/{year}/{lat}/{lng}")
    suspend fun getPrayerTimes(
        @Path("year") year: Int,
        @Path("lat") lat: Double,
        @Path("lng") lng: Double
    ): PrayerTimesResponse?

    @GET("cities?page=1&limit=30")
    suspend fun getCities(@Query("page") page: Int): CitiesResponse
}

/**
 * {"success": true, "longitude": 57.166667, "city_name": "\u0410\u049b\u0442\u04e9\u0431\u0435", "result": [{"Asr": "15:37 ", "Isha": "18:58 ", "Sunrise": "09:06 ", "Maghrib": "17:23 ", "date": "01-01-2016", "Dhuhr": "13:20 ", "Fajr": "07:31 "}, ... ], "year": 2016, "latitude": 50.3, "timezone": "5"}
 */
data class PrayerTimesResponse(
    val success: Boolean,
    val longitude: String,
    val city: String,
    val result: List<PrayerTimesResponseResultItem>,
    val year: Int,
    val latitude: String,
    val timezone: String
)

/**
 * {"Asr": "15:37 ", "Isha": "18:58 ", "Sunrise": "09:06 ", "Maghrib": "17:23 ", "date": "01-01-2016", "Dhuhr": "13:20 ", "Fajr": "07:31 "}
 */
data class PrayerTimesResponseResultItem(
    val asr: String,
    val isha: String,
    val maghrib: String,
    @SerializedName("Date") val date: String,
    val dhuhr: String,
    val fajr: String
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

    suspend fun getPrayerTimes(city: City): List<PrayerTimeEntity> {
        val currentYear = LocalDateTime.now().year
        val prayerTimes =  this.client.getPrayerTimes(currentYear, city.latitude, city.longitude)
        if (prayerTimes == null) {
            return listOf()
        }
        val prayerTimeEntities = mutableListOf<PrayerTimeEntity>()

        for (prayerTime in prayerTimes.result) {
            val listOfDayPrayers = mapOf(
                Pair("Fajr", prayerTime.fajr.trim()),
                Pair("Dhuhr",  prayerTime.dhuhr.trim()),
                Pair("Asr", prayerTime.asr.trim()),
                Pair("Maghrib", prayerTime.maghrib.trim()),
                Pair("Isha", prayerTime.isha.trim()),
            )

            for (dayPrayer in listOfDayPrayers) {
                prayerTimeEntities.add(PrayerTimeEntity(0, city.name,dayPrayer.key, timeToDateToLong(prayerTime.date, dayPrayer.value)))
            }
        }

        return prayerTimeEntities
    }

    private fun timeToDateToLong(date: String, hm: String): Long {
        val dateTimeString = "$date $hm"

        val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Set the desired time zone

        val dateObject: Date? = dateFormat.parse(dateTimeString)

        if (dateObject != null) {
            return dateObject.time
        }

        return 0
    }
}
