package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.ILocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BalanceViewTypeManager(private val localStorage: ILocalStorage) {
    val viewTypes = BalanceViewType.entries

    val balanceViewType: BalanceViewType
        get() = localStorage.balanceViewType ?: BalanceViewType.CoinThenFiat

    private val _balanceViewTypeFlow = MutableStateFlow(balanceViewType)

    val balanceViewTypeFlow = _balanceViewTypeFlow.asStateFlow()

    fun setViewType(viewType: BalanceViewType) {
        localStorage.balanceViewType = viewType

        _balanceViewTypeFlow.update {
            viewType
        }
    }
}
