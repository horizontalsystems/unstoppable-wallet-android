package io.horizontalsystems.pin.core

import android.os.SystemClock

class UptimeProvider {

    //elapsed time since phone boot
    val uptime: Long
        get() = SystemClock.elapsedRealtime()

}
