package io.horizontalsystems.core.modules.rateapp

import androidx.lifecycle.ViewModel
import io.horizontalsystems.core.core.IRateAppManager

class RateAppViewModel(private val rateAppManager: IRateAppManager) : ViewModel() {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

}
