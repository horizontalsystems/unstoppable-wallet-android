package cash.p.terminal.wallet.managers

import kotlinx.coroutines.flow.StateFlow

interface IBalanceHiddenManager {
    val balanceHidden: Boolean
    val balanceAutoHidden: Boolean
    val balanceHiddenFlow: StateFlow<Boolean>
    fun toggleBalanceHidden()
    fun setBalanceAutoHidden(enabled: Boolean)
}