package io.horizontalsystems.bankwallet.modules.amount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cash.z.ecc.android.sdk.ext.onFirstWith
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.modules.xrate.XRateService

@HiltViewModel(assistedFactory = AmountInputModeViewModel.Factory::class)
class AmountInputModeViewModel @AssistedInject constructor(
    @Assisted private val coinUid: String,
    private val localStorage: ILocalStorage,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(coinUid: String): AmountInputModeViewModel
    }

    private val xRateService = XRateService(marketKit, currencyManager.baseCurrency)

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

