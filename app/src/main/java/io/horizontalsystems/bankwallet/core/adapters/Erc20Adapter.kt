package io.horizontalsystems.bankwallet.core.adapters

import android.content.Context
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.AppLogger
import io.horizontalsystems.bankwallet.core.toHexString
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.erc20kit.core.Erc20Kit
import io.horizontalsystems.erc20kit.core.Erc20Kit.SyncState
import io.horizontalsystems.erc20kit.core.TransactionKey
import io.horizontalsystems.erc20kit.models.Transaction
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.ethereumkit.core.hexStringToByteArray
import io.horizontalsystems.ethereumkit.models.Address
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal
import java.math.BigInteger

class Erc20Adapter(
        context: Context,
        kit: EthereumKit,
        decimal: Int,
        contractAddress: String,
        private val fee: BigDecimal,
        override val minimumRequiredBalance: BigDecimal,
        override val minimumSendAmount: BigDecimal
) : EthereumBaseAdapter(kit, decimal) {

    private val contractAddress: Address = Address(contractAddress)
    private val erc20Kit: Erc20Kit = Erc20Kit.getInstance(context, ethereumKit, this.contractAddress)

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

    override fun sendInternal(address: Address, amount: BigInteger, gasPrice: Long, gasLimit: Long, logger: AppLogger): Single<Unit> {
        return erc20Kit.send(address, amount, gasPrice, gasLimit)
                .doOnSubscribe {
                    logger.info("call erc20Kit.send")
                }
                .map { Unit }
    }

    override fun estimateGasLimitInternal(toAddress: Address?, value: BigInteger, gasPrice: Long?): Single<Long> {
        return erc20Kit.estimateGas(toAddress, contractAddress, value, gasPrice)
    }

    override fun availableBalance(gasPrice: Long, gasLimit: Long): BigDecimal {
        return BigDecimal.ZERO.max(balance - fee)
    }

    override val ethereumBalance: BigDecimal
        get() = balanceInBigDecimal(ethereumKit.balance, EthereumAdapter.decimal)

    private fun transactionRecord(transaction: Transaction): TransactionRecord {
        val myAddress = ethereumKit.receiveAddress
        val fromMine = transaction.from == myAddress
        val toMine = transaction.to == myAddress

        val type = when {
            fromMine && toMine -> TransactionType.SentToSelf
            fromMine -> TransactionType.Outgoing
            else -> TransactionType.Incoming
        }

        val txHashHex = transaction.transactionHash.toHexString()
        return TransactionRecord(
                uid = "$txHashHex${transaction.interTransactionIndex}${contractAddress.hex}",
                transactionHash = txHashHex,
                transactionIndex = transaction.transactionIndex ?: 0,
                interTransactionIndex = transaction.interTransactionIndex,
                blockHeight = transaction.blockNumber,
                amount = scaleDown(transaction.value.toBigDecimal()),
                timestamp = transaction.timestamp,
                from = transaction.from.hex,
                to = transaction.to.hex,
                type = type,
                failed = transaction.isError
        )
    }

    fun allowance(spenderAddress: Address): Single<BigDecimal> {
        return erc20Kit.allowance(spenderAddress)
                .map {
                    scaleDown(it.toBigDecimal())
                }
    }

    fun approve(address: String, amount: BigDecimal, gasPrice: Long, gasLimit: Long): Single<Transaction> {
        return erc20Kit.approve(Address(address), scaleUp(amount), gasPrice, gasLimit)
    }

    fun estimateApprove(address: String, amount: BigDecimal, gasPrice: Long): Single<Long> {
        return erc20Kit.estimateApprove(Address(address), scaleUp(amount), gasPrice)
    }

    companion object {
        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) EthereumKit.NetworkType.Ropsten else EthereumKit.NetworkType.MainNet
            Erc20Kit.clear(App.instance, networkType, walletId)
        }
    }

}
