package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.IRateAppManager

class RateAppService(private val rateAppManager: IRateAppManager) {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

}
