package io.horizontalsystems.bankwallet.core.managers

import android.os.SystemClock
import io.horizontalsystems.bankwallet.core.IUptimeProvider

class UptimeProvider: IUptimeProvider {

    override val uptime: Long
        get() = SystemClock.elapsedRealtime()

}
