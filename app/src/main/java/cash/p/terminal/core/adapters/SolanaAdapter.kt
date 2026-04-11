package cash.p.terminal.core.adapters

import cash.p.terminal.core.App
import cash.p.terminal.core.managers.SolanaKitWrapper
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.core.SafeSuspendedCall
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.Address
import io.horizontalsystems.solanakit.models.FullTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class SolanaAdapter(private val kitWrapper: SolanaKitWrapper) :
    BaseSolanaAdapter(kitWrapper, decimal) {

    // IAdapter

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override suspend fun refresh() {
        kitWrapper.solanaKit.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(solanaKit.syncState)

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = solanaKit.balanceSyncStateFlow.map {}

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(solanaKit.balance, decimal))

    override val balanceUpdatedFlow: Flow<Unit>
        get() = solanaKit.balanceFlow.map {}

    override val fee: StateFlow<BigDecimal> =
        MutableStateFlow(SolanaKit.fee + SolanaKit.accountRentAmount)

    override val maxSpendableBalance: BigDecimal
        get() = maxOf(balanceData.available - fee.value, BigDecimal.ZERO)

    override suspend fun send(amount: BigDecimal, to: Address): FullTransaction {
        if (signer == null) throw Exception()

        return SafeSuspendedCall.executeSuspendable {
            solanaKit.sendSol(to, amount.movePointRight(decimal).toLong(), signer)
        }
    }

    private fun convertToAdapterState(syncState: SolanaKit.SyncState): AdapterState =
        when (syncState) {
            is SolanaKit.SyncState.Synced -> AdapterState.Synced
            is SolanaKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is SolanaKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    companion object {
        const val decimal = 9

        private fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
            return amount.movePointLeft(decimals).stripTrailingZeros()
        }

        fun balanceInBigDecimal(balance: Long?, decimal: Int): BigDecimal {
            balance?.toBigDecimal()?.let {
                return scaleDown(it, decimal)
            } ?: return BigDecimal.ZERO
        }

        fun clear(walletId: String) {
            SolanaKit.clear(App.instance, walletId)
        }
    }

}
