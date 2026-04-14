package com.quantum.wallet.bankwallet.modules.amount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.onFirstWith
import com.quantum.wallet.bankwallet.core.ILocalStorage
import com.quantum.wallet.bankwallet.modules.xrate.XRateService

class AmountInputModeViewModel(
    private val localStorage: ILocalStorage,
    private val xRateService: XRateService,
    private val coinUid: String
) : ViewModel() {

    private var hasValidRate: Boolean = xRateService.getCoinPrice(coinUid)?.expired == false

    var inputType by mutableStateOf(
        when {
            hasValidRate -> localStorage.amountInputType ?: AmountInputType.COIN
            else -> AmountInputType.COIN
        }
    )
        private set

    init {
        xRateService.getCoinPriceFlow(coinUid)
            .onFirstWith(viewModelScope) { coinPrice ->
                hasValidRate = coinPrice.expired == false
            }
    }

    fun onToggleInputType() {
        if (!hasValidRate) return

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

