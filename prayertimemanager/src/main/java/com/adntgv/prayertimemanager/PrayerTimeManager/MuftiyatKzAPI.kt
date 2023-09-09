package com.adntgv.prayertimemanager.PrayerTimeManager

import com.google.gson.annotations.SerializedName
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.Calendar

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
    val fajr: String,
    val sunrise: String
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

    suspend fun getPrayerTimes(city: City, year: Int): List<PrayerTime> {
        val res =  this.client.getPrayerTimes(year, city.latitude, city.longitude)

        val prayerTimes = mutableListOf<PrayerTime>()

        if (res != null) {
            var id = 0
            for (prayerTime in res.result) {
                val prayers = listOf(
                    prayerTime.fajr,
                    prayerTime.sunrise,
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
                        prayerTime.sunrise -> name = "Sunrise"
                        prayerTime.dhuhr -> name = "Dhuhr"
                        prayerTime.asr -> name = "Asr"
                        prayerTime.maghrib -> name = "Maghrib"
                        prayerTime.isha -> name = "Isha"
                    }

                    prayerTimes.add(PrayerTime(id, city.name, name, dateTime))
                    id++
                }
            }
        }

        // sort by date in ascending order
        prayerTimes.sortBy { it.dateTime }

        return prayerTimes
    }

    suspend fun getAllCities(): List<City> {
        val cities = mutableListOf<City>()

        var res = this.client.getCities(1)
        val citiesCount = res.count

        var id = 0
        for (page in 1 .. citiesCount / 30) {
            res = this.client.getCities(page)
            for (city in res.results) {
                cities.add(City(id, city.title, city.lat.toDouble(), city.lng.toDouble()))
                id++
            }
        }

        return cities
    }
}
