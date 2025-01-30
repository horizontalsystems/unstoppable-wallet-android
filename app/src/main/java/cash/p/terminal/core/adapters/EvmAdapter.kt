package cash.p.terminal.core.adapters

import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.core.App
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.core.ICoinManager
import cash.p.terminal.core.managers.EvmKitWrapper
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Flowable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal

class EvmAdapter(evmKitWrapper: EvmKitWrapper, coinManager: ICoinManager) :
    BaseEvmAdapter(evmKitWrapper, decimal, coinManager) {

    // IAdapter

    override fun start() {
        // started via EthereumKitManager
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        // refreshed via EthereumKitManager
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(evmKit.syncState)

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = evmKit.syncStateFlowable.map {}.asFlow()

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(evmKit.accountState?.balance, decimal))

    override val balanceUpdatedFlow: Flow<Unit>
        get() = evmKit.accountStateFlowable.map { }.asFlow()

    private fun convertToAdapterState(syncState: EthereumKit.SyncState): AdapterState =
        when (syncState) {
            is EthereumKit.SyncState.Synced -> AdapterState.Synced
            is EthereumKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is EthereumKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    // ISendEthereumAdapter

    override fun getTransactionData(amount: BigDecimal, address: Address): TransactionData {
        val amountBigInt = amount.movePointRight(decimal).toBigInteger()
        return TransactionData(address, amountBigInt, byteArrayOf())
    }

    companion object {
        const val decimal = 18

        fun clear(walletId: String) {
            val networkTypes = listOf(
                Chain.Ethereum,
                Chain.BinanceSmartChain,
                Chain.Polygon,
                Chain.Avalanche,
                Chain.Optimism,
                Chain.ArbitrumOne,
                Chain.Gnosis,
            )
            networkTypes.forEach {
                EthereumKit.clear(App.instance, it, walletId)
            }
        }
    }

}
