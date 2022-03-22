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
    private val coinDecimal: Int,
    private val currencyDecimal: Int
) : ViewModel() {

    var inputMode by mutableStateOf(AmountInputModule.InputMode.Coin)
        private set

    var inputPrefix by mutableStateOf<String?>(null)
        private set

    var hint by mutableStateOf("")
        private set

    var coinAmount: BigDecimal? = null
        private set

    private var rate = marketKit.coinPrice(coin.uid, currencyManager.baseCurrency.code)
    private var currencyAmount: BigDecimal? = null
    private var disposables = CompositeDisposable()

    init {
        marketKit.coinPriceObservable(coin.uid, currencyManager.baseCurrency.code)
            .subscribeIO {
                rate = it
                refreshHint()
            }
            .let {
                disposables.add(it)
            }

        refreshHint()
        updateInputPrefix()
    }

    override fun onCleared() {
        disposables.clear()
    }

    fun onEnterAmount(text: String) {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull()?.stripTrailingZeros() else null

        when (inputMode) {
            AmountInputModule.InputMode.Coin -> updateCoinAmount(amount)
            AmountInputModule.InputMode.Currency -> updateCurrencyAmount(amount)
        }

        refreshHint()
    }

    fun onEnterCoinAmount(amount: BigDecimal) {
        updateCoinAmount(amount)
        refreshHint()
    }

    private fun updateCurrencyAmount(amount: BigDecimal?) {
        coinAmount = rate?.let { rate ->
            amount?.divide(rate.value, coinDecimal, RoundingMode.CEILING)?.stripTrailingZeros()
        }
        currencyAmount = amount
    }

    private fun updateCoinAmount(amount: BigDecimal?) {
        coinAmount = amount
        currencyAmount = rate?.let { rate ->
            amount?.times(rate.value)?.setScale(currencyDecimal, RoundingMode.CEILING)
                ?.stripTrailingZeros()
        }
    }

    fun getEnterAmount(): String {
        val amount = when (inputMode) {
            AmountInputModule.InputMode.Coin -> coinAmount
            AmountInputModule.InputMode.Currency -> currencyAmount
        }
        return amount?.toPlainString() ?: ""

    }

    private fun refreshHint() {
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

        refreshHint()
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
