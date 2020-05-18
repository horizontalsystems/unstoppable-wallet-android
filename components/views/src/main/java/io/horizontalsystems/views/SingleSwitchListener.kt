package io.horizontalsystems.views

import android.os.SystemClock
import android.widget.CompoundButton

abstract class SingleSwitchListener : CompoundButton.OnCheckedChangeListener {

    override fun onCheckedChanged(buttonView: CompoundButton?, isChecked: Boolean) {
        if (canBeChanged()) {
            onSingleSwitch(buttonView, isChecked)
        }
    }

    abstract fun onSingleSwitch(buttonView: CompoundButton?, isChecked: Boolean)

    companion object {

        private const val SWITCH_TIME_DELTA: Long = 500 //milliseconds
        private var lastSwitchTime: Long = 0

        fun canBeChanged(): Boolean {
            val switchTime = SystemClock.elapsedRealtime()

            if (switchTime - lastSwitchTime > SWITCH_TIME_DELTA) {
                lastSwitchTime = switchTime
                return true
            }
            return false
        }
    }
}
