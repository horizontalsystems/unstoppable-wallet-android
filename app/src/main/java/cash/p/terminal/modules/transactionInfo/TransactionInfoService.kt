package cash.p.terminal.modules.transactionInfo

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.adapters.TonTransactionRecord
import cash.p.terminal.entities.nft.NftAssetBriefMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ApproveTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.ExternalContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.SwapTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.UnknownSwapTransactionRecord
import cash.p.terminal.entities.transactionrecords.nftUids
import cash.p.terminal.entities.transactionrecords.solana.SolanaIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.solana.SolanaOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.solana.SolanaUnknownTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronApproveTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronExternalContractCallTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronIncomingTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronOutgoingTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.NftMetadataService
import io.horizontalsystems.core.CurrencyManager
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.CurrencyValue
import cash.p.terminal.wallet.transaction.TransactionSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class TransactionInfoService(
    val transactionRecord: TransactionRecord,
    private val adapter: ITransactionsAdapter,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val nftMetadataService: NftMetadataService,
    balanceHidden: Boolean,
    transactionStatusUrl: Pair<String, String>?
) {

    val transactionHash: String get() = transactionRecord.transactionHash
    val source: TransactionSource get() = transactionRecord.source

    private val _transactionInfoItemFlow = MutableStateFlow<TransactionInfoItem?>(null)
    val transactionInfoItemFlow = _transactionInfoItemFlow.filterNotNull()

    var transactionInfoItem = TransactionInfoItem(
        record = transactionRecord,
        lastBlockInfo = adapter.lastBlockInfo,
        explorerData = TransactionInfoModule.ExplorerData(adapter.explorerTitle, adapter.getTransactionUrl(transactionRecord.transactionHash)),
        rates = mapOf(),
        nftMetadata = mapOf(),
        hideAmount = balanceHidden,
        transactionStatusUrl = transactionStatusUrl
    )
        private set(value) {
            field = value
            _transactionInfoItemFlow.update { value }
        }

    private val coinUidsForRates: List<String>
        get() {
            val coinUids = mutableListOf<String?>()

            val txCoinTypes = when (val tx = transactionRecord) {
                is TonTransactionRecord -> buildList {
                    add(tx.mainValue?.coinUid)
                    add(tx.fee.coinUid)

                    tx.actions.forEach { action ->
                        val actionType = action.type
                        when (actionType) {
                            is TonTransactionRecord.Action.Type.Burn -> {
                                add(actionType.value.coinUid)
                            }
                            is TonTransactionRecord.Action.Type.ContractCall -> {
                                add(actionType.value.coinUid)
                            }
                            is TonTransactionRecord.Action.Type.Mint -> {
                                add(actionType.value.coinUid)
                            }
                            is TonTransactionRecord.Action.Type.Receive -> {
                                add(actionType.value.coinUid)
                            }
                            is TonTransactionRecord.Action.Type.Send -> {
                                add(actionType.value.coinUid)
                            }
                            is TonTransactionRecord.Action.Type.Swap -> {
                                add(actionType.valueIn.coinUid)
                                add(actionType.valueOut.coinUid)
                            }
                            is TonTransactionRecord.Action.Type.ContractDeploy,
                            is TonTransactionRecord.Action.Type.Unsupported -> Unit
                        }
                    }
                }
                is EvmIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is EvmOutgoingTransactionRecord -> listOf(tx.fee?.coinUid, tx.value.coinUid)
                is SwapTransactionRecord -> listOf(tx.fee, tx.valueIn, tx.valueOut).map { it?.coinUid }
                is UnknownSwapTransactionRecord -> listOf(tx.fee, tx.valueIn, tx.valueOut).map { it?.coinUid }
                is ApproveTransactionRecord -> listOf(tx.fee?.coinUid, tx.value.coinUid)
                is ContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                is ExternalContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                is BitcoinIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is BitcoinOutgoingTransactionRecord -> listOf(tx.fee, tx.value).map { it?.coinUid }
                is BinanceChainIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is BinanceChainOutgoingTransactionRecord -> listOf(tx.fee, tx.value).map { it.coinUid }
                is SolanaIncomingTransactionRecord -> listOf(tx.value.coinUid)
                is SolanaOutgoingTransactionRecord -> listOf(tx.fee?.coinUid, tx.value.coinUid)
                is SolanaUnknownTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingTransfers.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingTransfers.map { it.value.coinUid })
                    tempCoinUidList
                }
                is TronOutgoingTransactionRecord -> {
                    listOf(tx.value.coinUid, tx.fee?.coinUid)
                }
                is TronIncomingTransactionRecord -> {
                    listOf(tx.value.coinUid)
                }
                is TronApproveTransactionRecord -> {
                    listOf(tx.value.coinUid, tx.fee?.coinUid)
                }
                is TronContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                is TronExternalContractCallTransactionRecord -> {
                    val tempCoinUidList = mutableListOf<String>()
                    tempCoinUidList.addAll(tx.incomingEvents.map { it.value.coinUid })
                    tempCoinUidList.addAll(tx.outgoingEvents.map { it.value.coinUid })
                    tempCoinUidList
                }
                else -> emptyList()
            }

            (transactionRecord as? EvmTransactionRecord)?.let { transactionRecord ->
                if (!transactionRecord.foreignTransaction) {
                    coinUids.add(transactionRecord.fee?.coinUid)
                }
            }

            (transactionRecord as? TronTransactionRecord)?.let { transactionRecord ->
                if (!transactionRecord.foreignTransaction) {
                    coinUids.add(transactionRecord.fee?.coinUid)
                }
            }

            coinUids.addAll(txCoinTypes)

            return coinUids.filterNotNull().filter { it.isNotBlank() }.distinct()
        }

    suspend fun start() = withContext(Dispatchers.IO) {
        _transactionInfoItemFlow.update { transactionInfoItem }

        launch {
            adapter.getTransactionRecordsFlowable(null, FilterTransactionType.All, null).asFlow()
                .collect { transactionRecords ->
                    val record = transactionRecords.find { it == transactionRecord }

                    if (record != null) {
                        handleRecordUpdate(record)
                    }
                }
        }

        launch {
            adapter.lastBlockUpdatedFlowable.asFlow()
                .collect {
                    handleLastBlockUpdate()
                }
        }

        launch {
            nftMetadataService.assetsBriefMetadataFlow.collect {
                handleNftMetadata(it)
            }
        }

        fetchRates()
        fetchNftMetadata()
    }

    private suspend fun fetchNftMetadata() {
        val nftUids = transactionRecord.nftUids
        val assetsBriefMetadata = nftMetadataService.assetsBriefMetadata(nftUids)

        handleNftMetadata(assetsBriefMetadata)

        if (nftUids.subtract(assetsBriefMetadata.keys).isNotEmpty()) {
            nftMetadataService.fetch(nftUids)
        }
    }

    private suspend fun fetchRates() = withContext(Dispatchers.IO) {
        val coinUids = coinUidsForRates
        val timestamp = transactionRecord.timestamp

        val rates = coinUids.mapNotNull { coinUid ->
            try {
                val rate = marketKit
                    .coinHistoricalPriceSingle(coinUid, currencyManager.baseCurrency.code, timestamp)
                    .await()
                if (rate != BigDecimal.ZERO) {
                    Pair(coinUid, CurrencyValue(currencyManager.baseCurrency, rate))
                } else {
                    null
                }
            } catch (error: Exception) {
                null
            }
        }.toMap()

        handleRates(rates)
    }

    @Synchronized
    private fun handleLastBlockUpdate() {
        transactionInfoItem = transactionInfoItem.copy(lastBlockInfo = adapter.lastBlockInfo)
    }

    @Synchronized
    private fun handleRecordUpdate(transactionRecord: TransactionRecord) {
        transactionInfoItem = transactionInfoItem.copy(record = transactionRecord)
    }

    @Synchronized
    private fun handleRates(rates: Map<String, CurrencyValue>) {
        transactionInfoItem = transactionInfoItem.copy(rates = rates)
    }

    @Synchronized
    private fun handleNftMetadata(nftMetadata: Map<NftUid, NftAssetBriefMetadata>) {
        transactionInfoItem = transactionInfoItem.copy(nftMetadata = nftMetadata)
    }

    fun getRawTransaction(): String? {
        return adapter.getRawTransaction(transactionRecord.transactionHash)
    }

}
