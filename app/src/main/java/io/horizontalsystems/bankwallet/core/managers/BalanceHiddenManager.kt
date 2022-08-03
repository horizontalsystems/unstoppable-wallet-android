package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BalanceHiddenManager(
    private val localStorage: ILocalStorage
) {
    val balanceHidden: Boolean
        get() = localStorage.balanceHidden

    private val _balanceHiddenFlow = MutableStateFlow(localStorage.balanceHidden)
    val balanceHiddenFlow = _balanceHiddenFlow.asStateFlow()

    fun toggleBalanceHidden() {
        localStorage.balanceHidden = !localStorage.balanceHidden

        _balanceHiddenFlow.update { localStorage.balanceHidden }
    }

}
