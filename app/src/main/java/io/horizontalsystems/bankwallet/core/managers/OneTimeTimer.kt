package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.OneTimerDelegate
import java.util.*

class OneTimeTimer {

    var delegate: OneTimerDelegate? = null
    private var timer: Timer? = null

    fun schedule(time: Date) {

        if(timer == null) {
            timer = Timer()
        }

        timer?.schedule(object : TimerTask() {
            override fun run() {
                delegate?.onFire()
            }
        }, time)

    }
}
