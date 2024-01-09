package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.CurrencyManager
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapXxxViewModel(private val currencyManager: CurrencyManager) : ViewModel() {
    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapXxxViewModel(App.currencyManager) as T
        }
    }

    private var coinAmountHint = "0"
    private var currencyAmountHint = "${currencyManager.baseCurrency.symbol}0"
    private var spendingCoinAmount: BigDecimal? = null
    private var spendingCurrencyAmount = ""
    private var receivingCoinAmount: BigDecimal? = null
    private var receivingCurrencyAmount = ""
    private var tokenFrom: Token? = null
    private var tokenTo: Token? = null
    private var calculating = false

    var uiState: SwapXxxUiState by mutableStateOf(
        SwapXxxUiState(
            coinAmountHint = coinAmountHint,
            currencyAmountHint = currencyAmountHint,
            spendingCoinAmount = spendingCoinAmount,
            spendingCurrencyAmount = spendingCurrencyAmount,
            receivingCoinAmount = receivingCoinAmount,
            receivingCurrencyAmount = receivingCurrencyAmount,
            tokenFrom = tokenFrom,
            tokenTo = tokenTo,
            calculating = calculating,
            swapEnabled = isSwapEnabled(),
        )
    )
        private set

    private var calculatingJob: Job? = null

    fun onEnterAmount(v: BigDecimal?) {
        spendingCoinAmount = v
        receivingCoinAmount = null
        emitState()

        runCalculation()
    }

    fun onSelectTokenFrom(token: Token) {
        tokenFrom = token
        receivingCoinAmount = null
        emitState()

        runCalculation()
    }

    fun onSelectTokenTo(token: Token) {
        tokenTo = token

        receivingCoinAmount = null
        emitState()

        runCalculation()
    }

    fun onSwitchPairs() {
        val tmpTokenFrom = tokenFrom

        tokenFrom = tokenTo
        tokenTo = tmpTokenFrom

        spendingCoinAmount = receivingCoinAmount
        receivingCoinAmount = null
        emitState()

        runCalculation()
    }

    private fun emitState() {
        viewModelScope.launch {
            uiState = SwapXxxUiState(
                coinAmountHint = coinAmountHint,
                currencyAmountHint = currencyAmountHint,
                spendingCoinAmount = spendingCoinAmount,
                spendingCurrencyAmount = spendingCurrencyAmount,
                receivingCoinAmount = receivingCoinAmount,
                receivingCurrencyAmount = receivingCurrencyAmount,
                tokenFrom = tokenFrom,
                tokenTo = tokenTo,
                calculating = calculating,
                swapEnabled = isSwapEnabled(),
            )
        }
    }

    private fun isSwapEnabled() = receivingCoinAmount != null

    private fun runCalculation() {
        calculatingJob?.cancel()

        val spendingCoinAmount = spendingCoinAmount
        val tokenFrom = tokenFrom
        val tokenTo = tokenTo

        if (spendingCoinAmount == null || tokenFrom == null || tokenTo == null) {
            calculating = false
            emitState()
        } else {
            calculatingJob = viewModelScope.launch {
                calculating = true
                emitState()

                delay(3000)
                receivingCoinAmount = spendingCoinAmount.multiply(BigDecimal.TEN)
                calculating = false
                emitState()
            }
        }
    }
}

data class SwapXxxUiState(
    val coinAmountHint: String,
    val currencyAmountHint: String,
    val spendingCoinAmount: BigDecimal?,
    val spendingCurrencyAmount: String,
    val receivingCoinAmount: BigDecimal?,
    val receivingCurrencyAmount: String,
    val tokenFrom: Token?,
    val tokenTo: Token?,
    val calculating: Boolean,
    val swapEnabled: Boolean,
)