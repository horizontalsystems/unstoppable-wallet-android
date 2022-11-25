package io.horizontalsystems.bankwallet.modules.balance

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.BalanceHiddenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


interface ITotalBalance {
    val balanceHidden: Boolean
    val totalUiState: TotalUIState

    fun toggleBalanceVisibility()
}

class TotalBalance(
    private val totalService: TotalService,
    private val balanceViewTypeManager: BalanceViewTypeManager,
    private val balanceHiddenManager: BalanceHiddenManager,
) : ITotalBalance {

    private var balanceViewType = balanceViewTypeManager.balanceViewTypeFlow.value
    private var totalState = totalService.stateFlow.value

    override val balanceHidden by balanceHiddenManager::balanceHidden

    override var totalUiState by mutableStateOf(createTotalUIState())
        private set

    fun start(viewModelScope: CoroutineScope) {
        viewModelScope.launch {
            totalService.stateFlow.collect {
                totalState = it
                totalUiState = createTotalUIState()
            }
        }
        viewModelScope.launch {
            balanceViewTypeManager.balanceViewTypeFlow.collect {
                balanceViewType = it
                totalUiState = createTotalUIState()
            }
        }
        totalService.start()
    }

    fun createTotalUIState() = when (val state = totalState) {
        TotalService.State.Hidden -> TotalUIState.Hidden
        is TotalService.State.Visible -> TotalUIState.Visible(
            primaryAmountStr = getPrimaryAmount(balanceViewType, state) ?: "---",
            secondaryAmountStr = getSecondaryAmount(balanceViewType, state) ?: "---",
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

    private fun getPrimaryAmount(
        balanceViewType: BalanceViewType,
        totalState: TotalService.State.Visible
    ): String? {
        return when (balanceViewType) {
            BalanceViewType.CoinThenFiat -> totalState.coinValue?.let {
                App.numberFormatter.formatCoinFull(it.value, it.coin.code, it.decimal)
            }
            BalanceViewType.FiatThenCoin -> totalState.currencyValue?.let {
                App.numberFormatter.formatFiatFull(it.value, it.currency.symbol)
            }
        }
    }

    private fun getSecondaryAmount(
        balanceViewType: BalanceViewType,
        totalState: TotalService.State.Visible
    ): String? {
        return when (balanceViewType) {
            BalanceViewType.CoinThenFiat -> totalState.currencyValue?.let {
                "~" + App.numberFormatter.formatFiatFull(it.value, it.currency.symbol)
            }
            BalanceViewType.FiatThenCoin -> totalState.coinValue?.let {
                "~" + App.numberFormatter.formatCoinFull(it.value, it.coin.code, it.decimal)
            }
        }
    }
}
