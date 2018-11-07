package io.horizontalsystems.bankwallet.viewHelpers

import android.content.Context
import android.text.format.DateFormat
import io.horizontalsystems.bankwallet.R
import java.text.SimpleDateFormat
import java.util.*



object DateHelper {

    fun getOnlyTime(date: Date): String = formatDate(date, "HH:mm")
    fun getFullDateWithShortMonth(date: Date): String = formatDate(date, "MMM d, yyyy, HH:mm")

    fun getRelativeDateString(context: Context, date: Date): String {
        val secondsAgo = getSecondsAgo(date).toInt()
        val hoursAgo = secondsAgo / (60 * 60)
        val minutesAgo = secondsAgo / 60

        return when {
            secondsAgo < 60 -> context.getString(R.string.timestamp_just_now)
            secondsAgo < 60 * 60 -> context.getString(R.string.timestamp_min_ago, minutesAgo)
            hoursAgo < 12 -> context.getString(R.string.timestamp_hours_ago, hoursAgo)
            isToday(date) -> context.getString(R.string.timestamp_today)
            isYesterday(date) -> context.getString(R.string.timestamp_yesterday)
            isThisYear(date) -> getFormattedDateWithoutYear(date)
            else -> getFormattedDateWithYear(date)
        }
    }

    fun getShortDateForTransaction(date: Date): String = if (isThisYear(date)) {
        formatDate(date, "MMM d")
    } else {
        formatDate(date, "MM/dd/yyyy")
    }

    fun getCalendarFromTimestamp(timestamp: Long) : Calendar {
        val timeZone = TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(timeZone)
        cal.timeInMillis = timestamp
        return cal
    }

    fun minutesAfterNow(minutes: Int): Long {
        var now = Date()
        val cal = Calendar.getInstance()
        cal.time = now
        cal.add(Calendar.MINUTE, minutes)
        now = cal.time
        return now.time
    }

    private fun formatDate(date: Date, outputFormat: String) =
            SimpleDateFormat(DateFormat.getBestDateTimePattern(Locale.getDefault(), outputFormat)).format(date)

    private fun getFormattedDateWithYear(date: Date) = formatDate(date, "MMMM d, yyyy")

    private fun getFormattedDateWithoutYear(date: Date) = formatDate(date, "MMMM d")

    private fun getSecondsAgo(date: Date): Long {
        val differenceInMillis = Date().time - date.time
        return differenceInMillis / 1000
    }

    fun getSecondsAgo(dateInMillis: Long): Long {
        val differenceInMillis = Date().time - dateInMillis
        return differenceInMillis / 1000
    }

    private fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        return isSameDay(date, today)
    }

    private fun isYesterday(date: Date): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(date, yesterday)
    }

    private fun isSameDay(date: Date, calendar: Calendar): Boolean {
        val calendar2 = Calendar.getInstance()
        calendar2.time = date

        return calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR) && calendar.get(Calendar.DAY_OF_YEAR) == calendar2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isThisYear(date: Date): Boolean {
        val calendar = Calendar.getInstance()
        val calendar2 = Calendar.getInstance()
        calendar2.time = date

        return calendar.get(Calendar.YEAR) == calendar2.get(Calendar.YEAR)
    }

}