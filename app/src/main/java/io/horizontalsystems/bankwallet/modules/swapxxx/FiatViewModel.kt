package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.marketkit.models.Coin
import io.horizontalsystems.marketkit.models.CoinPrice
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import java.math.BigDecimal

class FiatViewModel(
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val numberFormatter: IAppNumberFormatter,
) : ViewModel() {
    private var coin: Coin? = null
    private var amount: BigDecimal? = null
    private var coinPrice: CoinPrice? = null
    private var fiatAmount: BigDecimal? = null

    var fiatAmountString: String? by mutableStateOf(null)
        private set

    val currencyAmountHint: String
        get() = "${currencyManager.baseCurrency.symbol}0"

    init {
        viewModelScope.launch {
            currencyManager.baseCurrencyUpdatedSignal.asFlow().collect {
                refreshCoinPrice()
                refreshFiatAmount()

                emitState()
            }
        }
    }

    fun setCoin(coin: Coin?) {
        this.coin = coin

        refreshCoinPrice()
        refreshFiatAmount()

        emitState()
    }

    fun setAmount(amount: BigDecimal?) {
        this.amount = amount

        refreshFiatAmount()

        emitState()
    }

    private fun emitState() {
        val baseCurrency = currencyManager.baseCurrency
        fiatAmountString = fiatAmount?.let { numberFormatter.formatFiatShort(it, baseCurrency.symbol, baseCurrency.decimal) }
    }

    private fun refreshCoinPrice() {
        coinPrice = coin?.let {
            marketKit.coinPrice(it.uid, currencyManager.baseCurrency.code)
        }
    }

    private fun refreshFiatAmount() {
        fiatAmount = amount?.let { amount ->
            coinPrice?.let { coinPrice ->
                amount.multiply(coinPrice.value)
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FiatViewModel(App.marketKit, App.currencyManager, App.numberFormatter) as T
        }
    }
}
