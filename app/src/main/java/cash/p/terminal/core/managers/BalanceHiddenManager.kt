package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.wallet.managers.IBalanceHiddenManager
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
) : IBalanceHiddenManager {
    override val balanceHidden: Boolean
        get() = localStorage.balanceHidden

    override val balanceAutoHidden: Boolean
        get() = localStorage.balanceAutoHideEnabled

    private var balanceAutoHide = balanceAutoHidden

    private val _balanceHiddenFlow = MutableStateFlow(localStorage.balanceHidden)
    override val balanceHiddenFlow = _balanceHiddenFlow.asStateFlow()
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

    override fun toggleBalanceHidden() {
        setBalanceHidden(!localStorage.balanceHidden)
    }

    override fun setBalanceAutoHidden(enabled: Boolean) {
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
