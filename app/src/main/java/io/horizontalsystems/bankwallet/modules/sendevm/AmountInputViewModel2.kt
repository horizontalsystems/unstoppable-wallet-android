package io.horizontalsystems.bankwallet.modules.sendevm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.modules.send.SendModule
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
    private val localStorage: ILocalStorage,
    private var inputType: SendModule.InputType
) : ViewModel() {

    interface AmountValidator {
        fun validateAmount(amount: BigDecimal?): Caution?
    }

    var availableBalance = BigDecimal.ZERO

    val isMaxEnabled: Boolean
        get() = availableBalance > BigDecimal.ZERO

    var inputPrefix by mutableStateOf<String?>(null)
        private set

    var hint by mutableStateOf<String?>(null)
        private set

    var coinAmount: BigDecimal? = null
        private set

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

    override fun onCleared() {
        disposables.clear()
    }

    fun onEnterAmount(text: String) {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull()?.stripTrailingZeros() else null

        when (inputType) {
            SendModule.InputType.COIN -> updateCoinAmount(amount)
            SendModule.InputType.CURRENCY -> updateCurrencyAmount(amount)
        }

        refreshHint()
    }

    fun onClickMax() {
        updateCoinAmount(availableBalance)
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
            amount?.times(rate.value)?.setScale(fiatDecimal, RoundingMode.DOWN)?.stripTrailingZeros()
        }
    }

    fun getEnterAmount(): String {
        val amount = when (inputType) {
            SendModule.InputType.COIN -> coinAmount
            SendModule.InputType.CURRENCY -> currencyAmount
        }
        return amount?.toPlainString() ?: ""
    }

    private fun refreshHint() {
        hint = if (rate == null) {
            null
        } else {
            when (inputType) {
                SendModule.InputType.COIN -> {
                    App.numberFormatter.format(currencyAmount ?: BigDecimal.ZERO, fiatDecimal, fiatDecimal, prefix = currency.symbol)
                }
                SendModule.InputType.CURRENCY -> {
                    App.numberFormatter.formatCoin(coinAmount ?: BigDecimal.ZERO, coin.code, 0, coinDecimal)
                }
            }
        }
    }

    fun setInputType(inputType: SendModule.InputType) {
        this.inputType = inputType

        refreshHint()
        updateInputPrefix()
    }

    private fun updateInputPrefix() {
        inputPrefix = when (inputType) {
            SendModule.InputType.COIN -> null
            SendModule.InputType.CURRENCY -> currency.symbol
        }
    }

    fun isValid(text: String): Boolean {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull() else null
        if (amount == null) return true

        val maxAllowedScale = when (inputType) {
            SendModule.InputType.COIN -> coinDecimal
            SendModule.InputType.CURRENCY -> fiatDecimal
        }

        return amount.scale() <= maxAllowedScale
    }

}

object AmountInputModule {

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val coin: Coin,
        private val coinDecimal: Int,
        private val fiatDecimal: Int,
        private val amountValidator: AmountInputViewModel2.AmountValidator?,
        private val inputType: SendModule.InputType
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AmountInputViewModel2(
                App.marketKit,
                App.currencyManager,
                coin,
                coinDecimal,
                fiatDecimal,
                amountValidator,
                App.localStorage,
                inputType
            ) as T
        }

    }
}
