/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.adntgv.namaztime

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.adntgv.namaztime.ui.theme.NamazTimeTheme
import com.adntgv.prayertimemanager.PrayerTimeManager.City
import com.adntgv.prayertimemanager.PrayerTimeManager.PrayerTime
import com.adntgv.prayertimemanager.PrayerTimeManager.PrayerTimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : ComponentActivity(), LocationListener {
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private var city = mutableStateOf(
        City(
            0,
            "Hi",
            0.0,
            0.0
        )
    )
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var prayerTimeManager: PrayerTimeManager
    private var nextPrayerTime = mutableStateOf(
        PrayerTime(
            0,
            "",
            "",
            Calendar.getInstance()
        )
    )
    private var cities: List<City> = listOf(
        City(0, "", 0.0, 0.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        prayerTimeManager =
            PrayerTimeManager(applicationContext)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                update()
            }
        }

        getLocation()

        setContent {
            val cityName = rememberSaveable(city.value.name) {
                city.value.name
            }

            WearApp(cityName)
        }
    }

    @Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() {
        WearApp("Астана")
    }

    @Composable
    fun WearApp(cityName: String) {
        val name = remember {
            nextPrayerTime.value.name
        }

        val formatter = SimpleDateFormat("HH:mm")
        val formattedTime = remember {
            mutableStateOf(formatter.format(nextPrayerTime.value.dateTime.time))
        }
        val time = rememberSaveable{
            formattedTime.value
        }

        NamazTimeTheme {
            /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
             * version of LazyColumn for wear devices with some added features. For more information,
             * see d.android.com/wear/compose.
             */
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .verticalScroll(rememberScrollState())
                ,
                verticalArrangement = Arrangement.Center,

                ) {
                Spacer(modifier = Modifier.height(16.dp))
                ShowCityName(city.value.name)
                Spacer(modifier = Modifier.height(16.dp))
                PrayerTimeView(name, time)
                Spacer(modifier = Modifier.height(16.dp))
                Update()
                Spacer(modifier = Modifier.height(16.dp))
                CitySelector(cities)
            }
        }
    }

    @Composable
    private fun CitySelector(cities: List<City>) {
        Column(
            modifier = Modifier.fillMaxWidth()
            // scrollable

        ) {
            for (c in cities) {
                if (c.name == "" || c.name == city.value.name) {
                    continue
                }
                CitySelectorButton(c.name)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }


    @Composable
    private fun CitySelectorButton(cityName: String) {
        val coroutineScope = rememberCoroutineScope()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        selectCity(cityName)
                    }
                }
            },
        ) {
            Text(text = cityName)
        }
    }

     private suspend fun selectCity(cityName: String) {
        city.value = prayerTimeManager.selectCity(cityName)

        updatePrayerTimesForCity(cityName)
    }

    private suspend fun updatePrayerTimesForCity(cityName: String) {
        nextPrayerTime.value =
            prayerTimeManager.getNextPrayerTime(cityName).first
    }

    @Composable
    fun ShowCityName(cityName: String) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = Color.White,
            text =  cityName
        )
    }

    @Composable
    fun Update() {
        val coroutineScope = rememberCoroutineScope()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        update()
                    }
                }
            },
        ) {
            Text(text = "Обновить")
        }
    }

    private suspend fun update() {
            cities = prayerTimeManager.getAllCities()

            city.value =
                    prayerTimeManager.updateCurrentCity(latitude, longitude)

            updatePrayerTimesForCity(city.value.name)
    }

    @Composable
    private fun PrayerTimeView(name: String, time: String) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Black),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(text = name, textAlign = TextAlign.Center , color = Color.White)
            Text(text = time, textAlign = TextAlign.Center,  color = Color.White)
        }
    }

    private fun getLocation() {
        if ((ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }

        val provider = getLocationProvider()

        locationManager.requestLocationUpdates(provider, 5000, 5f, this)
    }

    private fun getLocationProvider(): String {
        return LocationManager.FUSED_PROVIDER
    }

    @Deprecated("Deprecated")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Получены данные о геолокации", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Доступ к геолокации запрещен", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onLocationChanged(location: Location) {
        latitude = location.latitude
        longitude = location.longitude
    }
}