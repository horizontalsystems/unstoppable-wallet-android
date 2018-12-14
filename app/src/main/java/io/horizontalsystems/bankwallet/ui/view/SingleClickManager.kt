package io.horizontalsystems.bankwallet.ui.view

import android.os.SystemClock

object SingleClickManager {

    private const val CLICK_TIME_DELTA: Long = 500 //milliseconds
    private var lastClickTime: Long = 0

    fun canBeClicked(): Boolean {
        val clickTime = SystemClock.elapsedRealtime()

        if (clickTime - lastClickTime > CLICK_TIME_DELTA) {
            lastClickTime  = clickTime
            return true
        }
        return false
    }

}
