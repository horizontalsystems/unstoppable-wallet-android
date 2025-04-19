package cash.p.terminal.modules.transactionInfo

import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.core.usecase.UpdateChangeNowStatusesUseCase
import cash.p.terminal.entities.nft.NftAssetBriefMetadata
import cash.p.terminal.entities.nft.NftUid
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.binancechain.BinanceChainTransactionRecord
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.entities.transactionrecords.evm.EvmTransactionRecord
import cash.p.terminal.entities.transactionrecords.nftUids
import cash.p.terminal.entities.transactionrecords.solana.SolanaTransactionRecord
import cash.p.terminal.entities.transactionrecords.tron.TronTransactionRecord
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.NftMetadataService
import cash.p.terminal.modules.transactions.TransactionStatus
import cash.p.terminal.modules.transactions.toUniversalStatus
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.CurrencyValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class TransactionInfoService(
    val transactionRecord: TransactionRecord,
    private val changeNowTransactionId: String?,
    private val adapter: ITransactionsAdapter,
    private val marketKit: MarketKitWrapper,
    private val currencyManager: CurrencyManager,
    private val nftMetadataService: NftMetadataService,
    private val updateChangeNowStatusesUseCase: UpdateChangeNowStatusesUseCase,
    balanceHidden: Boolean,
    transactionStatusUrl: Pair<String, String>?
) {

    val transactionHash: String get() = transactionRecord.transactionHash
    val source: TransactionSource get() = transactionRecord.source

    private val _transactionInfoItemFlow = MutableStateFlow<TransactionInfoItem?>(null)
    val transactionInfoItemFlow = _transactionInfoItemFlow.filterNotNull()

    var transactionInfoItem = TransactionInfoItem(
        record = transactionRecord,
        externalStatus = TransactionStatus.Pending,
        lastBlockInfo = adapter.lastBlockInfo,
        explorerData = TransactionInfoModule.ExplorerData(
            adapter.explorerTitle,
            adapter.getTransactionUrl(transactionRecord.transactionHash)
        ),
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

                is EvmTransactionRecord -> {
                    when (transactionRecord.transactionRecordType) {
                        TransactionRecordType.EVM_INCOMING -> {
                            listOf(tx.value!!.coinUid)
                        }

                        TransactionRecordType.EVM_APPROVE,
                        TransactionRecordType.EVM_OUTGOING -> {
                            listOf(tx.fee?.coinUid, tx.value!!.coinUid)
                        }

                        TransactionRecordType.EVM_UNKNOWN_SWAP,
                        TransactionRecordType.EVM_SWAP -> {
                            listOf(
                                tx.fee,
                                tx.valueIn,
                                tx.valueOut
                            ).map { it?.coinUid }
                        }

                        TransactionRecordType.EVM_CONTRACT_CALL,
                        TransactionRecordType.EVM_EXTERNAL_CONTRACT_CALL -> {
                            val tempCoinUidList = mutableListOf<String>()
                            tempCoinUidList.addAll(tx.incomingEvents!!.map { it.value.coinUid })
                            tempCoinUidList.addAll(tx.outgoingEvents!!.map { it.value.coinUid })
                            tempCoinUidList
                        }

                        else -> emptyList()
                    }
                }

                is BitcoinTransactionRecord -> {
                    when (transactionRecord.transactionRecordType) {
                        TransactionRecordType.BITCOIN_INCOMING -> {
                            listOf(tx.mainValue.coinUid)
                        }

                        TransactionRecordType.BITCOIN_OUTGOING -> {
                            listOf(
                                tx.fee,
                                tx.mainValue
                            ).map { it?.coinUid }
                        }

                        else -> emptyList()
                    }
                }

                is BinanceChainTransactionRecord -> {
                    when (transactionRecord.transactionRecordType) {
                        TransactionRecordType.BINANCE_INCOMING -> {
                            listOf(tx.mainValue.coinUid)
                        }

                        TransactionRecordType.BINANCE_OUTGOING -> {
                            listOf(
                                tx.fee,
                                tx.mainValue
                            ).map { it.coinUid }
                        }

                        else -> emptyList()
                    }
                }

                is SolanaTransactionRecord -> {
                    when (transactionRecord.transactionRecordType) {
                        TransactionRecordType.SOLANA_INCOMING -> {
                            listOf(tx.mainValue?.coinUid)
                        }

                        TransactionRecordType.SOLANA_OUTGOING -> {
                            listOf(tx.fee?.coinUid, tx.mainValue?.coinUid)
                        }

                        TransactionRecordType.SOLANA_UNKNOWN -> {
                            val tempCoinUidList = mutableListOf<String>()
                            tx.incomingSolanaTransfers?.map { it.value.coinUid }
                                ?.let { tempCoinUidList.addAll(it) }
                            tx.outgoingSolanaTransfers?.map { it.value.coinUid }
                                ?.let { tempCoinUidList.addAll(it) }
                            tempCoinUidList
                        }

                        else -> emptyList()
                    }
                }

                is TronTransactionRecord -> {
                    when (transactionRecord.transactionRecordType) {
                        TransactionRecordType.TRON_APPROVE,
                        TransactionRecordType.TRON_OUTGOING ->
                            listOf(tx.value?.coinUid, tx.fee?.coinUid)

                        TransactionRecordType.TRON_INCOMING ->
                            listOf(tx.value?.coinUid)

                        TransactionRecordType.TRON_CONTRACT_CALL,
                        TransactionRecordType.TRON_EXTERNAL_CONTRACT_CALL -> {
                            val tempCoinUidList = mutableListOf<String>()
                            tempCoinUidList.addAll(tx.incomingEvents!!.map { it.value.coinUid })
                            tempCoinUidList.addAll(tx.outgoingEvents!!.map { it.value.coinUid })
                            tempCoinUidList
                        }

                        else -> emptyList()
                    }
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
        handleLastBlockUpdate(getChangeNowTransactionStatus())
        _transactionInfoItemFlow.update { transactionInfoItem }

        launch {
            adapter.getTransactionRecordsFlow(null, FilterTransactionType.All, null)
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
                    handleLastBlockUpdate(getChangeNowTransactionStatus())
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

    private suspend fun getChangeNowTransactionStatus(): TransactionStatus? =
        changeNowTransactionId?.let { changeNowTransactionId ->
            updateChangeNowStatusesUseCase.updateTransactionStatus(changeNowTransactionId)
                .toUniversalStatus()
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
                marketKit
                    .coinHistoricalPriceSingle(
                        coinUid,
                        currencyManager.baseCurrency.code,
                        timestamp
                    ).takeIf { it != BigDecimal.ZERO }?.let {
                        Pair(coinUid, CurrencyValue(currencyManager.baseCurrency, it))
                    }
            } catch (error: Exception) {
                null
            }
        }.toMap()

        handleRates(rates)
    }

    @Synchronized
    private fun handleLastBlockUpdate(externalStatus: TransactionStatus?) {
        transactionInfoItem = transactionInfoItem.copy(
            lastBlockInfo = adapter.lastBlockInfo,
            externalStatus = externalStatus
        )
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
