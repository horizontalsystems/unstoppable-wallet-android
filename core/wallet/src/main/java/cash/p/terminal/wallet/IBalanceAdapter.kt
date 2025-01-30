package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.BalanceData
import kotlinx.coroutines.flow.Flow

interface IBalanceAdapter {
    val balanceState: AdapterState
    val balanceStateUpdatedFlow: Flow<Unit>

    val balanceData: BalanceData
    val balanceUpdatedFlow: Flow<Unit>

    fun sendAllowed() = balanceState is AdapterState.Synced
}