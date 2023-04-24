package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.BackgroundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class BalanceHiddenManager(
    private val localStorage: ILocalStorage,
    backgroundManager: BackgroundManager,
): BackgroundManager.Listener {
    val balanceHidden: Boolean
        get() = localStorage.balanceHidden

    val balanceAutoHidden: Boolean
        get() = localStorage.balanceAutoHideEnabled

    private var balanceAutoHide = balanceAutoHidden

    private val _balanceHiddenFlow = MutableStateFlow(localStorage.balanceHidden)
    val balanceHiddenFlow = _balanceHiddenFlow.asStateFlow()

    init {
        backgroundManager.registerListener(this)

        if (balanceAutoHide) {
            setBalanceHidden(true)
        }
    }

    fun toggleBalanceHidden() {
        setBalanceHidden(!localStorage.balanceHidden)
    }

    fun setBalanceAutoHidden(enabled: Boolean) {
        balanceAutoHide = enabled
        localStorage.balanceAutoHideEnabled = enabled

        if (balanceAutoHide) {
            setBalanceHidden(true)
        }
    }

    override fun didEnterBackground() {
        if (balanceAutoHide) {
            setBalanceHidden(true)
        }
    }

    private fun setBalanceHidden(hidden: Boolean) {
        localStorage.balanceHidden = hidden
        _balanceHiddenFlow.update { localStorage.balanceHidden }
    }

}
