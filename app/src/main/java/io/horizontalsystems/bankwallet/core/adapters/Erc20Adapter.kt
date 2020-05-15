package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState
import io.horizontalsystems.erc20kit.core.TransactionKey
import io.horizontalsystems.erc20kit.models.TransactionInfo
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

class Erc20Adapter(
        context: Context,
        kit: EthereumKit,
        decimal: Int,
        private val fee: BigDecimal,
        private val contractAddress: String,
        gasLimit: Long,
        override val minimumRequiredBalance: BigDecimal,
        override val minimumSendAmount: BigDecimal
) : EthereumBaseAdapter(kit, decimal) {

    private val erc20Kit: Erc20Kit = Erc20Kit.getInstance(context, ethereumKit, contractAddress, gasLimit)

    // IAdapter

    override fun start() {
        erc20Kit.refresh()
    }

    override fun stop() {
        // stopped via EthereumKitManager
    }

    override fun refresh() {
        erc20Kit.refresh()
    }

    // IBalanceAdapter

    override val state: AdapterState
        get() = when (val kitSyncState = erc20Kit.syncState) {
            is SyncState.Synced -> AdapterState.Synced
            is SyncState.NotSynced -> AdapterState.NotSynced(kitSyncState.error)
            is SyncState.Syncing -> AdapterState.Syncing(50, null)
        }

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = erc20Kit.syncStateFlowable.map { Unit }

    override val balance: BigDecimal
        get() = balanceInBigDecimal(erc20Kit.balance, decimal)

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = erc20Kit.balanceFlowable.map { Unit }

    // ITransactionsAdapter

    override fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>> {
        return erc20Kit.transactions(from?.let { TransactionKey(it.transactionHash.hexStringToByteArray(), it.interTransactionIndex) }, limit).map {
            it.map { tx -> transactionRecord(tx) }
        }
    }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = erc20Kit.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }

    // ISendEthereumAdapter

    override fun sendSingle(address: String, amount: String, gasPrice: Long, gasLimit: Long): Single<Unit> {
        return erc20Kit.send(address, amount, gasPrice, gasLimit).map { Unit }
    }

    override fun estimateGasLimit(toAddress: String, value: BigDecimal, gasPrice: Long?): Single<Long> {

        val poweredDecimal = value.scaleByPowerOfTen(decimal)
        val noScaleDecimal = poweredDecimal.setScale(0)

        return erc20Kit.estimateGas(toAddress, contractAddress, noScaleDecimal.toBigInteger(), gasPrice)
    }

    override fun availableBalance(gasPrice: Long, gasLimit: Long?): BigDecimal {
        return BigDecimal.ZERO.max(balance - fee)
    }

    override val ethereumBalance: BigDecimal
        get() = balanceInBigDecimal(ethereumKit.balance, EthereumAdapter.decimal)

    override fun fee(gasPrice: Long, gasLimit: Long): BigDecimal {
        val value = BigDecimal(gasPrice) * BigDecimal(gasLimit)

        return value.movePointLeft(EthereumAdapter.decimal)
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val myAddress = ethereumKit.receiveAddress
        val fromMine = transaction.from == myAddress
        val toMine = transaction.to == myAddress

        val type = when {
            fromMine && toMine -> TransactionType.SentToSelf
            fromMine -> TransactionType.Outgoing
            else -> TransactionType.Incoming
        }

        return TransactionRecord(
                uid = "${transaction.transactionHash}${transaction.interTransactionIndex}",
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = transaction.interTransactionIndex,
                blockHeight = transaction.blockNumber,
                amount = transaction.value.toBigDecimal().movePointLeft(decimal),
                timestamp = transaction.timestamp,
                from = transaction.from,
                to = transaction.to,
                type = type,
                failed = transaction.isError
        )
    }

    companion object {
        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) EthereumKit.NetworkType.Ropsten else EthereumKit.NetworkType.MainNet
            Erc20Kit.clear(App.instance, networkType, walletId)
        }
    }

}
