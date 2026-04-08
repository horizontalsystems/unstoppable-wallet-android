package cash.p.terminal.core.notifications

import android.content.Context
import cash.p.terminal.R
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.BackgroundKeepAliveManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.notifications.polling.TransactionPollingManager
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.premium.settings.PollingInterval
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.IAppNumberFormatter
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.entities.BlockchainType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber


@Suppress("LongParameterList")
class TransactionMonitor(
    private val context: Context,
    private val transactionAdapterManager: TransactionAdapterManager,
    private val walletManager: IWalletManager,
    private val localStorage: ILocalStorage,
    private val deduplicator: NotificationDeduplicator,
    private val notificationManager: TransactionNotificationManager,
    private val keepAliveManager: BackgroundKeepAliveManager,
    private val marketKit: MarketKitWrapper,
    private val checkPremiumUseCase: CheckPremiumUseCase,
    private val currencyManager: CurrencyManager,
    private val numberFormatter: IAppNumberFormatter,
    private val pollingManager: TransactionPollingManager,
) {
    private var monitoringJob: Job? = null
    private var monitoredTypes: Set<BlockchainType> = emptySet()
    private var activeWallets: List<Wallet> = emptyList()
    private val recordsProcessingMutex = Mutex()
    var onPremiumExpired: (() -> Unit)? = null

    fun start(scope: CoroutineScope) {
        stop()

        val enabledUids = localStorage.pushEnabledBlockchainUids
        monitoredTypes = walletManager.activeWallets
            .map { it.token.blockchainType }
            .filter { it.uid in enabledUids }
            .toSet()
        activeWallets = walletManager.activeWallets

        if (monitoredTypes.isEmpty()) return

        val now = System.currentTimeMillis() / 1000
        monitoredTypes.forEach { deduplicator.updateLastCheckTime(it.uid, now) }

        val interval = localStorage.pushPollingInterval

        monitoringJob = scope.launch {
            if (interval == PollingInterval.REALTIME) {
                val adapters = transactionAdapterManager.adaptersReadyFlow
                    .first { it.isNotEmpty() }
                val relevantAdapters = adapters.filter { (source, _) ->
                    source.blockchain.type in monitoredTypes
                }
                launchRealtimeCollectors(relevantAdapters)
            } else {
                launchPolling(interval)
            }
        }
    }

    fun stop() {
        monitoringJob?.cancel()
        monitoringJob = null
        keepAliveManager.clear()
        deduplicator.reset()
    }

    private fun CoroutineScope.launchRealtimeCollectors(
        adapters: Map<TransactionSource, ITransactionsAdapter>,
    ) {
        adapters.values.forEach { adapter ->
            launch {
                adapter.getTransactionRecordsFlow(
                    token = null,
                    transactionType = FilterTransactionType.All,
                    address = null,
                ).collect { records ->
                    processRecords(records)
                }
            }
        }
        launch {
            while (true) {
                delay(PREMIUM_CHECK_INTERVAL_MS)
                if (!checkPremiumUseCase.getPremiumType().isPremium()) {
                    onPremiumExpired?.invoke()
                    return@launch
                }
            }
        }
    }

    private fun CoroutineScope.launchPolling(
        interval: PollingInterval,
    ) {
        val delayMs = interval.minutes * 60 * 1000L
        launch {
            while (true) {
                delay(delayMs)
                Timber.tag("TxMonitor").d("Polling for transactions...")
                if (!checkPremiumUseCase.getPremiumType().isPremium()) {
                    onPremiumExpired?.invoke()
                    Timber.tag("TxMonitor").d("Premium expired, stopping monitor")
                    return@launch
                }
                val records = pollingManager.pollAll(monitoredTypes, activeWallets)
                Timber.tag("TxMonitor").d("Polling returned %d records", records.size)
                processRecords(records)
            }
        }
    }

    private suspend fun processRecords(records: List<TransactionRecord>) = recordsProcessingMutex.withLock {
        Timber.tag("TxMonitor").d("processRecords: %d records from %s",
            records.size,
            records.map { it.blockchainType.uid }.distinct().joinToString()
        )
        val showBlockchainName = localStorage.pushShowBlockchainName
        val showCoinAmount = localStorage.pushShowCoinAmount
        val showFiatAmount = localStorage.pushShowFiatAmount

        records.forEach { record ->
            val blockchainUid = record.blockchainType.uid
            if (deduplicator.isNew(record.uid, blockchainUid, record.timestamp)) {
                val mainValue = record.mainValue
                Timber.tag("TxMonitor").d(
                    "New tx: uid=%s chain=%s amount=%s %s type=%s hash=%s ts=%d",
                    record.uid,
                    blockchainUid,
                    mainValue?.decimalValue?.toPlainString() ?: "?",
                    mainValue?.coinCode ?: "?",
                    record.transactionRecordType,
                    record.transactionHash,
                    record.timestamp,
                )
                deduplicator.markNotified(record.uid)

                val baseCurrency = currencyManager.baseCurrency
                val (title, text) = buildNotificationContent(
                    record, showBlockchainName, showCoinAmount, showFiatAmount,
                    baseCurrency.code, baseCurrency.symbol,
                )

                notificationManager.showTransactionNotification(
                    recordUid = record.uid,
                    title = title,
                    text = text,
                )
            }
        }

        records.groupBy { it.blockchainType.uid }.forEach { (blockchainUid, blockchainRecords) ->
            val maxTimestamp = blockchainRecords.maxOf { it.timestamp }
            val maxTimestampUids = blockchainRecords
                .filter { it.timestamp == maxTimestamp }
                .mapTo(mutableSetOf()) { it.uid }
            deduplicator.updateLastCheckTime(blockchainUid, maxTimestamp, maxTimestampUids)
        }
    }

    private fun buildNotificationContent(
        record: TransactionRecord,
        showBlockchainName: Boolean,
        showCoinAmount: Boolean,
        showFiatAmount: Boolean,
        currencyCode: String,
        currencySymbol: String,
    ): Pair<String, String> {
        val mainValue = record.mainValue
        val coinText = buildCoinText(mainValue, showCoinAmount)
        val fiatText = buildFiatText(mainValue, showFiatAmount, currencyCode, currencySymbol)
        val amountPart = buildAmountPart(coinText, fiatText)

        val blockchainName = record.source.blockchain.name
        val fallback = context.getString(R.string.push_notification_new_transaction)

        val text = when {
            showBlockchainName && amountPart != null -> "$blockchainName: $amountPart"
            showBlockchainName -> "$blockchainName: $fallback"
            amountPart != null -> amountPart
            else -> fallback
        }

        return context.getString(R.string.App_Name) to text
    }

    private fun buildCoinText(
        mainValue: TransactionValue?,
        showCoinAmount: Boolean,
    ): String? {
        if (!showCoinAmount || mainValue == null) return null
        val decimalValue = mainValue.decimalValue ?: return null
        return numberFormatter.formatCoinFull(decimalValue, mainValue.coinCode, mainValue.decimals ?: 8)
    }

    private fun buildFiatText(
        mainValue: TransactionValue?,
        showFiatAmount: Boolean,
        currencyCode: String,
        currencySymbol: String,
    ): String? {
        if (!showFiatAmount || mainValue == null) return null
        val coinUid = mainValue.coinUid
        val decimalValue = mainValue.decimalValue
        if (coinUid.isEmpty() || decimalValue == null) return null
        val price = marketKit.coinPrice(coinUid, currencyCode)?.value ?: return null
        val fiat = decimalValue.abs().multiply(price)
        return numberFormatter.formatFiatFull(fiat, currencySymbol)
    }

    private fun buildAmountPart(coinText: String?, fiatText: String?): String? = when {
        coinText != null && fiatText != null -> "$coinText ($fiatText)"
        coinText != null -> coinText
        fiatText != null -> "~$fiatText"
        else -> null
    }

    companion object {
        private const val PREMIUM_CHECK_INTERVAL_MS = 30 * 60 * 1000L
    }
}
