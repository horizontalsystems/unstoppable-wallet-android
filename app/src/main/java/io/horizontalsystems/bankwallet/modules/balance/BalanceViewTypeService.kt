package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.ui.compose.Select
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BalanceViewTypeService(private val localStorage: ILocalStorage) {
    private val viewTypes = BalanceViewType.values().asList()

    private val _optionsFlow = MutableStateFlow(
        Select(localStorage.balanceViewType ?: BalanceViewType.CoinThenFiat, viewTypes)
    )
    val optionsFlow = _optionsFlow.asStateFlow()

    fun setViewType(viewType: BalanceViewType) {
        localStorage.balanceViewType = viewType

        _optionsFlow.update {
            Select(viewType, viewTypes)
        }
    }
}
