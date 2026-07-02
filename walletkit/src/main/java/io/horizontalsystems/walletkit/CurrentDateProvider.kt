package io.horizontalsystems.walletkit

import java.util.*

class CurrentDateProvider : ICurrentDateProvider {

    override val currentDate: Date
        get() = Date()

}
