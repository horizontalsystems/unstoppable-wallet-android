package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.TransactionAddress
import io.horizontalsystems.bankwallet.entities.TransactionRecord
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
        val contractAddress: String,
        gasLimit: Long,
        override val minimumRequiredBalance: BigDecimal,
        override val minimumSendAmount: BigDecimal
) : EthereumBaseAdapter(kit, decimal) {

    private val erc20Kit: Erc20Kit = Erc20Kit.getInstance(context, ethereumKit, contractAddress, gasLimit)

    // IBalanceAdapter

    override val state: AdapterState
        get() = when (erc20Kit.syncState) {
            is SyncState.Synced -> AdapterState.Synced
            is SyncState.NotSynced -> AdapterState.NotSynced
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

    override fun availableBalance(gasPrice: Long): BigDecimal {
        return balance - fee
    }

    override val ethereumBalance: BigDecimal
        get() = balanceInBigDecimal(ethereumKit.balance, EthereumAdapter.decimal)

    override fun fee(gasPrice: Long): BigDecimal {
        return erc20Kit.fee(gasPrice).movePointLeft(EthereumAdapter.decimal)
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val mineAddress = ethereumKit.receiveAddress

        val from = TransactionAddress(transaction.from, transaction.from == mineAddress)
        val to = TransactionAddress(transaction.to, transaction.to == mineAddress)

        var amount = BigDecimal.ZERO

        if (from.mine) {
            amount -= transaction.value.toBigDecimal()
        }
        if (to.mine) {
            amount += transaction.value.toBigDecimal()
        }

        return TransactionRecord(
                uid = "${transaction.transactionHash}${transaction.interTransactionIndex}",
                transactionHash = transaction.transactionHash,
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = transaction.interTransactionIndex,
                blockHeight = transaction.blockNumber,
                amount = amount.movePointLeft(decimal),
                timestamp = transaction.timestamp,
                from = listOf(from),
                to = listOf(to)
        )
    }

    companion object {
        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) EthereumKit.NetworkType.Ropsten else EthereumKit.NetworkType.MainNet
            Erc20Kit.clear(App.instance, networkType, walletId)
        }
    }

}
