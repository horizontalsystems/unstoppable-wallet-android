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
            primaryAmountStr = getPrimaryAmount(state) ?: "---",
            secondaryAmountStr = getSecondaryAmount(state) ?: "---",
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
        totalState: TotalService.State.Visible
    ) = totalState.currencyValue?.let {
        App.numberFormatter.formatFiatShort(it.value, it.currency.symbol, 2)
    }

    private fun getSecondaryAmount(
        totalState: TotalService.State.Visible
    ) = totalState.coinValue?.let {
        "~" + App.numberFormatter.formatCoinFull(it.value, it.coin.code, it.decimal)
    }
}
