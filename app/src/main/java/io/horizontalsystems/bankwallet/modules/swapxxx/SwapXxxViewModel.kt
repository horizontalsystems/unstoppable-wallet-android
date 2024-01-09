package io.horizontalsystems.bankwallet.modules.swapxxx

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.horizontalsystems.marketkit.models.Token
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.math.BigDecimal

class SwapXxxViewModel : ViewModel() {
    private var spendingCoinAmount: BigDecimal? = null
    private var receivingCoinAmount: BigDecimal? = null
    private var tokenFrom: Token? = null
    private var tokenTo: Token? = null
    private var calculating = false

    var uiState: SwapXxxUiState by mutableStateOf(
        SwapXxxUiState(
            spendingCoinAmount = spendingCoinAmount,
            receivingCoinAmount = receivingCoinAmount,
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
                spendingCoinAmount = spendingCoinAmount,
                receivingCoinAmount = receivingCoinAmount,
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

                delay(1000)
                receivingCoinAmount = spendingCoinAmount.multiply(BigDecimal.TEN)
                calculating = false
                emitState()
            }
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SwapXxxViewModel() as T
        }
    }
}

data class SwapXxxUiState(
    val spendingCoinAmount: BigDecimal?,
    val receivingCoinAmount: BigDecimal?,
    val tokenFrom: Token?,
    val tokenTo: Token?,
    val calculating: Boolean,
    val swapEnabled: Boolean,
)