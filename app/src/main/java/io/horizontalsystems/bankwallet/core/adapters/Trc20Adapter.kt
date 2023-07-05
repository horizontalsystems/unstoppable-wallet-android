package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.BalanceData
import io.horizontalsystems.bankwallet.core.ISendTronAdapter
import io.horizontalsystems.bankwallet.core.managers.TronKitWrapper
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.tronkit.TronKit.SyncState
import io.horizontalsystems.tronkit.models.Address
import io.horizontalsystems.tronkit.transaction.Fee
import io.reactivex.Flowable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.withContext
import java.math.BigInteger

class Trc20Adapter(
    tronKitWrapper: TronKitWrapper,
    contractAddress: String,
    wallet: Wallet
) : BaseTronAdapter(tronKitWrapper, wallet.decimal), ISendTronAdapter {

    private val contractAddress: Address = Address.fromBase58(contractAddress)

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
        get() = tronKit.syncStateFlow.map { }.asFlowable()

    override val balanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(tronKit.getTrc20Balance(contractAddress.base58), decimal))

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = tronKit.getTrc20BalanceFlow(contractAddress.base58).map { }.asFlowable()

    // ISendTronAdapter

    override val trxBalanceData: BalanceData
        get() = BalanceData(balanceInBigDecimal(tronKit.trxBalance, TronAdapter.decimal))

    override suspend fun estimateFee(amount: BigInteger, to: Address): List<Fee> = withContext(Dispatchers.IO) {
        val contract = tronKit.transferTrc20TriggerSmartContract(contractAddress, to, amount)
        tronKit.estimateFee(contract)
    }

    override suspend fun send(amount: BigInteger, to: Address, feeLimit: Long?) {
        if (signer == null) throw Exception()
        val contract = tronKit.transferTrc20TriggerSmartContract(contractAddress, to, amount)

        tronKit.send(contract, signer, feeLimit)
    }

    private fun convertToAdapterState(syncState: SyncState): AdapterState = when (syncState) {
        is SyncState.Synced -> AdapterState.Synced
        is SyncState.NotSynced -> AdapterState.NotSynced(syncState.error)
        is SyncState.Syncing -> AdapterState.Syncing()
    }

}
