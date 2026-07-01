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

