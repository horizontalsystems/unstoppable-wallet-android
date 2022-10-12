package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendSolanaAdapter
import io.horizontalsystems.bankwallet.core.managers.SolanaKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.solanakit.SolanaKit
import io.horizontalsystems.solanakit.models.Address
import io.horizontalsystems.solanakit.models.FullTransaction
import io.reactivex.Flowable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import java.math.BigDecimal

class SplAdapter(
        solanaKitWrapper: SolanaKitWrapper,
        wallet: Wallet,
        private val mintAddressString: String
) : BaseSolanaAdapter(solanaKitWrapper, wallet.decimal), ISendSolanaAdapter {

    val mintAddress = Address(mintAddressString)

    // IAdapter

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        solanaKit.refresh()
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(solanaKit.tokenBalanceSyncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = solanaKit.tokenBalanceSyncStateFlow.map { }.asFlowable()

    override val balanceData: BalanceData
        get() = BalanceData(solanaKit.tokenBalance(mintAddressString) ?: BigDecimal.ZERO)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = solanaKit.tokenBalanceFlow(mintAddressString).map { }.asFlowable()

    // ISendSolanaAdapter
    override val availableBalance: BigDecimal
        get() = solanaKit.tokenBalance(mintAddressString) ?: BigDecimal.ZERO

    override suspend fun send(amount: BigDecimal, to: Address): FullTransaction {
        if (signer == null) throw Exception()

        return solanaKit.sendSpl(mintAddress, to, amount, signer)
    }

    private fun convertToAdapterState(syncState: SolanaKit.SyncState): AdapterState = when (syncState) {
        is SolanaKit.SyncState.Synced -> AdapterState.Synced
        is SolanaKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SolanaKit.SyncState.Syncing -> AdapterState.Syncing()
    }

}
