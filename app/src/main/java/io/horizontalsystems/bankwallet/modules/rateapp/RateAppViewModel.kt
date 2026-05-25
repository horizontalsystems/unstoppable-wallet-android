package io.horizontalsystems.bankwallet.modules.rateapp

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.IRateAppManager
import javax.inject.Inject

@HiltViewModel
class RateAppViewModel @Inject constructor(private val rateAppManager: IRateAppManager) : ViewModel() {

    fun onBalancePageActive() {
        rateAppManager.onBalancePageActive()
    }

    fun onBalancePageInactive() {
        rateAppManager.onBalancePageInactive()
    }

}
