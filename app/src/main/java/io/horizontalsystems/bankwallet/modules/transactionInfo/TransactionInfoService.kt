package io.horizontalsystems.bankwallet.modules.transactionInfo

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.managers.AccountSettingManager
import io.horizontalsystems.bankwallet.core.subscribeIO
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.*
import io.horizontalsystems.bankwallet.modules.transactions.FilterTransactionType
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.core.ICurrencyManager
import io.horizontalsystems.ethereumkit.core.EthereumKit
import io.horizontalsystems.marketkit.MarketKit
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.BehaviorSubject
import java.math.BigDecimal

class TransactionInfoService(
    transactionRecord: TransactionRecord,
    private val adapter: ITransactionsAdapter,
    private val marketKit: MarketKit,
    private val currencyManager: ICurrencyManager,
    private val testMode: Boolean,
    private val accountSettingManager: AccountSettingManager
) : Clearable {

    val transactionHash: String get() = transactionInfoItem.record.transactionHash
    val source: TransactionSource get() = transactionInfoItem.record.source

    private val disposables = CompositeDisposable()

    private val transactionInfoItemSubject = BehaviorSubject.create<TransactionInfoItem>()
    val transactionInfoItemObservable: Observable<TransactionInfoItem> = transactionInfoItemSubject

    private var transactionInfoItem = TransactionInfoItem(transactionRecord, adapter.lastBlockInfo, getExplorerData(transactionRecord), mapOf())

    private val coinUidsForRates: List<String>
        get() {
            val coinUids = mutableListOf<String>()

            val txCoinTypes = when (val tx = transactionInfoItem.record) {
                is EvmIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is EvmOutgoingTransactionRecord -> listOf(tx.fee.coinUid, tx.value.coinUid)
                is SwapTransactionRecord -> listOf(
                    tx.fee,
                    tx.valueIn,
                    tx.valueOut
                ).mapNotNull { it?.coinUid }
                is ApproveTransactionRecord -> listOf(tx.fee.coinUid, tx.value.coinUid)
                is ContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    if (tx.value.value != BigDecimal.ZERO) {
                        tempCoinUidList.add(tx.value.coinUid)
                    }
                    tempCoinUidList.addAll(tx.incomingInternalETHs.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.incomingEip20Events.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEip20Events.map { it.value.coinUid })
                    tempCoinUidList
                }
                is BitcoinIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is BitcoinOutgoingTransactionRecord -> listOf(
                    tx.fee,
                    tx.value
                ).mapNotNull { it?.coinUid }
                is BinanceChainIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is BinanceChainOutgoingTransactionRecord -> listOf(
                    tx.fee,
                    tx.value
                ).map { it.coinUid }
                else -> emptyList()
            }

            (transactionInfoItem.record as? EvmTransactionRecord)?.let { transactionRecord ->
                if (!transactionRecord.foreignTransaction) {
                    coinUids.add(transactionRecord.fee.coinUid)
                }
            }

            coinUids.addAll(txCoinTypes)

            return coinUids.filter { it.isNotBlank() }.distinct()
        }

    init {
        transactionInfoItemSubject.onNext(transactionInfoItem)

        fetchRates()
            .subscribeIO {
                handleRates(it)
            }
            .let {
                disposables.add(it)
            }

        adapter.getTransactionRecordsFlowable(null, FilterTransactionType.All)
            .flatMap {
                val record = it.find { it == transactionInfoItem.record }
                if (record != null) {
                    Flowable.just(record)
                } else {
                    Flowable.empty()
                }
            }
            .subscribeIO {
                handleRecordUpdate(it)
            }
            .let {
                disposables.add(it)
            }

        adapter.lastBlockUpdatedFlowable
            .subscribeIO {
                handleLastBlockUpdate()
            }
            .let {
                disposables.add(it)
            }
    }

    private fun fetchRates(): Single<Map<String, CurrencyValue>> {
        val coinUids = coinUidsForRates
        val timestamp = transactionInfoItem.record.timestamp
        val flowables: List<Single<Pair<String, CurrencyValue>>> = coinUids.map { coinUid ->
            marketKit.coinHistoricalPriceSingle(coinUid, currencyManager.baseCurrency.code, timestamp)
                .onErrorResumeNext(Single.just(BigDecimal.ZERO)) //provide default value on error
                .map {
                    Pair(coinUid, CurrencyValue(currencyManager.baseCurrency, it))
                }
        }

        return Single
            .zip(flowables) { array ->
                array.mapNotNull {
                    it as Pair<String, CurrencyValue>

                    if (it.second.value == BigDecimal.ZERO) {
                        null
                    } else {
                        it.first to it.second
                    }
                }.toMap()
            }
    }

    @Synchronized
    private fun handleLastBlockUpdate() {
        transactionInfoItem = transactionInfoItem.copy(lastBlockInfo = adapter.lastBlockInfo)
        transactionInfoItemSubject.onNext(transactionInfoItem)
    }

    @Synchronized
    private fun handleRecordUpdate(transactionRecord: TransactionRecord) {
        transactionInfoItem = transactionInfoItem.copy(record = transactionRecord)
        transactionInfoItemSubject.onNext(transactionInfoItem)
    }

    @Synchronized
    private fun handleRates(rates: Map<String, CurrencyValue>) {
        transactionInfoItem = transactionInfoItem.copy(rates = rates)
        transactionInfoItemSubject.onNext(transactionInfoItem)
    }

    fun getRaw(): String? {
        return adapter.getRawTransaction(transactionInfoItem.record.transactionHash)
    }

    override fun clear() {
        disposables.clear()
    }

    private fun ethereumNetworkType(account: Account): EthereumKit.NetworkType {
        return accountSettingManager.ethereumNetwork(account).networkType
    }

    private fun getExplorerData(record: TransactionRecord): TransactionInfoModule.ExplorerData {
        val hash = record.transactionHash
        val blockchain = record.source.blockchain
        val account = record.source.account

        return when (blockchain) {
            is TransactionSource.Blockchain.Bitcoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/bitcoin/transaction/$hash"
            )
            is TransactionSource.Blockchain.BitcoinCash -> TransactionInfoModule.ExplorerData(
                "btc.com",
                if (testMode) null else "https://bch.btc.com/$hash"
            )
            is TransactionSource.Blockchain.Litecoin -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/litecoin/transaction/$hash"
            )
            is TransactionSource.Blockchain.Dash -> TransactionInfoModule.ExplorerData(
                "dash.org",
                if (testMode) null else "https://insight.dash.org/insight/tx/$hash"
            )
            is TransactionSource.Blockchain.Ethereum -> {
                val domain = when (ethereumNetworkType(account)) {
                    EthereumKit.NetworkType.EthMainNet -> "etherscan.io"
                    EthereumKit.NetworkType.EthRopsten -> "ropsten.etherscan.io"
                    EthereumKit.NetworkType.EthKovan -> "kovan.etherscan.io"
                    EthereumKit.NetworkType.EthRinkeby -> "rinkeby.etherscan.io"
                    EthereumKit.NetworkType.EthGoerli -> "goerli.etherscan.io"
                    EthereumKit.NetworkType.BscMainNet -> throw IllegalArgumentException("")
                }
                TransactionInfoModule.ExplorerData("etherscan.io", "https://$domain/tx/0x$hash")
            }
            is TransactionSource.Blockchain.Bep2 -> TransactionInfoModule.ExplorerData(
                "binance.org",
                if (testMode) "https://testnet-explorer.binance.org/tx/$hash" else "https://explorer.binance.org/tx/$hash"
            )
            is TransactionSource.Blockchain.BinanceSmartChain -> TransactionInfoModule.ExplorerData(
                "bscscan.com",
                "https://bscscan.com/tx/0x$hash"
            )
            is TransactionSource.Blockchain.Zcash -> TransactionInfoModule.ExplorerData(
                "blockchair.com",
                if (testMode) null else "https://blockchair.com/zcash/transaction/$hash"
            )
        }
    }

}
