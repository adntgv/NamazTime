/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.adntgv.namaztime.presentation

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.adntgv.namaztime.presentation.theme.NamazTimeTheme
import com.adntgv.prayertimemanager.PrayerTimeManager.City
import com.adntgv.prayertimemanager.PrayerTimeManager.PrayerTime
import com.adntgv.prayertimemanager.PrayerTimeManager.PrayerTimeManager
import com.adntgv.namaztime.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar

class MainActivity : ComponentActivity(), LocationListener {
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private var city = mutableStateOf(City(0, "", 0.0, 0.0))
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    private lateinit var prayerTimeManager: PrayerTimeManager
    private var nextPrayerTime = mutableStateOf(PrayerTime(0, "", "", Calendar.getInstance()))
    private var formattedTime = mutableStateOf("")
    private var cities: List<City> = listOf(
        City(0, "", 0.0, 0.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        prayerTimeManager = PrayerTimeManager(applicationContext)

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                update()
            }
        }

        getLocation()
        setContent {
            WearApp(this.city.value.name)
        }
    }

    @Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() {
        WearApp("Астана")
    }

    @Composable
    fun WearApp(cityName: String) {
        NamazTimeTheme {
            /* If you have enough items in your list, use [ScalingLazyColumn] which is an optimized
             * version of LazyColumn for wear devices with some added features. For more information,
             * see d.android.com/wear/compose.
             */
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colors.background)
                    .verticalScroll(rememberScrollState())
                ,
                verticalArrangement = Arrangement.Center,

            ) {
                Spacer(modifier = Modifier.height(16.dp))
                ShowCityName(cityName = cityName)
                Spacer(modifier = Modifier.height(16.dp))
                PrayerTimeView()
                Spacer(modifier = Modifier.height(16.dp))
                Update()
                Spacer(modifier = Modifier.height(16.dp))
                CitySelector(cities)
            }
        }
    }

    private @Composable
    fun CitySelector(cities: List<City>) {
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


    private @Composable
    fun CitySelectorButton(cityName: String) {
        val coroutineScope = rememberCoroutineScope()

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        city.value = prayerTimeManager.selectCity(cityName)
                        updatePrayerTime(cityName = cityName)
                    }
                }
            },
        ) {
            Text(text = cityName)
        }
    }

    @Composable
    fun ShowCityName(cityName: String) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            text = stringResource(R.string.cityName, cityName)
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
            Text(text = stringResource(R.string.update))
        }
    }

    private suspend fun update() {
        cities = prayerTimeManager.getAllCities()

        city.value = prayerTimeManager.updateCurrentCity(latitude, longitude)

        updatePrayerTime(cityName = city.value.name)
    }

    private suspend fun updatePrayerTime(cityName: String = city.value.name) {
        nextPrayerTime.value = prayerTimeManager.getNextPrayerTime(cityName).first

        val formatter = SimpleDateFormat("HH:mm")
        formattedTime.value = formatter.format(nextPrayerTime.value.dateTime.time)
    }

    @Composable
    private fun PrayerTimeView() {
        rememberCoroutineScope()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            Text(text = nextPrayerTime.value.name, textAlign = TextAlign.Center)
            Text(text = formattedTime.value, textAlign = TextAlign.Center)
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