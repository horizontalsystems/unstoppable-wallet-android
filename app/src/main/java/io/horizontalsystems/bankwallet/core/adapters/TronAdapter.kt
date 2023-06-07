package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.managers.TronKitWrapper
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.network.Network
import io.horizontalsystems.tronkit.transaction.Fee
import io.reactivex.Flowable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import java.math.BigInteger

class TronAdapter(kitWrapper: TronKitWrapper) : BaseTronAdapter(kitWrapper, decimal), ISendTronAdapter {

    // IAdapter

    override fun start() {
        // started via TronKitManager
    }

    override fun stop() {
        // stopped via TronKitManager
    }

    override fun refresh() {
        // refreshed via TronKitManager
    }

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = convertToAdapterState(tronKit.syncState)

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = tronKit.syncStateFlow.map {}.asFlowable()

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(tronKit.trxBalance, decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = tronKit.trxBalanceFlow.map {}.asFlowable()

    // ISendTronAdapter

    override val trxBalanceData: BalanceData
        get() = balanceData

    override suspend fun estimateFee(amount: BigInteger, to: Address): List<Fee> {
        val contract = tronKit.transferContract(amount, to)
        return tronKit.estimateFee(contract)
    }

    override suspend fun send(amount: BigInteger, to: Address, feeLimit: Long?) {
        if (signer == null) throw Exception()
        val contract = tronKit.transferContract(amount, to)

        tronKit.send(contract, signer, feeLimit)
    }

    private fun convertToAdapterState(syncState: TronKit.SyncState): AdapterState =
        when (syncState) {
            is TronKit.SyncState.Synced -> AdapterState.Synced
            is TronKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is TronKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    companion object {
        const val decimal = 6

        fun clear(walletId: String) {
            Network.values().forEach { network ->
                TronKit.clear(App.instance, network, walletId)
            }
        }
    }

}
