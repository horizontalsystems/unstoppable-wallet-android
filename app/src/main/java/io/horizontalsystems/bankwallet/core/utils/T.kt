package io.horizontalsystems.bankwallet.core.utils

import android.util.Log
import okhttp3.internal.format

object T {
    private var timeStart = System.currentTimeMillis()
    private var lastMarkedTime: Long? = null

    fun t(reset: Boolean = false) {
        val currentTime = System.currentTimeMillis()

        if (reset) timeStart = currentTime

        val elapsedTimeFromStart = currentTime - timeStart
        val elapsedTimeFromLast = lastMarkedTime?.let { currentTime - it } ?: 0

        lastMarkedTime = currentTime

        val traceElement = Thread.currentThread().stackTrace[4]

        Log.e("Displayed", format("elapsed: %4d, all: %5d, %s", elapsedTimeFromLast, elapsedTimeFromStart, traceElement))
    }
}
