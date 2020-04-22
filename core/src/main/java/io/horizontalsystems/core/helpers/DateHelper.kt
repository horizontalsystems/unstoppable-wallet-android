package io.horizontalsystems.core.helpers

import android.content.Context
import android.text.format.DateFormat
import io.horizontalsystems.core.CoreApp
import io.horizontalsystems.core.R
import java.text.SimpleDateFormat
import java.util.*

object DateHelper {

    private val timeFormat: String by lazy {
        val is24HourFormat = DateFormat.is24HourFormat(CoreApp.instance)
        if (is24HourFormat) "HH:mm" else "h:mm a"
    }

    fun getDayAndTime(date: Date): String = formatDate(date, "MMM d, $timeFormat")
    fun getOnlyTime(date: Date): String = formatDate(date, timeFormat)
    fun getFullDate(date: Date): String = formatDate(date, "MMM d, yyyy, $timeFormat")

    fun getTxDurationString(context: Context, durationInSec: Long): String {
        return when {
            durationInSec < 10 -> context.getString(R.string.Duration_instant)
            durationInSec < 60 -> context.getString(R.string.Duration_Seconds, durationInSec)
            durationInSec < 60 * 60 -> {
                val minutes = durationInSec / 60
                context.getString(R.string.Duration_Minutes, minutes)
            }
            else -> {
                val hours = durationInSec / (60 * 60)
                context.getString(R.string.Duration_Hours, hours)
            }
        }
    }

    fun getTxDurationIntervalString(context: Context, durationInSec: Long): String {
        return when {
            durationInSec < 10 -> context.getString(R.string.Duration_instant)
            durationInSec < 60 -> {
                val seconds = context.getString(R.string.Duration_Seconds, durationInSec)
                context.getString(R.string.Duration_Within, seconds)
            }
            durationInSec < 60 * 60 -> {
                val minutes = context.getString(R.string.Duration_Minutes, durationInSec / 60)
                context.getString(R.string.Duration_Within, minutes)
            }
            else -> {
                val hours = context.getString(R.string.Duration_Hours, durationInSec / (60 * 60))
                context.getString(R.string.Duration_Within, hours)
            }
        }
    }

    fun shortDate(date: Date, near: String = "MMM d", far: String = "MMM dd, yyyy"): String = if (isThisYear(date)) {
        formatDate(date, near)
    } else {
        formatDate(date, far)
    }

    fun formatDate(date: Date, pattern: String): String {
        return SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), pattern),
                                Locale.getDefault()).format(date)
    }

    fun getSecondsAgo(dateInMillis: Long): Long {
        val differenceInMillis = Date().time - dateInMillis
        return differenceInMillis / 1000
    }

    fun isSameDay(date1: Date, date2: Date): Boolean {
        val calendar1 = Calendar.getInstance().apply { time = date1 }
        val calendar2 = Calendar.getInstance().apply { time = date2 }

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) && calendar1.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisYear(date: Date): Boolean {
        val calendar1 = Calendar.getInstance()
        val calendar2 = Calendar.getInstance().apply { time = date }

        return calendar1.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
    }
}
