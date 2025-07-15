package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


interface ITotalBalance {
    val balanceHidden: Boolean
    val totalUiState: TotalUIState
    val stateFlow: StateFlow<TotalService.State>

    fun toggleBalanceVisibility()
    fun toggleTotalType()
}

class TotalBalance(
    private val totalService: TotalService,
    private val balanceHiddenManager: BalanceHiddenManager,
) : ITotalBalance {

    private var totalState = totalService.stateFlow.value

    override val balanceHidden by balanceHiddenManager::balanceHidden

    override var totalUiState by mutableStateOf(createTotalUIState())
        private set

    override val stateFlow: StateFlow<TotalService.State>
        get() = totalService.stateFlow

    fun start(viewModelScope: CoroutineScope) {
        viewModelScope.launch {
            totalService.stateFlow.collect {
                totalState = it
                totalUiState = createTotalUIState()
            }
        }
        totalService.start()
    }

    private fun createTotalUIState() = when (val state = totalState) {
        TotalService.State.Hidden -> TotalUIState.Hidden
        is TotalService.State.Visible -> TotalUIState.Visible(
            primaryAmountStr = getPrimaryAmount(state, state.showFullAmount) ?: "---",
            secondaryAmountStr = getSecondaryAmount(state, state.showFullAmount) ?: "---",
            dimmed = state.dimmed
        )
    }

    fun stop() {
        totalService.stop()
    }

    fun setTotalServiceItems(map: List<TotalService.BalanceItem>?) {
        totalService.setItems(map)
    }

    override fun toggleBalanceVisibility() {
        balanceHiddenManager.toggleBalanceHidden()
    }

    override fun toggleTotalType() {
        totalService.toggleType()
    }

    private fun getPrimaryAmount(
        totalState: TotalService.State.Visible,
        fullFormat: Boolean
    ) = totalState.currencyValue?.let {
        if (fullFormat) {
            App.numberFormatter.formatFiatFull(it.value, it.currency.symbol)
        } else {
            App.numberFormatter.formatFiatShort(it.value, it.currency.symbol, 8)
        }
    }

    private fun getSecondaryAmount(
        totalState: TotalService.State.Visible,
        fullFormat: Boolean
    ) = totalState.coinValue?.let {
        if (fullFormat) {
            "≈" + App.numberFormatter.formatCoinFull(it.value, it.coin.code, it.decimal)
        } else {
            "≈" + App.numberFormatter.formatCoinShort(it.value, it.coin.code, it.decimal)
        }
    }
}
