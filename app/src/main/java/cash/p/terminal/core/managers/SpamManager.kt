package cash.p.terminal.core.managers

import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.factories.TransferEventFactory
import cash.p.terminal.core.providers.AppConfigProvider
import cash.p.terminal.core.storage.SpamAddressStorage
import cash.p.terminal.entities.SpamAddress
import cash.p.terminal.entities.SpamScanState
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.evm.TransferEvent
import cash.p.terminal.entities.transactionrecords.ton.TonTransactionRecord
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.math.BigDecimal
import kotlin.coroutines.cancellation.CancellationException

class SpamManager(
    private val localStorage: ILocalStorage,
    private val spamAddressStorage: SpamAddressStorage,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val dispatcherProvider: DispatcherProvider,
) {
    private data class AdapterSubscription(
        val adapter: ITransactionsAdapter,
        val job: Job
    )

    private val transferEventFactory = TransferEventFactory()
    private val adapterSubscriptionsMutex = Mutex()
    private val adapterSubscriptions = mutableMapOf<TransactionSource, AdapterSubscription>()

    init {
        dispatcherProvider.applicationScope.launch {
            transactionAdapterManager.adaptersReadyFlow.collect(::subscribeToAdapters)
        }
    }

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    private suspend fun subscribeToAdapters(adapters: Map<TransactionSource, ITransactionsAdapter>) {
        adapterSubscriptionsMutex.withLock {
            val removedSources = adapterSubscriptions.keys - adapters.keys
            removedSources.forEach { source ->
                adapterSubscriptions.remove(source)?.job?.cancel()
            }

            adapters.forEach { (source, adapter) ->
                val subscription = adapterSubscriptions[source]
                if (subscription?.adapter === adapter) return@forEach

                subscription?.job?.cancel()
                adapterSubscriptions[source] = AdapterSubscription(
                    adapter = adapter,
                    job = subscribeToAdapter(source, adapter)
                )
            }
        }
    }

    private fun subscribeToAdapter(source: TransactionSource, adapter: ITransactionsAdapter): Job =
        dispatcherProvider.applicationScope.launch {
            adapter.transactionsStateUpdatedFlowable.asFlow().collect {
                sync(source)
            }
        }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun find(address: String): SpamAddress? {
        return spamAddressStorage.findByAddress(address)
    }

    fun shouldHide(record: TransactionRecord): Boolean {
        if (!hideSuspiciousTx) return false
        return record.spam || isZeroAmountTransfer(record)
    }

    companion object {

        fun isZeroAmountTransfer(record: TransactionRecord): Boolean {
            val isTransferType = when (record.transactionRecordType) {
                TransactionRecordType.BITCOIN_INCOMING,
                TransactionRecordType.BITCOIN_OUTGOING,
                TransactionRecordType.EVM_INCOMING,
                TransactionRecordType.EVM_OUTGOING,
                TransactionRecordType.TRON_INCOMING,
                TransactionRecordType.TRON_OUTGOING,
                TransactionRecordType.SOLANA_INCOMING,
                TransactionRecordType.SOLANA_OUTGOING,
                TransactionRecordType.MONERO_INCOMING,
                TransactionRecordType.MONERO_OUTGOING,
                TransactionRecordType.STELLAR_INCOMING,
                TransactionRecordType.STELLAR_OUTGOING -> true
                TransactionRecordType.TON -> isTonTransferLikeAction(record)
                else -> false
            }
            if (!isTransferType) return false
            return record.mainValue?.decimalValue?.signum() == 0
        }

        private fun isTonTransferLikeAction(record: TransactionRecord): Boolean {
            if (record !is TonTransactionRecord) return false
            val action = record.actions.singleOrNull() ?: return false
            return when (action.type) {
                is TonTransactionRecord.Action.Type.Send,
                is TonTransactionRecord.Action.Type.Receive,
                is TonTransactionRecord.Action.Type.Burn,
                is TonTransactionRecord.Action.Type.Mint -> true
                is TonTransactionRecord.Action.Type.ContractCall,
                is TonTransactionRecord.Action.Type.ContractDeploy,
                is TonTransactionRecord.Action.Type.Swap,
                is TonTransactionRecord.Action.Type.Unsupported -> false
            }
        }

        private fun handleSpamAddresses(events: List<TransferEvent>): List<String> {
            val spamTokenSenders = mutableListOf<String>()
            val nativeSenders = mutableListOf<String>()
            var totalNativeTransactionValue: TransactionValue? = null

            events.forEach { event ->
                if (event.value is TransactionValue.CoinValue && event.value.token.type == TokenType.Native) {
                    val totalNativeValue =
                        totalNativeTransactionValue?.decimalValue ?: BigDecimal.ZERO
                    totalNativeTransactionValue = TransactionValue.CoinValue(
                        event.value.token,
                        event.value.value + totalNativeValue
                    )
                    event.address?.let { nativeSenders.add(it) }
                } else {
                    if (event.address != null && isSpam(event.value)) {
                        spamTokenSenders.add(event.address)
                    }
                }
            }

            if (totalNativeTransactionValue != null && isSpam(totalNativeTransactionValue) && nativeSenders.isNotEmpty()) {
                spamTokenSenders.addAll(nativeSenders)
            }

            return spamTokenSenders
        }

        private fun isSpam(transactionValue: TransactionValue): Boolean {
            val spamCoinLimits = AppConfigProvider.spamCoinValueLimits
            val value = transactionValue.decimalValue?.abs()

            var limit: BigDecimal = BigDecimal.ZERO
            when (transactionValue) {
                is TransactionValue.CoinValue -> {
                    if (value?.signum() == 0) return true
                    limit = spamCoinLimits[transactionValue.coinCode] ?: BigDecimal.ZERO
                }

                is TransactionValue.JettonValue -> {
                    if (value?.signum() == 0) return true
                    limit = spamCoinLimits[transactionValue.coinCode] ?: BigDecimal.ZERO
                }

                is TransactionValue.NftValue -> {
                    if (transactionValue.value > BigDecimal.ZERO)
                        return false
                }

                is TransactionValue.RawValue,
                is TransactionValue.TokenValue -> {
                    return true
                }
            }

            return limit > value
        }

        fun isSpam(events: List<TransferEvent>): Boolean {
            return handleSpamAddresses(events).isNotEmpty()
        }
    }

    private suspend fun sync(source: TransactionSource) {
        withContext(dispatcherProvider.io) {
            try {
                val adapter = transactionAdapterManager.getAdapter(source) ?: return@withContext
                val spamScanState =
                    spamAddressStorage.getSpamScanState(source.blockchain.type, source.account.id)
                val transactions = adapter.getTransactionsAfter(spamScanState?.lastSyncedTransactionId)
                val lastSyncedTransactionId = handle(transactions, source)
                lastSyncedTransactionId?.let {
                    spamAddressStorage.save(
                        SpamScanState(
                            source.blockchain.type,
                            source.account.id,
                            lastSyncedTransactionId
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.d(e, "SpamManager sync error")
            }
        }
    }

    private fun handle(transactions: List<TransactionRecord>, source: TransactionSource): String? {
        val txWithEvents =
            transactions.map { Pair(it.transactionHash, transferEventFactory.transferEvents(it)) }

        val spamAddresses = mutableListOf<SpamAddress>()

        txWithEvents.forEach { (hash, events) ->
            val hashByteArray = hash.hexStringToByteArrayOrNull() ?: return@forEach
            if (events.isEmpty()) return@forEach

            val result = handleSpamAddresses(events)
            if (result.isNotEmpty()) {
                result.forEach { address ->
                    spamAddresses.add(
                        SpamAddress(
                            hashByteArray,
                            address,
                            null,
                            source.blockchain.type
                        )
                    )
                }
            }
        }

        try {
            spamAddressStorage.save(spamAddresses)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }

        val sortedTransactions = transactions.sortedWith(
            compareBy<TransactionRecord> { it.timestamp }
                .thenBy { it.transactionIndex }
                .thenBy { it.transactionHash }
        )

        return sortedTransactions.lastOrNull()?.transactionHash
    }
}
