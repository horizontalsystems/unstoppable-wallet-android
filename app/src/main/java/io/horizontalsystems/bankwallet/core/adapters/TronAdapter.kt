package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.managers.TronKitWrapper
import io.horizontalsystems.tronkit.TronKit
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.network.Network
import io.reactivex.Flowable
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import java.math.BigDecimal
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

    override suspend fun send(amount: BigInteger, to: Address): String {
        if (signer == null) throw Exception()

        val transferContract = tronKit.transferContract(amount, to)

        return tronKit.send(transferContract, signer)
    }

    private fun convertToAdapterState(syncState: TronKit.SyncState): AdapterState =
        when (syncState) {
            is TronKit.SyncState.Synced -> AdapterState.Synced
            is TronKit.SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
            is TronKit.SyncState.Syncing -> AdapterState.Syncing()
        }

    private fun scaleDown(amount: BigDecimal, decimals: Int = decimal): BigDecimal {
        return amount.movePointLeft(decimals).stripTrailingZeros()
    }

    private fun scaleUp(amount: BigDecimal, decimals: Int = decimal): BigInteger {
        return amount.movePointRight(decimals).toBigInteger()
    }

    private fun balanceInBigDecimal(balance: BigInteger?, decimal: Int): BigDecimal {
        balance?.toBigDecimal()?.let {
            return scaleDown(it, decimal)
        } ?: return BigDecimal.ZERO
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
