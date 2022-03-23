package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal

class AvailableBalanceViewModel(
    private val coin: Coin,
    private val coinDecimal: Int,
    private val fiatDecimal: Int,
    private val currencyManager: ICurrencyManager,
    private val marketKit: MarketKit
) : ViewModel() {

    var amountInputMode: AmountInputModule.InputMode? = null
    var availableBalance: BigDecimal? = null

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
        val tmpAvailableBalance = availableBalance ?: return
        val tmpAmountInputMode = amountInputMode ?: return

        formatted = when (tmpAmountInputMode) {
            AmountInputModule.InputMode.Coin -> {
                App.numberFormatter.formatCoin(tmpAvailableBalance, coin.code, 0, coinDecimal)

            }
            AmountInputModule.InputMode.Currency -> {
                val currencyAmount = rate?.let {
                    tmpAvailableBalance.times(it.value)
                } ?: BigDecimal.ZERO

                App.numberFormatter.format(currencyAmount, fiatDecimal, fiatDecimal, prefix = currency.symbol)
            }
        }
    }
}

object AvailableBalanceModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coin: Coin,
        private val coinDecimal: Int,
        private val fiatDecimal: Int,
    ) : ViewModelProvider.Factory {

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AvailableBalanceViewModel(
                coin,
                coinDecimal,
                fiatDecimal,
                App.currencyManager,
                App.marketKit
            ) as T
        }
    }
}
