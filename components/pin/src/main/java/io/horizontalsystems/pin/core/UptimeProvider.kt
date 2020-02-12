package io.horizontalsystems.pin.core

import android.os.SystemClock

class UptimeProvider {

    val uptime: Long
        get() = SystemClock.elapsedRealtime()

}
