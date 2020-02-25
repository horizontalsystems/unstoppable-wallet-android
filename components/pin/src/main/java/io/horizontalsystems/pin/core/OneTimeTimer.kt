package io.horizontalsystems.pin.core

import java.util.*

class OneTimeTimer {

    var delegate: OneTimerDelegate? = null
    private var timer: Timer? = null

    fun schedule(time: Date) {

        if (timer == null) {
            timer = Timer()
        }

        timer?.schedule(object : TimerTask() {
            override fun run() {
                delegate?.onFire()
            }
        }, time)

    }
}
