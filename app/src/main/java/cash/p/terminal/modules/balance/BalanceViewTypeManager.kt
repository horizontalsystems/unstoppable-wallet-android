package cash.p.terminal.modules.balance

import cash.p.terminal.core.ILocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BalanceViewTypeManager(private val localStorage: ILocalStorage) {
    val viewTypes = BalanceViewType.values().asList()

    private val _balanceViewTypeFlow = MutableStateFlow(
        localStorage.balanceViewType ?: BalanceViewType.CoinThenFiat
    )
    val balanceViewTypeFlow = _balanceViewTypeFlow.asStateFlow()

    fun setViewType(viewType: BalanceViewType) {
        localStorage.balanceViewType = viewType

        _balanceViewTypeFlow.update {
            viewType
        }
    }
}
