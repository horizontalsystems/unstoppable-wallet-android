package cash.p.terminal.core.adapters

import cash.p.terminal.core.INativeBalanceProvider
import cash.p.terminal.core.managers.SolanaKitWrapper
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.entities.BalanceData
import io.horizontalsystems.core.SafeSuspendedCall
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.Address
import io.horizontalsystems.solanakit.models.FullTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal

class SplAdapter(
    private val solanaKitWrapper: SolanaKitWrapper,
    wallet: Wallet,
    private val mintAddressString: String
) : BaseSolanaAdapter(solanaKitWrapper, wallet.decimal), INativeBalanceProvider {

    private val mintAddress = Address(mintAddressString)

    init {
        solanaKit.addTokenAccount(mintAddressString, wallet.decimal)
    }

    // IAdapter

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override suspend fun refresh() {
        solanaKitWrapper.solanaKit.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(solanaKit.tokenBalanceSyncState)

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = solanaKit.tokenBalanceSyncStateFlow.map { }

    override val balanceData: BalanceData
        get() = BalanceData(
                solanaKit.tokenAccount(mintAddressString)?.let {
                    it.tokenAccount.balance.movePointLeft(it.tokenAccount.decimals)
                } ?: BigDecimal.ZERO
        )

    override val balanceUpdatedFlow: Flow<Unit>
        get() = solanaKit.tokenAccountFlow(mintAddressString).map { }

    override val maxSpendableBalance: BigDecimal
        get() = balanceData.available

    // INativeBalanceProvider

    override val nativeBalanceData: BalanceData
        get() = BalanceData(SolanaAdapter.balanceInBigDecimal(solanaKit.balance, SolanaAdapter.decimal))

    override val nativeBalanceUpdatedFlow: Flow<Unit>
        get() = solanaKit.balanceFlow.map { }

    override suspend fun send(amount: BigDecimal, to: Address): FullTransaction {
        if (signer == null) throw Exception()

        return SafeSuspendedCall.executeSuspendable {
            solanaKit.sendSpl(mintAddress, to, amount.movePointRight(decimal).toLong(), signer)
        }
    }

    private fun convertToAdapterState(syncState: SolanaKit.SyncState): AdapterState = when (syncState) {
        is SolanaKit.SyncState.Synced -> AdapterState.Synced
        is SolanaKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SolanaKit.SyncState.Syncing -> AdapterState.Syncing()
    }

}
