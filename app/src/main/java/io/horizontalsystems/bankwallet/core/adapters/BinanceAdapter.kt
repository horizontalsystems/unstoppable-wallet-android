package io.horizontalsystems.bankwallet.core.adapters

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.*
import io.horizontalsystems.bankwallet.entities.LastBlockInfo
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.entities.TransactionType
import io.horizontalsystems.bankwallet.entities.Wallet
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceError
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import java.math.BigDecimal

class BinanceAdapter(
        private val binanceKit: BinanceChainKit,
        private val symbol: String)
    : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter, ISendBinanceAdapter {

    private val asset = binanceKit.register(symbol)

    private val syncState: AdapterState
        get() = when (val kitSyncState = binanceKit.syncState) {
            BinanceChainKit.SyncState.Synced -> AdapterState.Synced
            BinanceChainKit.SyncState.Syncing -> AdapterState.Syncing(50, null)
            is BinanceChainKit.SyncState.NotSynced -> AdapterState.NotSynced(kitSyncState.error)
        }

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

    override fun getReceiveAddressType(wallet: Wallet): String? = null

    override val debugInfo: String
        get() = ""

    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.syncStateFlowable.map { }

    override val balance: BigDecimal
        get() = asset.balance

    override val balanceUpdatedFlowable: Flowable<Unit>
        get() = asset.balanceFlowable.map { }

    // ITransactionsAdapter

    override val transactionsState: AdapterState
        get() = syncState

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.syncStateFlowable.map { }

    override val lastBlockInfo: LastBlockInfo?
        get() = binanceKit.latestBlock?.height?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.latestBlockFlowable.map { }

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
                confirmationsThreshold = confirmationsThreshold,
                amount = transaction.amount.toBigDecimal(),
                fee = transferFee,
                timestamp = transaction.date.time / 1000,
                from = transaction.from,
                memo = transaction.memo,
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

    override fun send(amount: BigDecimal, address: String, memo: String?, logger: AppLogger): Single<Unit> {
        return binanceKit.send(symbol, address, amount, memo ?: "")
                .doOnSubscribe {
                    logger.info("call binanceKit.send")
                }
                .onErrorResumeNext { Single.error(getException(it)) }
                .map { Unit }
    }

    private fun getException(error: Throwable): Exception {
        when (error) {
            is BinanceError -> {
                if (error.message.contains("receiver requires non-empty memo in transfer transaction")) {
                    return LocalizedException(R.string.Binance_Backend_Error_MemoRequired)
                } else if (error.message.contains("requires the memo contains only digits")) {
                    return LocalizedException(R.string.Binance_Backend_Error_RequiresDigits)
                }
            }
        }
        return Exception(error.message)
    }

    override fun validate(address: String) {
        binanceKit.validateAddress(address)
    }

    // IReceiveAdapter

    override val receiveAddress: String
        get() = binanceKit.receiveAddress()


    companion object {
        private const val confirmationsThreshold = 1
        val transferFee = BigDecimal.valueOf(0.000375)

        fun clear(walletId: String, testMode: Boolean) {
            val networkType = if (testMode) BinanceChainKit.NetworkType.TestNet else BinanceChainKit.NetworkType.MainNet
            BinanceChainKit.clear(App.instance, networkType, walletId)
        }

    }
}
