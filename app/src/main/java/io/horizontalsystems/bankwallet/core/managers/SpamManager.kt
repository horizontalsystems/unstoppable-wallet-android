package io.horizontalsystems.bankwallet.core.managers

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.core.ITransactionsAdapter
import io.horizontalsystems.bankwallet.core.factories.TransferEventFactory
import io.horizontalsystems.bankwallet.core.storage.SpamAddressStorage
import io.horizontalsystems.bankwallet.entities.SpamAddress
import io.horizontalsystems.bankwallet.entities.SpamScanState
import io.horizontalsystems.bankwallet.entities.TransactionValue
import io.horizontalsystems.bankwallet.entities.transactionrecords.TransactionRecord
import io.horizontalsystems.bankwallet.entities.transactionrecords.evm.TransferEvent
import io.horizontalsystems.bankwallet.modules.transactions.TransactionSource
import io.horizontalsystems.ethereumkit.core.hexStringToByteArrayOrNull
import io.horizontalsystems.marketkit.models.TokenType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import java.math.BigDecimal
import java.util.concurrent.Executors

class SpamManager(
    private val localStorage: ILocalStorage,
    private val spamAddressStorage: SpamAddressStorage
) {
    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    private val singleDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val singleDispatcherCoroutineScope = CoroutineScope(singleDispatcher)
    private var transactionAdapterManager: TransactionAdapterManager? = null
    private val transferEventFactory = TransferEventFactory()

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    fun set(transactionAdapterManager: TransactionAdapterManager) {
        this.transactionAdapterManager = transactionAdapterManager

        coroutineScope.launch {
            transactionAdapterManager.adaptersReadyFlow.collect {
                subscribeToAdapters(transactionAdapterManager)
            }
        }
    }

    private fun subscribeToAdapters(transactionAdapterManager: TransactionAdapterManager) {
        transactionAdapterManager.adaptersMap.forEach { (transactionSource, transactionsAdapter) ->
            subscribeToAdapter(transactionSource, transactionsAdapter)
        }
    }

    private fun subscribeToAdapter(source: TransactionSource, adapter: ITransactionsAdapter) {
        coroutineScope.launch {
            adapter.transactionsStateUpdatedFlowable.asFlow().collect {
                sync(source)
            }
        }
    }

    private fun sync(source: TransactionSource) {
        singleDispatcherCoroutineScope.launch {
            val adapter = transactionAdapterManager?.getAdapter(source) ?: run {
                return@launch
            }
            val spamScanState = spamAddressStorage.getSpamScanState(source.blockchain.type, source.account.id)
            val transactions = adapter.getTransactionsAfter(spamScanState?.lastSyncedTransactionId).blockingGet()
            val lastSyncedTransactionId = handle(transactions, source)
            lastSyncedTransactionId?.let {
                spamAddressStorage.save(SpamScanState(source.blockchain.type, source.account.id, lastSyncedTransactionId))
            }
        }
    }

    private fun handle(transactions: List<TransactionRecord>, source: TransactionSource): String? {
        val txWithEvents = transactions.map { Pair(it.transactionHash, transferEventFactory.transferEvents(it)) }

        val spamAddresses = mutableListOf<SpamAddress>()

        txWithEvents.forEach { (hash, events) ->
            val hashByteArray = hash.hexStringToByteArrayOrNull() ?: return@forEach
            if (events.isEmpty()) return@forEach

            val result = handleSpamAddresses(events)
            if (result.isNotEmpty()) {
                result.forEach { address ->
                    spamAddresses.add(SpamAddress(hashByteArray, address, null, source.blockchain.type))
                }
            }
        }

        try {
            spamAddressStorage.save(spamAddresses)
        } catch (_: Throwable) {
        }

        val sortedTransactions = transactions.sortedWith(
            compareBy<TransactionRecord> { it.timestamp }
                .thenBy { it.transactionIndex }
                .thenBy { it.transactionHash }
        )

        return sortedTransactions.lastOrNull()?.transactionHash
    }

    fun updateFilterHideSuspiciousTx(hide: Boolean) {
        localStorage.hideSuspiciousTransactions = hide
        hideSuspiciousTx = hide
    }

    fun find(address: String): SpamAddress? {
        return spamAddressStorage.findByAddress(address)
    }

    companion object {

        private fun handleSpamAddresses(events: List<TransferEvent>): List<String> {
            val spamTokenSenders = mutableListOf<String>()
            val nativeSenders = mutableListOf<String>()
            var totalNativeTransactionValue: TransactionValue? = null

            events.forEach { event ->
                if (event.value is TransactionValue.CoinValue && event.value.token.type == TokenType.Native) {
                    val totalNativeValue = totalNativeTransactionValue?.decimalValue ?: BigDecimal.ZERO
                    totalNativeTransactionValue = TransactionValue.CoinValue(event.value.token, event.value.value + totalNativeValue)
                    event.address?.let { nativeSenders.add(it) }
                } else {
                    if (event.address != null && isSpam(event.value)) {
                        spamTokenSenders.add(event.address)
                    }
                }
            }

            if (totalNativeTransactionValue != null && isSpam(totalNativeTransactionValue!!) && nativeSenders.isNotEmpty()) {
                spamTokenSenders.addAll(nativeSenders)
            }

            return spamTokenSenders
        }

        private fun isSpam(transactionValue: TransactionValue): Boolean {
            val spamCoinLimits = App.appConfigProvider.spamCoinValueLimits
            val value = transactionValue.decimalValue

            var limit: BigDecimal = BigDecimal.ZERO
            when (transactionValue) {
                is TransactionValue.CoinValue -> {
                    limit = spamCoinLimits[transactionValue.coinCode] ?: BigDecimal.ZERO
                }

                is TransactionValue.JettonValue -> {
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
}
