package io.horizontalsystems.walletkit.core.adapters

import io.horizontalsystems.walletkit.core.AdapterState
import io.horizontalsystems.walletkit.core.App
import io.horizontalsystems.walletkit.core.managers.EvmKitManagerRegistry
import io.horizontalsystems.walletkit.core.managers.EvmBlockchainManager
import io.horizontalsystems.walletkit.core.BalanceData
import io.horizontalsystems.walletkit.core.ICoinManager
import io.horizontalsystems.walletkit.core.managers.EvmKitWrapper
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.models.Address
import io.horizontalsystems.ethereumkit.models.Chain
import io.horizontalsystems.ethereumkit.models.TransactionData
import io.reactivex.Flowable
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

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = evmKit.syncStateFlowable.map {}

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(evmKit.accountState?.balance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = evmKit.accountStateFlowable.map { }

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
            // Driven by the canonical list rather than a copy of it: the copy that used to live
            // here was written when there were seven EVM chains and never grew, so deleting an
            // account left the Base, ZkSync and Fantom databases behind forever.
            EvmBlockchainManager.blockchainTypes.forEach { blockchainType ->
                EthereumKit.clear(App.instance, EvmKitManagerRegistry.getChain(blockchainType), walletId)
            }
        }
    }

}
