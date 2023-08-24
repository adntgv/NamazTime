package com.example.namaztime.complication

import android.app.PendingIntent
import android.content.Intent
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.example.namaztime.PrayerTimeManager.PrayerTimeManager
import com.example.namaztime.presentation.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

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
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "6!").build(),
            contentDescription = PlainComplicationText.Builder(text = "Short Text version of Number.").build()
        )
            .setTapAction(null)
            .build()
    }


    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val complicationPendingIntent = Intent(applicationContext, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val prayerTimes =
            withContext(Dispatchers.IO) {
                prayerTimeManager.getPrayerTimes()
            }
        val currentTime = Calendar.getInstance()

        var nextPrayerTime = prayerTimes.last()
        var currentPrayerTime = prayerTimes.first()

        for (prayerTime in prayerTimes) {
            if (currentTime.timeInMillis < prayerTime.dateTime.timeInMillis) {
                nextPrayerTime = prayerTime
                break
            }
            currentPrayerTime = prayerTime
        }

        val remainingSeconds = prayerTimeManager.calculateTimeRemaining(currentTime, nextPrayerTime)
        var minutes = remainingSeconds / 60
        val hours = minutes / 60
        minutes %= 60

        val hoursString = if (hours < 10) "0$hours" else hours.toString()
        val minutesString = if (minutes < 10) "0$minutes" else minutes.toString()

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                val complicationText = "${nextPrayerTime.name} in $hoursString:$minutesString"
                // Create complication data.
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text = complicationText).build(),
                    contentDescription = PlainComplicationText.Builder(text = "Short Text version of Number.").build()
                )
                    .setTapAction(complicationPendingIntent)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                val complicationText = "${nextPrayerTime.name} in $hours h and $minutes minutes"
                // Create complication data.
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text = complicationText).build(),
                    contentDescription = PlainComplicationText.Builder(text = "Long Text version of Number.").build()
                )
                    .setTapAction(complicationPendingIntent)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                var complicationText = if (hours > 0) {
                    "$hours h"
                } else if (minutes > 0) {
                    "$minutes m"
                } else {
                    "$remainingSeconds s"
                }

                val max = prayerTimeManager.calculateTimeRemaining(currentPrayerTime.dateTime, nextPrayerTime)
                RangedValueComplicationData.Builder(
                    value = remainingSeconds.toFloat(),
                    min = 0f,
                    max = max.toFloat(),
                    contentDescription = PlainComplicationText.Builder(text = "Ranged Value version of Number.").build()
                )
                    .setText(PlainComplicationText.Builder(text = complicationText).build())
                    .setTapAction(complicationPendingIntent)
                    .build()
            }
            else -> {
                // This will only happen if you have a watch face that contains a complication that
                // is not supported by this data source. We include it as a precaution, but this
                // should never be reached.
                throw IllegalArgumentException("Complication type ${request.complicationType} not supported by this data source")
            }
        }
    }
}