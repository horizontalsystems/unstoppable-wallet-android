package io.horizontalsystems.bankwallet.modules.amount

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import java.math.BigDecimal
import java.math.RoundingMode

class AmountInputViewModel2(
    private val coinCode: String,
    private val coinDecimal: Int,
    private val fiatDecimal: Int,
    private var inputType: AmountInputType
) : ViewModel() {

    private var availableBalance = BigDecimal.ZERO

    var isMaxEnabled by mutableStateOf(false)
        private set

    var inputPrefix by mutableStateOf<String?>(null)
        private set

    var hint by mutableStateOf<String?>(null)
        private set

    var coinAmount: BigDecimal? = null
        private set

    private var rate: CurrencyValue? = null
    private var currencyAmount: BigDecimal? = null

    init {
        refreshHint()
        refreshInputPrefix()
    }

    fun onEnterAmount(text: String) {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull()?.stripTrailingZeros() else null

        when (inputType) {
            AmountInputType.COIN -> setCoinAmount(amount)
            AmountInputType.CURRENCY -> setCurrencyAmount(amount)
        }

        refreshHint()
    }

    fun onClickMax() {
        setCoinAmount(availableBalance)
        refreshHint()
    }

    fun setCoinAmountExternal(amount: BigDecimal) {
        setCoinAmount(amount)
        refreshHint()
    }

    private fun setCurrencyAmount(amount: BigDecimal?) {
        currencyAmount = amount
        calculateCoinAmount()
    }

    private fun setCoinAmount(amount: BigDecimal?) {
        coinAmount = amount
        calculateCurrencyAmount()
    }

    private fun calculateCurrencyAmount() {
        currencyAmount = rate?.let { rate ->
            coinAmount?.times(rate.value)?.setScale(fiatDecimal, RoundingMode.DOWN)?.stripTrailingZeros()
        }
    }

    private fun calculateCoinAmount() {
        coinAmount = rate?.let { rate ->
            currencyAmount?.divide(rate.value, coinDecimal, RoundingMode.CEILING)
                ?.stripTrailingZeros()
        }
    }

    fun getEnterAmount(): String {
        val amount = when (inputType) {
            AmountInputType.COIN -> coinAmount
            AmountInputType.CURRENCY -> currencyAmount
        }
        return amount?.toPlainString() ?: ""
    }

    private fun refreshHint() {
        val tmpRate = rate

        hint = if (tmpRate == null) {
            null
        } else {
            when (inputType) {
                AmountInputType.COIN -> {
                    App.numberFormatter.format(currencyAmount ?: BigDecimal.ZERO, fiatDecimal, fiatDecimal, prefix = tmpRate.currency.symbol)
                }
                AmountInputType.CURRENCY -> {
                    App.numberFormatter.formatCoinFull(coinAmount ?: BigDecimal.ZERO, coinCode, coinDecimal)
                }
            }
        }
    }

    fun setInputType(inputType: AmountInputType) {
        this.inputType = inputType

        refreshHint()
        refreshInputPrefix()
    }

    fun setAvailableBalance(availableBalance: BigDecimal) {
        this.availableBalance = availableBalance
        refreshIsMaxEnabled()
    }

    fun setRate(rate: CurrencyValue?) {
        this.rate = rate

        when (inputType) {
            AmountInputType.COIN -> calculateCurrencyAmount()
            AmountInputType.CURRENCY -> calculateCoinAmount()
        }

        refreshHint()
    }

    private fun refreshIsMaxEnabled() {
        isMaxEnabled = availableBalance > BigDecimal.ZERO
    }

    private fun refreshInputPrefix() {
        inputPrefix = when (inputType) {
            AmountInputType.COIN -> null
            AmountInputType.CURRENCY -> rate?.currency?.symbol
        }
    }

    fun isValid(text: String): Boolean {
        val amount = if (text.isNotBlank()) text.toBigDecimalOrNull() else null
        if (amount == null) return true

        val maxAllowedScale = when (inputType) {
            AmountInputType.COIN -> coinDecimal
            AmountInputType.CURRENCY -> fiatDecimal
        }

        return amount.scale() <= maxAllowedScale
    }

}