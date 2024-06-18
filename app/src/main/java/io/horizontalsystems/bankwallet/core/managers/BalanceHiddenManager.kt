package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.BackgroundManagerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BalanceHiddenManager(
    private val localStorage: ILocalStorage,
    backgroundManager: BackgroundManager,
) {
    val balanceHidden: Boolean
        get() = localStorage.balanceHidden

    val balanceAutoHidden: Boolean
        get() = localStorage.balanceAutoHideEnabled

    private var balanceAutoHide = balanceAutoHidden

    private val _balanceHiddenFlow = MutableStateFlow(localStorage.balanceHidden)
    val balanceHiddenFlow = _balanceHiddenFlow.asStateFlow()
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            backgroundManager.stateFlow.collect { state ->
                if (state == BackgroundManagerState.EnterBackground && balanceAutoHide) {
                    setBalanceHidden(true)
                }
            }
        }

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

    private fun setBalanceHidden(hidden: Boolean) {
        localStorage.balanceHidden = hidden
        _balanceHiddenFlow.update { localStorage.balanceHidden }
    }

}
