package io.horizontalsystems.bankwallet.modules.amount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.onFirstWith
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.xrate.XRateService

class AmountInputModeViewModel(
    private val localStorage: ILocalStorage,
    private val xRateService: XRateService,
    private val coinUid: String
) : ViewModel() {

    private var hasXRate = xRateService.getRate(coinUid) != null

    var inputType by mutableStateOf(
        when {
            hasXRate -> localStorage.amountInputType ?: AmountInputType.COIN
            else -> AmountInputType.COIN
        }
    )
        private set

    init {
        xRateService.getRateFlow(coinUid)
            .onFirstWith(viewModelScope) {
                hasXRate = true
            }
    }

    fun onToggleInputType() {
        if (!hasXRate) return

        inputType = inputType.reversed()
        localStorage.amountInputType = inputType
    }

    fun onCoinInput() {
        inputType = AmountInputType.COIN
        localStorage.amountInputType = inputType
    }

    fun onFiatInput() {
        inputType = AmountInputType.CURRENCY
        localStorage.amountInputType = inputType
    }
}

