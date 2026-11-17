package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.ILocalStorage
import io.horizontalsystems.walletkit.modules.settings.appearance.AppearanceDefaults
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BalanceViewTypeManager(private val localStorage: ILocalStorage) {
    val viewTypes = BalanceViewType.entries

    val balanceViewType: BalanceViewType
        get() = localStorage.balanceViewType ?: AppearanceDefaults.balanceViewType

    private val _balanceViewTypeFlow = MutableStateFlow(balanceViewType)

    val balanceViewTypeFlow = _balanceViewTypeFlow.asStateFlow()

    fun setViewType(viewType: BalanceViewType) {
        localStorage.balanceViewType = viewType

        _balanceViewTypeFlow.update {
            viewType
        }
    }
}
