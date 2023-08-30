package com.adntgv.namaztime.complication

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
import com.adntgv.namaztime.presentation.MainActivity
import com.adntgv.prayertimemanager.PrayerTimeManager.PrayerTimeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Skeleton for complication data source that returns short text.
 */
class MainComplicationService : SuspendingComplicationDataSourceService() {
    private lateinit var prayerTimeManager: PrayerTimeManager

    override fun onCreate() {
        super.onCreate()
        prayerTimeManager = PrayerTimeManager(applicationContext)
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData {
        return ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text = "Asr через 01:32").build(),
            contentDescription = PlainComplicationText.Builder(text = "Время до намаза.").build()
        )
            .setTapAction(null)
            .build()
    }


    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val complicationPendingIntent = Intent(applicationContext, MainActivity::class.java).let { intent ->
            PendingIntent.getActivity(applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        }

        val cityName = withContext(Dispatchers.IO) { prayerTimeManager.getCurrentCity().name}
        val triple = withContext(Dispatchers.IO) { prayerTimeManager.getNextPrayerTime(cityName) }

        val nextPrayerTime = triple.first
        val max = triple.second
        val remainingSeconds = triple.third

        var minutes = remainingSeconds / 60
        val hours = minutes / 60
        minutes %= 60

        var complicationText = if (hours > 0) {
            "$hours ч"
        } else if (minutes > 0) {
            "$minutes м"
        } else {
            "$remainingSeconds с"
        }

        return when (request.complicationType) {
            ComplicationType.SHORT_TEXT -> {
                // Create complication data.
                ShortTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text = complicationText).build(),
                    contentDescription = PlainComplicationText.Builder(text = "Время до намаза").build(),

                )
                    .setTapAction(complicationPendingIntent)
                    .build()
            }
            ComplicationType.LONG_TEXT -> {
                complicationText = "${nextPrayerTime.name} через $hours ч и $minutes м"
                // Create complication data.
                LongTextComplicationData.Builder(
                    text = PlainComplicationText.Builder(text = complicationText).build(),
                    contentDescription = PlainComplicationText.Builder(text = "Время до намаза").build()
                )
                    .setTapAction(complicationPendingIntent)
                    .build()
            }
            ComplicationType.RANGED_VALUE -> {
                RangedValueComplicationData.Builder(
                    value = remainingSeconds.toFloat(),
                    min = 0f,
                    max = max.toFloat(),
                    contentDescription = PlainComplicationText.Builder(text = "Время до намаза").build()
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