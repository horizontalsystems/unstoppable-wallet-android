package cash.p.terminal.core.adapters

import cash.p.terminal.R
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.core.App
import io.horizontalsystems.core.logger.AppLogger
import cash.p.terminal.wallet.entities.BalanceData
import cash.p.terminal.wallet.IAdapter
import cash.p.terminal.wallet.IBalanceAdapter
import cash.p.terminal.wallet.IReceiveAdapter
import cash.p.terminal.core.ISendBinanceAdapter
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.UnsupportedFilterException
import cash.p.terminal.entities.LastBlockInfo
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.binancechainkit.BinanceChainKit
import io.horizontalsystems.binancechainkit.core.api.BinanceError
import io.horizontalsystems.binancechainkit.models.TransactionFilterType
import io.horizontalsystems.binancechainkit.models.TransactionInfo
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal

class BinanceAdapter(
    private val binanceKit: BinanceChainKit,
    private val symbol: String,
    private val feeToken: Token,
    private val wallet: Wallet,
) : IAdapter, ITransactionsAdapter, IBalanceAdapter, IReceiveAdapter, ISendBinanceAdapter {

    private val asset = binanceKit.register(symbol)
    private val token = wallet.token

    private val syncState: AdapterState
        get() = when (val kitSyncState = binanceKit.syncState) {
            BinanceChainKit.SyncState.Synced -> AdapterState.Synced
            BinanceChainKit.SyncState.Syncing -> AdapterState.Syncing()
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

    override val debugInfo: String
        get() = ""


    // IBalanceAdapter

    override val balanceState: AdapterState
        get() = syncState

    override val balanceStateUpdatedFlow: Flow<Unit>
        get() = binanceKit.syncStateFlowable.map { }.asFlow()

    override val balanceData: BalanceData
        get() = BalanceData(asset.balance)

    override val balanceUpdatedFlow: Flow<Unit>
        get() = asset.balanceFlowable.map { }.asFlow()

    // ITransactionsAdapter

    override val explorerTitle: String =
        "binance.org"

    override val transactionsState: AdapterState
        get() = syncState

    override val transactionsStateUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.syncStateFlowable.map { }

    override val lastBlockInfo: LastBlockInfo?
        get() = binanceKit.latestBlock?.height?.let { LastBlockInfo(it) }

    override val lastBlockUpdatedFlowable: Flowable<Unit>
        get() = binanceKit.latestBlockFlowable.map { }

    override fun getTransactionRecordsFlowable(
        token: Token?,
        transactionType: FilterTransactionType,
        address: String?,
    ): Flowable<List<TransactionRecord>> = when (address) {
        null -> getTransactionRecordsFlowable(transactionType)
        else -> Flowable.empty()
    }

    private fun getTransactionRecordsFlowable(transactionType: FilterTransactionType): Flowable<List<TransactionRecord>> {
        return try {
            val filter = getBinanceTransactionTypeFilter(transactionType)
            asset.getTransactionsFlowable(filter).map { it.map { transactionRecord(it) } }
        } catch (e: UnsupportedFilterException) {
            Flowable.empty()
        }
    }

    override fun getTransactionsAsync(
        from: TransactionRecord?,
        token: Token?,
        limit: Int,
        transactionType: FilterTransactionType,
        address: String?,
    ) = when (address) {
        null -> getTransactionsAsync(from, limit, transactionType)
        else -> Single.just(listOf())
    }

    private fun getTransactionsAsync(
        from: TransactionRecord?,
        limit: Int,
        transactionType: FilterTransactionType
    ): Single<List<TransactionRecord>> {
        return try {
            val filter = getBinanceTransactionTypeFilter(transactionType)
            binanceKit
                .transactions(asset, filter, from?.transactionHash, limit)
                .map { it.map { transactionRecord(it) } }
        } catch (e: UnsupportedFilterException) {
            Single.just(listOf())
        }
    }

    private fun getBinanceTransactionTypeFilter(transactionType: FilterTransactionType): TransactionFilterType? {
        return when (transactionType) {
            FilterTransactionType.All -> null
            FilterTransactionType.Incoming -> TransactionFilterType.Incoming
            FilterTransactionType.Outgoing -> TransactionFilterType.Outgoing
            else -> throw UnsupportedFilterException()
        }
    }

    private fun transactionRecord(transaction: TransactionInfo): TransactionRecord {
        val myAddress = binanceKit.receiveAddress()
        val fromMine = transaction.from == myAddress
        val toMine = transaction.to == myAddress

        return when {
            fromMine && !toMine -> BinanceChainOutgoingTransactionRecord(
                transaction,
                feeToken,
                token,
                false,
                wallet.transactionSource
            )
            !fromMine && toMine -> BinanceChainIncomingTransactionRecord(
                transaction,
                feeToken,
                token,
                wallet.transactionSource
            )
            else -> BinanceChainOutgoingTransactionRecord(
                transaction,
                feeToken,
                token,
                true,
                wallet.transactionSource
            )
        }
    }

    override fun getTransactionUrl(transactionHash: String): String =
        "https://explorer.binance.org/tx/$transactionHash"

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

    override fun send(
        amount: BigDecimal,
        address: String,
        memo: String?,
        logger: AppLogger
    ): Single<String> {
        return binanceKit.send(symbol, address, amount, memo ?: "")
            .doOnSubscribe {
                logger.info("call binanceKit.send")
            }
            .onErrorResumeNext { Single.error(getException(it)) }
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

    override val isMainNet: Boolean = true

    companion object {
        const val confirmationsThreshold = 1
        val transferFee = BigDecimal.valueOf(0.000075)

        fun clear(walletId: String) {
            val networkType = BinanceChainKit.NetworkType.MainNet
            BinanceChainKit.clear(App.instance, networkType, walletId)
        }

    }
}
