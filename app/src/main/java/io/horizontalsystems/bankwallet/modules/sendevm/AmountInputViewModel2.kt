package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.swap.settings.Caution
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.marketkit.MarketKit
import io.horizontalsystems.marketkit.models.Coin
import io.reactivex.disposables.CompositeDisposable
import java.math.BigDecimal
import java.math.RoundingMode

class AmountInputViewModel2(
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager,
    private val coin: Coin,
    private val coinDecimal: Int,
    private val fiatDecimal: Int,
    private val amountValidator: AmountValidator?,
) : ViewModel() {

    interface AmountValidator {
        fun validateAmount(amount: BigDecimal?): Caution?
    }

    var availableBalance = BigDecimal.ZERO

    val isMaxEnabled: Boolean
        get() = availableBalance > BigDecimal.ZERO

    var inputMode by mutableStateOf(AmountInputModule.InputMode.Coin)
        private set

    var inputPrefix by mutableStateOf<String?>(null)
        private set

    var hint by mutableStateOf("")
        private set

    var caution by mutableStateOf<Caution?>(null)
        private set

    private var coinAmount: BigDecimal? = null

    private val currency = currencyManager.baseCurrency
    private var rate = marketKit.coinPrice(coin.uid, currency.code)
    private var currencyAmount: BigDecimal? = null
    private var disposables = CompositeDisposable()

    init {
        marketKit.coinPriceObservable(coin.uid, currency.code)
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

    fun getResultCoinAmount() = when {
        caution?.type != Caution.Type.Error -> coinAmount
        else -> null
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
        refreshCaution()
    }

    fun onClickMax() {
        updateCoinAmount(availableBalance)
        refreshHint()
        refreshCaution()
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
            amount?.times(rate.value)?.setScale(fiatDecimal, RoundingMode.DOWN)?.stripTrailingZeros()
        }
    }

    fun getEnterAmount(): String {
        val amount = when (inputMode) {
            AmountInputModule.InputMode.Coin -> coinAmount
            AmountInputModule.InputMode.Currency -> currencyAmount
        }
        return amount?.toPlainString() ?: ""
    }

    private fun refreshCaution() {
        val tmpCoinAmount = coinAmount

        caution = when {
            tmpCoinAmount == null -> null
            tmpCoinAmount > availableBalance -> Caution(
                Translator.getString(R.string.Swap_ErrorInsufficientBalance),
                Caution.Type.Error
            )
            else -> amountValidator?.validateAmount(tmpCoinAmount)
        }
    }

    private fun refreshHint() {
        hint = if (rate == null) {
            ""
        } else {
            when (inputMode) {
                AmountInputModule.InputMode.Coin -> {
                    App.numberFormatter.format(currencyAmount ?: BigDecimal.ZERO, fiatDecimal, fiatDecimal, prefix = currency.symbol)
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
            AmountInputModule.InputMode.Currency -> currency.symbol
        }
    }

    fun isValid(text: String): Boolean {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull() else null
        if (amount == null) return true

        val maxAllowedScale = when (inputMode) {
            AmountInputModule.InputMode.Coin -> coinDecimal
            AmountInputModule.InputMode.Currency -> fiatDecimal
        }

        return amount.scale() <= maxAllowedScale
    }

}

object AmountInputModule {
    enum class InputMode {
        Coin, Currency;
    }

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coin: Coin,
        private val coinDecimal: Int,
        private val fiatDecimal: Int,
        private val amountValidator: AmountInputViewModel2.AmountValidator?
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AmountInputViewModel2(
                App.marketKit,
                App.currencyManager,
                coin,
                coinDecimal,
                fiatDecimal,
                amountValidator
            ) as T
        }

    }
}
