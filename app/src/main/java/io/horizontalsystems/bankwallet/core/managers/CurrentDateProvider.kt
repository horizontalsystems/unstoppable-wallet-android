package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ICurrentDateProvider
import java.util.*

class CurrentDateProvider: ICurrentDateProvider {

    override val currentDate: Date
        get() = Date()

}
