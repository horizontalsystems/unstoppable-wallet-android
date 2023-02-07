package cash.p.terminal.modules.rateapp

import androidx.lifecycle.ViewModel
import cash.p.terminal.core.IRateAppManager

class RateAppViewModel(private val rateAppManager: IRateAppManager) : ViewModel() {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

}
