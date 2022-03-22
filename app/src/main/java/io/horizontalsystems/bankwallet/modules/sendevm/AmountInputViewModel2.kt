package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.min

class AmountInputViewModel2(
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager,
    private val coin: Coin,
    val coinDecimal: Int,
    val currencyDecimal: Int
) : ViewModel() {

    var inputMode by mutableStateOf(AmountInputModule.InputMode.Coin)
        private set

    var inputPrefix by mutableStateOf<String?>(null)
        private set

    var hint by mutableStateOf("")
        private set

    private var disposables = CompositeDisposable()
    private var rate = marketKit.coinPrice(coin.uid, currencyManager.baseCurrency.code)
    var coinAmount: BigDecimal? = null
    private var currencyAmount: BigDecimal? = null

    init {
        marketKit.coinPriceObservable(coin.uid, currencyManager.baseCurrency.code)
            .subscribeIO {
                rate = it
                updateHint()
            }
            .let {
                disposables.add(it)
            }

        updateHint()
        updateInputPrefix()
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onEnterAmount(text: String) {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull()?.stripTrailingZeros() else null

        when (inputMode) {
            AmountInputModule.InputMode.Coin -> {
                coinAmount = amount
                currencyAmount = rate?.let { rate ->
                    amount?.times(rate.value)?.setScale(currencyDecimal, RoundingMode.CEILING)?.stripTrailingZeros()
                }
            }
            AmountInputModule.InputMode.Currency -> {
                coinAmount = rate?.let { rate ->
                    amount?.divide(rate.value, coinDecimal, RoundingMode.CEILING)?.stripTrailingZeros()
                }
                currencyAmount = amount
            }
        }

        updateHint()
    }

    fun getEnterAmount(): String {
        val amount = when (inputMode) {
            AmountInputModule.InputMode.Coin -> coinAmount
            AmountInputModule.InputMode.Currency -> currencyAmount
        }
        return amount?.toPlainString() ?: ""

    }

    private fun updateHint() {
        hint = if (rate == null) {
            ""
        } else {
            when (inputMode) {
                AmountInputModule.InputMode.Coin -> {
                    App.numberFormatter.format(currencyAmount ?: BigDecimal.ZERO, currencyDecimal, currencyDecimal, prefix = currencyManager.baseCurrency.symbol)
                }
                AmountInputModule.InputMode.Currency -> {
                    App.numberFormatter.formatCoin(coinAmount ?: BigDecimal.ZERO, coin.code, 0, coinDecimal)
                }
            }
        }
    }

    fun onToggleInputMode() {
        inputMode = when (inputMode) {
            AmountInputModule.InputMode.Coin -> AmountInputModule.InputMode.Currency
            AmountInputModule.InputMode.Currency -> AmountInputModule.InputMode.Coin
        }

        updateHint()
        updateInputPrefix()
    }

    private fun updateInputPrefix() {
        inputPrefix = when (inputMode) {
            AmountInputModule.InputMode.Coin -> null
            AmountInputModule.InputMode.Currency -> currencyManager.baseCurrency.symbol
        }
    }

    fun isValid(text: String): Boolean {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull() else null
        if (amount == null) return true

        val maxAllowedScale = when (inputMode) {
            AmountInputModule.InputMode.Coin -> coinDecimal
            AmountInputModule.InputMode.Currency -> currencyDecimal
        }

        return amount.scale() <= maxAllowedScale
    }

}

object AmountInputModule {
    enum class InputMode {
        Coin, Currency;
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(private val wallet: Wallet) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val coinDecimal = min(wallet.decimal, App.appConfigProvider.maxDecimal)
            val currencyDecimal = App.appConfigProvider.fiatDecimal
            return AmountInputViewModel2(
                App.marketKit,
                App.currencyManager,
                wallet.coin,
                coinDecimal,
                currencyDecimal
            ) as T
        }

    }
}
