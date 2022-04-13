package io.horizontalsystems.bankwallet.modules.xrate

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.CoinPrice
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.launch

class XRateViewModel(
    private val coinUid: String,
    private val marketKit: MarketKit,
    private val currency: Currency
) : ViewModel() {

    var rate by mutableStateOf(marketKit.coinPrice(coinUid, currency.code)?.toCurrencyValue())

    private val disposables = CompositeDisposable()

    init {
        marketKit.coinPriceObservable(coinUid, currency.code)
            .subscribeIO {
                viewModelScope.launch {
                    rate = it.toCurrencyValue()
                }
            }
            .let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
    }

    private fun CoinPrice.toCurrencyValue() = CurrencyValue(currency, this.value)

}