package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
import io.horizontalsystems.bankwallet.core.IPeriodicTimerDelegate
import java.util.*

class PeriodicTimer(private val delay: Long) {

    var delegate: IPeriodicTimerDelegate? = null
    private var timer: Timer? = null

    fun schedule() {
        try {
            timer?.cancel()
        } catch (e: Exception) {
            Log.e("PeriodicTimer", "exception", e)
        }

        timer = Timer()

        timer?.schedule(object : TimerTask() {
            override fun run() {
                delegate?.onFire()
            }
        }, delay)

    }
}
