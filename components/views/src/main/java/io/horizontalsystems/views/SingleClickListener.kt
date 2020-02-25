package io.horizontalsystems.views

import android.os.SystemClock
import android.view.View

abstract class SingleClickListener : View.OnClickListener {

    override fun onClick(v: View) {
        if (canBeClicked()) {
            onSingleClick(v)
        }
    }

    abstract fun onSingleClick(v: View)

    companion object {

        private const val CLICK_TIME_DELTA: Long = 500 //milliseconds
        private var lastClickTime: Long = 0

        fun canBeClicked(): Boolean {
            val clickTime = SystemClock.elapsedRealtime()

            if (clickTime - lastClickTime > CLICK_TIME_DELTA) {
                lastClickTime = clickTime
                return true
            }
            return false
        }
    }
}
