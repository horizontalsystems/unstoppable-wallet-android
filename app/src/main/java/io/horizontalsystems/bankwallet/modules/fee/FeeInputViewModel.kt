package io.horizontalsystems.bankwallet.modules.fee

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.send.SendModule
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class FeeInputViewModel(
    private val coin: Coin,
    private val coinDecimal: Int,
    private val fiatDecimal: Int,
    private val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit
) : ViewModel() {

    var amountInputType: SendModule.InputType? = null
    var fee = BigDecimal.ZERO

    private val currency = currencyManager.baseCurrency
    private var rate = marketKit.coinPrice(coin.uid, currency.code)
    private var disposables = CompositeDisposable()

    var formatted by mutableStateOf<String?>(null)
        private set

    init {
        marketKit.coinPriceObservable(coin.uid, currency.code)
            .subscribeIO {
                rate = it
                refreshFormatted()
            }
            .let {
                disposables.add(it)
            }
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun refreshFormatted() {
        val tmpAmountInputType = amountInputType ?: return

        val values = mutableListOf(
            App.numberFormatter.formatCoin(fee, coin.code, 0, coinDecimal)
        )

        rate?.let {
            val currencyStr = App.numberFormatter.format(fee.times(it.value), fiatDecimal, fiatDecimal, prefix = currency.symbol)
            when (tmpAmountInputType) {
                SendModule.InputType.COIN -> values.add(currencyStr)
                SendModule.InputType.CURRENCY -> values.add(0, currencyStr)
            }
        }

        formatted = values.joinToString(" | ")
    }

}