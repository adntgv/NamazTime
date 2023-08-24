package com.example.namaztime.complication

import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.namaztime.PrayerTimeManager.PrayerTimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DateFormat.getDateTimeInstance
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {
    private lateinit var prayerTimeManager: PrayerTimeManager

    override fun onCreate() {
        super.onCreate()
        prayerTimeManager = PrayerTimeManager(applicationContext)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("Mon", "Monday")
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prayerTime =
            withContext(Dispatchers.IO) {
                prayerTimeManager.getClosestPrayerTime()
            }

        if (prayerTime == null) {
            return createComplicationData("empty", "Prayer")
        }

        val time = Date(prayerTime.prayerTime)
        val formatter = SimpleDateFormat("HH:mm")

        return createComplicationData(formatter.format(time), prayerTime.prayerName)
    }

    private fun createComplicationData(text: String, contentDescription: String) =
        ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(contentDescription).build()
        ).build()
}