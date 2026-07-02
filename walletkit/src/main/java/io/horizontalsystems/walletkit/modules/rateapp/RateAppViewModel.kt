package io.horizontalsystems.walletkit.modules.rateapp

import androidx.lifecycle.ViewModel
import io.horizontalsystems.walletkit.core.IRateAppManager

class RateAppViewModel(private val rateAppManager: IRateAppManager) : ViewModel() {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

}
