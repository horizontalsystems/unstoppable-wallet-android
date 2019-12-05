package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

class BinanceAdapter(
        private val binanceKit: BinanceChainKit,
        private val symbol: String)
    : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter, ISendBinanceAdapter {

    private val asset = binanceKit.register(symbol)

    // IAdapter

    override fun start() {
        // handled by BinanceKitManager
    }

    override fun stop() {
        binanceKit.unregister(asset)
    }

    override fun refresh() {
        // handled by BinanceKitManager
    }

    override val debugInfo: String
        get() = ""

    // IBalanceAdapter

    override val state: AdapterState
        get() = when (binanceKit.syncState) {
            BinanceChainKit.SyncState.Synced -> AdapterState.Synced
            BinanceChainKit.SyncState.NotSynced -> AdapterState.NotSynced
            BinanceChainKit.SyncState.Syncing -> AdapterState.Syncing(50, null)
        }

    override val stateUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.syncStateFlowable.map { Unit }

    override val balance: BigDecimal
        get() = asset.balance

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = asset.balanceFlowable.map { Unit }

    // ITransactionsAdapter

    override val confirmationsThreshold: Int
        get() = 1

    override val lastBlockHeight: Int?
        get() = binanceKit.latestBlock?.height

    override val lastBlockHeightUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.latestBlockFlowable.map { Unit }

    override val transactionRecordsFlowable: Flowable<List<TransactionRecord>>
        get() = asset.transactionsFlowable.map { it.map { tx -> transactionRecord(tx) } }

    override fun getTransactions(from: TransactionRecord?, limit: Int): Single<List<TransactionRecord>> {
        return binanceKit.transactions(asset, from?.transactionHash, limit).map { list ->
            list.map { transactionRecord(it) }
        }
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val myAddress = binanceKit.receiveAddress()
        val fromMine = transaction.from == myAddress
        val toMine = transaction.to == myAddress

        val type = when {
            fromMine && toMine -> TransactionType.SentToSelf
            fromMine -> TransactionType.Outgoing
            else -> TransactionType.Incoming
        }

        return TransactionRecord(
                uid = transaction.hash,
                transactionHash = transaction.hash,
                transactionIndex = 0,
                interTransactionIndex = 0,
                blockHeight = transaction.blockNumber.toLong(),
                amount = transaction.amount.toBigDecimal(),
                fee = transferFee,
                timestamp = transaction.date.time / 1000,
                from = transaction.from,
                to = transaction.to,
                type = type
        )
    }

    // ISendBinanceAdapter

    override val availableBalance: BigDecimal
        get() {
            var availableBalance = asset.balance
            if (asset.symbol == "BNB") {
                availableBalance -= transferFee
            }
            return if (availableBalance < BigDecimal.ZERO) BigDecimal.ZERO else availableBalance
        }

    override val availableBinanceBalance: BigDecimal
        get() = binanceKit.binanceBalance

    override val fee: BigDecimal
        get() = transferFee

    override fun send(amount: BigDecimal, address: String, memo: String?): Single<Unit> {
        return binanceKit.send(symbol, address, amount, memo ?: "").map { Unit }
    }

    override fun validate(address: String) {
        binanceKit.validateAddress(address)
    }

    // IReceiveAdapter

    override val receiveAddress: String
        get() = binanceKit.receiveAddress()


    companion object {
        val transferFee = BigDecimal(0.000375)

        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) BinanceChainKit.NetworkType.TestNet else BinanceChainKit.NetworkType.MainNet
            BinanceChainKit.clear(App.instance, networkType, walletId)
        }

    }
}
