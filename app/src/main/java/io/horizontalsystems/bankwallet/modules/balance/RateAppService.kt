package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.IRateAppManager

class RateAppService(private val rateAppManager: IRateAppManager) : Clearable {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

    override fun clear() = Unit
}
