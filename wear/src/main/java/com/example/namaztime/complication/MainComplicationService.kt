package com.example.namaztime.complication

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.namaztime.PrayerTimeManager.PrayerTimeManager
import com.example.namaztime.presentation.MainActivity
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
        return createComplicationData("20:30", "Monday", type)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val prayerTime =
            withContext(Dispatchers.IO) {
                prayerTimeManager.getClosestPrayerTime()
            }

        if (prayerTime == null) {
            return createComplicationData("empty", "Prayer", request.complicationType)
        }

        val time = Date(prayerTime.prayerTime)
        val formatter = SimpleDateFormat("HH:mm")

        return createComplicationData(formatter.format(time), prayerTime.prayerName, request.complicationType)
    }

    private fun createComplicationData(text: String, contentDescription: String, type: ComplicationType): ComplicationData {

        val complicationPendingIntent = Intent(applicationContext, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        return when (type) {
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text).build(),
                    contentDescription = PlainComplicationText.Builder(contentDescription).build()
                )
                    .setTapAction(complicationPendingIntent)

                    .build()
           ComplicationType.LONG_TEXT ->
               LongTextComplicationData.Builder(
                   text = PlainComplicationText.Builder(text).build(),
                   contentDescription = PlainComplicationText.Builder(contentDescription).build()
               )
                   .setTapAction(complicationPendingIntent)
                   .build()
            ComplicationType.RANGED_VALUE ->
                RangedValueComplicationData.Builder(
                    value = 0f,
                    min = 0f,
                    max = 100f,
                    contentDescription = PlainComplicationText.Builder(contentDescription).build()
                )
                    .setTapAction(complicationPendingIntent)
                    .build()

            else -> EmptyComplicationData()
        }
    }
}