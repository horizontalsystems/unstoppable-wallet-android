package io.horizontalsystems.bankwallet.core.managers

import android.util.Log
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

    private val stableCoinCodes = listOf("USDT", "USDC", "DAI", "BUSD", "EURS")
    private val negligibleValue = BigDecimal("0.01")

    private var transactionAdapterManager: TransactionAdapterManager? = null
    private val transferEventFactory = TransferEventFactory()

    var hideSuspiciousTx = localStorage.hideSuspiciousTransactions
        private set

    fun isSpam(
        incomingEvents: List<TransferEvent>,
        outgoingEvents: List<TransferEvent>
    ): Boolean {
        val allEvents = incomingEvents + outgoingEvents
        return allEvents.all { spamEvent(it) }
    }

    private fun spamEvent(event: TransferEvent): Boolean {
        return when (val eventValue = event.value) {
            is TransactionValue.CoinValue -> {
                spamValue(eventValue.coinCode, eventValue.value)
            }

            is TransactionValue.NftValue -> {
                eventValue.value <= BigDecimal.ZERO
            }

            else -> true
        }
    }

    private fun spamValue(coinCode: String, value: BigDecimal): Boolean {
        return if (stableCoinCodes.contains(coinCode)) {
            value < negligibleValue
        } else {
            value <= BigDecimal.ZERO
        }
    }

    fun set(transactionAdapterManager: TransactionAdapterManager) {
        this.transactionAdapterManager = transactionAdapterManager
        Log.e("eee", "set transactionAdapterManager")

        coroutineScope.launch {
            transactionAdapterManager.adaptersReadyFlow.collect {
                subscribeToAdapters(transactionAdapterManager)
            }
        }
    }

    private fun subscribeToAdapters(transactionAdapterManager: TransactionAdapterManager) {
        Log.e("eee", "total adapters to subscribe: ${transactionAdapterManager.adaptersMap.size}")

        transactionAdapterManager.adaptersMap.forEach { (transactionSource, transactionsAdapter) ->
            subscribeToAdapter(transactionSource, transactionsAdapter)
        }
    }

    private fun subscribeToAdapter(source: TransactionSource, adapter: ITransactionsAdapter) {
        coroutineScope.launch {
            adapter.transactionsStateUpdatedFlowable.asFlow().collect {
                Log.e("eee", "transactionsStateUpdated source: ${source.blockchain.name} ")
                sync(source)
            }
        }
    }

    private fun sync(source: TransactionSource) {
        singleDispatcherCoroutineScope.launch {
            val adapter = transactionAdapterManager?.getAdapter(source) ?: run {
                Log.e("eee", "No adapter for source: ${source.blockchain.name}")
                return@launch
            }
            val spamScanState = spamAddressStorage.getSpamScanState(source.blockchain.type, source.account.id)
            Log.e("eee", "lastSyncedTransactionId before: ${spamScanState?.lastSyncedTransactionId}")

            val transactions = adapter.getTransactionsAfter(spamScanState?.lastSyncedTransactionId).blockingGet()
            Log.e("eee", "sync transactions: ${transactions.size}")

            val lastSyncedTransactionId = handle(transactions, source)
            Log.e("eee", "lastSyncedTransactionId after: $lastSyncedTransactionId")

            lastSyncedTransactionId?.let {
                spamAddressStorage.save(SpamScanState(source.blockchain.type, source.account.id, lastSyncedTransactionId))
            }
        }
    }

    private fun handle(transactions: List<TransactionRecord>, source: TransactionSource): String? {
        val txWithEvents = transactions.map { Pair(it.transactionHash, transferEventFactory.transferEvents(it)) }

        Log.e("eee", "txWithEvents: ${txWithEvents.size}")
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
            Log.e("eee", "handleSpamAddresses: ${events.joinToString { (it.address ?: "") + " " + it.value.decimalValue }}")

            val spamTokenSenders = mutableListOf<String>()
            val nativeSenders = mutableListOf<String>()
            var totalNativeTransactionValue: TransactionValue? = null

            events.forEach { event ->
                if (event.value is TransactionValue.CoinValue && event.value.token.type == TokenType.Native) {
                    Log.e("eee", "${event.address} native")
                    val totalNativeValue = totalNativeTransactionValue?.decimalValue ?: BigDecimal.ZERO
                    totalNativeTransactionValue = TransactionValue.CoinValue(event.value.token, event.value.value + totalNativeValue)
                    event.address?.let { nativeSenders.add(it) }
                } else {
                    Log.e("eee", "${event.address} spam: ${isSpam(event.value)}, address: ${event.address}")
                    if (event.address != null && isSpam(event.value)) {
                        spamTokenSenders.add(event.address)
                    }
                }
            }

            if (totalNativeTransactionValue != null && isSpam(totalNativeTransactionValue!!) && nativeSenders.isNotEmpty()) {
                Log.e(
                    "eee",
                    "nativeSender ${nativeSenders.joinToString { it }}, totalNativeTransactionValue: ${totalNativeTransactionValue?.decimalValue}, ${totalNativeTransactionValue?.javaClass?.simpleName}"
                )

                spamTokenSenders.addAll(nativeSenders)
            }

            return spamTokenSenders
        }

        private fun isSpam(transactionValue: TransactionValue): Boolean {
            val spamCoinLimits = App.appConfigProvider.spamCoinValueLimits
            val value = transactionValue.decimalValue

            Log.e("eee", "isSpam value=${value}, transactionValue=${transactionValue.javaClass.simpleName}")
            var limit: BigDecimal = BigDecimal.ZERO
            when (transactionValue) {
                is TransactionValue.CoinValue -> {
                    limit = spamCoinLimits[transactionValue.coinCode] ?: BigDecimal.ZERO
                    Log.e("eee", "transactionValue.coinCode: ${transactionValue.coinCode}, limit: ${limit}, value=$value")
                }

                is TransactionValue.JettonValue -> {
                    limit = spamCoinLimits[transactionValue.coinCode] ?: BigDecimal.ZERO
                    Log.e("eee", "transactionValue.coinCode: ${transactionValue.coinCode}, limit: ${limit}, value=$value")
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
