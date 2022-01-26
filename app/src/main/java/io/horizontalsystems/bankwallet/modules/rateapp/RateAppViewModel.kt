package io.horizontalsystems.bankwallet.modules.rateapp

import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.IRateAppManager

class RateAppViewModel(private val rateAppManager: IRateAppManager) : ViewModel() {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

}
