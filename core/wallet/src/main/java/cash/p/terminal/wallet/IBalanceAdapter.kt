package cash.p.terminal.wallet

import cash.p.terminal.wallet.entities.BalanceData
import io.reactivex.Flowable

interface IBalanceAdapter {
    val balanceState: AdapterState
    val balanceStateUpdatedFlowable: Flowable<Unit>

    val balanceData: BalanceData
    val balanceUpdatedFlowable: Flowable<Unit>

    fun sendAllowed() = balanceState is AdapterState.Synced
}