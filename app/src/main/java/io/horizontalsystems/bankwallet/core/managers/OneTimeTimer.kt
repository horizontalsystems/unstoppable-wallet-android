package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.IPeriodicTimerDelegate
import java.util.*

class OneTimeTimer {

    var delegate: IPeriodicTimerDelegate? = null
    private var timer: Timer? = null

    fun schedule(time: Date) {
        try {
            timer?.cancel()
        } catch (e: Exception) {
            Log.e("OneTimeTimer", "exception", e)
        }

        timer = Timer()

        timer?.schedule(object : TimerTask() {
            override fun run() {
                delegate?.onFire()
            }
        }, time)

    }
}
