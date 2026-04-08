package cash.p.terminal.core.notifications

import android.content.Context
import android.content.SharedPreferences
import cash.p.terminal.core.ILocalStorage
import cash.p.terminal.core.ITransactionsAdapter
import cash.p.terminal.core.managers.BackgroundKeepAliveManager
import cash.p.terminal.core.managers.TransactionAdapterManager
import cash.p.terminal.core.notifications.polling.TransactionPollingManager
import io.horizontalsystems.core.BackgroundManager
import cash.p.terminal.entities.transactionrecords.TransactionRecord
import cash.p.terminal.modules.premium.settings.PollingInterval
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.premium.domain.usecase.CheckPremiumUseCase
import cash.p.terminal.premium.domain.usecase.PremiumType
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import cash.p.terminal.wallet.Wallet
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Integration-level tests for TransactionMonitor + NotificationDeduplicator,
 * verifying that the deduplication layer correctly prevents notification floods.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionMonitorDeduplicationTest {

    private val context = mockk<Context>(relaxed = true)
    private val transactionAdapterManager = mockk<TransactionAdapterManager>(relaxed = true)
    private val walletManager = mockk<IWalletManager>(relaxed = true)
    private val localStorage = mockk<ILocalStorage>(relaxed = true)
    private val notificationManager = mockk<TransactionNotificationManager>(relaxed = true)
    private val keepAliveManager = BackgroundKeepAliveManager()
    private val marketKit = mockk<MarketKitWrapper>(relaxed = true)
    private val checkPremiumUseCase = mockk<CheckPremiumUseCase>(relaxed = true)
    private val currencyManager = mockk<CurrencyManager>(relaxed = true)
    private val numberFormatter = mockk<IAppNumberFormatter>(relaxed = true)
    private val backgroundManager = mockk<BackgroundManager>(relaxed = true)
    private val pollingManager = TransactionPollingManager(pollers = emptyList(), backgroundManager)

    private val bitcoinBlockchain = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null)
    private val ethereumBlockchain = Blockchain(BlockchainType.Ethereum, "Ethereum", null)
    private val bitcoinSource = mockk<TransactionSource> {
        every { blockchain } returns bitcoinBlockchain
    }
    private val ethereumSource = mockk<TransactionSource> {
        every { blockchain } returns ethereumBlockchain
    }

    private val store = mutableMapOf<String, Any?>()
    private lateinit var prefs: SharedPreferences
    private lateinit var deduplicator: NotificationDeduplicator

    @Before
    fun setUp() {
        store.clear()
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        every { editor.putLong(any(), any()) } answers {
            store[firstArg()] = secondArg<Long>()
            editor
        }
        every { editor.apply() } just Runs

        prefs = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        every { prefs.all } answers { store.toMap() }

        deduplicator = NotificationDeduplicator(prefs)

        every { context.getString(any()) } returns "New transaction"
        every { localStorage.pushPollingInterval } returns PollingInterval.REALTIME
        every { localStorage.pushShowBlockchainName } returns false
        every { localStorage.pushShowCoinAmount } returns false
        every { localStorage.pushShowFiatAmount } returns false
        every { localStorage.baseCurrencyCode } returns "USD"
        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE
    }

    private fun createMonitor(
        deduplicator: NotificationDeduplicator = this.deduplicator,
    ) = TransactionMonitor(
        context = context,
        transactionAdapterManager = transactionAdapterManager,
        walletManager = walletManager,
        localStorage = localStorage,
        deduplicator = deduplicator,
        notificationManager = notificationManager,
        keepAliveManager = keepAliveManager,
        marketKit = marketKit,
        checkPremiumUseCase = checkPremiumUseCase,
        currencyManager = currencyManager,
        numberFormatter = numberFormatter,
        pollingManager = pollingManager,
    )


    private fun mockWallet(blockchainType: BlockchainType): Wallet =
        mockk(relaxed = true) {
            every { token.blockchainType } returns blockchainType
        }

    private fun mockAdapter(): Pair<ITransactionsAdapter, MutableSharedFlow<List<TransactionRecord>>> {
        val flow = MutableSharedFlow<List<TransactionRecord>>()
        val adapter = mockk<ITransactionsAdapter>(relaxed = true) {
            every {
                getTransactionRecordsFlow(null, FilterTransactionType.All, null)
            } returns flow
        }
        return adapter to flow
    }

    private fun mockRecord(
        uid: String,
        timestamp: Long,
        source: TransactionSource,
    ): TransactionRecord = mockk(relaxed = true) {
        every { this@mockk.uid } returns uid
        every { this@mockk.timestamp } returns timestamp
        every { this@mockk.source } returns source
        every { this@mockk.blockchainType } returns source.blockchain.type
        every { this@mockk.mainValue } returns null
    }

    @Test
    fun start_multipleBlockchains_onlyNotifiesNewRecordsPerChain() =
        runTest(StandardTestDispatcher()) {
            every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin", "ethereum")
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin),
                mockWallet(BlockchainType.Ethereum),
            )

            val (btcAdapter, btcFlow) = mockAdapter()
            val (ethAdapter, ethFlow) = mockAdapter()
            val adaptersFlow = MutableStateFlow(
                mapOf(
                    bitcoinSource to btcAdapter,
                    ethereumSource to ethAdapter,
                )
            )
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            val futureTime = System.currentTimeMillis() / 1000 + 500

            // Emit a new BTC transaction
            btcFlow.emit(listOf(mockRecord("btc-tx-1", futureTime, bitcoinSource)))
            runCurrent()

            // Emit a historical ETH transaction (should be filtered)
            ethFlow.emit(listOf(mockRecord("eth-old-1", 100L, ethereumSource)))
            runCurrent()

            // Emit a new ETH transaction
            ethFlow.emit(listOf(mockRecord("eth-tx-1", futureTime + 1, ethereumSource)))
            runCurrent()

            verify(exactly = 1) {
                notificationManager.showTransactionNotification(
                    recordUid = "btc-tx-1",
                    title = any(),
                    text = any(),
                )
            }
            verify(exactly = 0) {
                notificationManager.showTransactionNotification(
                    recordUid = "eth-old-1",
                    title = any(),
                    text = any(),
                )
            }
            verify(exactly = 1) {
                notificationManager.showTransactionNotification(
                    recordUid = "eth-tx-1",
                    title = any(),
                    text = any(),
                )
            }

            monitor.stop()
        }

    @Test
    fun processRecords_duplicateUids_notifiesOnlyOnce() =
        runTest(StandardTestDispatcher()) {
            every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin),
            )

            val (adapter, recordsFlow) = mockAdapter()
            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to adapter))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            val futureTime = System.currentTimeMillis() / 1000 + 500
            val record = mockRecord("btc-dup-1", futureTime, bitcoinSource)

            // Emit the same record twice in separate batches
            recordsFlow.emit(listOf(record))
            runCurrent()

            recordsFlow.emit(listOf(record))
            runCurrent()

            verify(exactly = 1) {
                notificationManager.showTransactionNotification(
                    recordUid = "btc-dup-1",
                    title = any(),
                    text = any(),
                )
            }

            monitor.stop()
        }

    @Test
    fun start_enabledBlockchainSubset_ignoresDisabledChains() =
        runTest(StandardTestDispatcher()) {
            // Only Bitcoin is enabled for notifications, Ethereum is not
            every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin),
                mockWallet(BlockchainType.Ethereum),
            )

            val (btcAdapter, btcFlow) = mockAdapter()
            val (ethAdapter, _) = mockAdapter()
            val adaptersFlow = MutableStateFlow(
                mapOf(
                    bitcoinSource to btcAdapter,
                    ethereumSource to ethAdapter,
                )
            )
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            val futureTime = System.currentTimeMillis() / 1000 + 500
            btcFlow.emit(listOf(mockRecord("btc-tx-1", futureTime, bitcoinSource)))
            runCurrent()

            verify(exactly = 1) {
                notificationManager.showTransactionNotification(
                    recordUid = "btc-tx-1",
                    title = any(),
                    text = any(),
                )
            }
            // Ethereum adapter should not even be collected (filtered by monitoredTypes)
            verify(exactly = 0) {
                ethAdapter.getTransactionRecordsFlow(any(), any(), any())
            }

            monitor.stop()
        }

    @Test
    fun start_realtimeConcurrentDuplicateRecord_notifiesOnlyOnce() =
        runTest(StandardTestDispatcher()) {
            every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin", "ethereum")
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin),
                mockWallet(BlockchainType.Ethereum),
            )

            val (btcAdapter, btcFlow) = mockAdapter()
            val (ethAdapter, ethFlow) = mockAdapter()
            val adaptersFlow = MutableStateFlow(
                mapOf(
                    bitcoinSource to btcAdapter,
                    ethereumSource to ethAdapter,
                )
            )
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val enteredIsNew = CountDownLatch(2)
            val marked = AtomicBoolean(false)
            val racingDeduplicator = mockk<NotificationDeduplicator>()
            every { racingDeduplicator.updateLastCheckTime(any(), any()) } just Runs
            every { racingDeduplicator.reset() } just Runs
            every { racingDeduplicator.markNotified(any()) } answers {
                marked.set(true)
            }
            every { racingDeduplicator.isNew(any(), any(), any()) } answers {
                enteredIsNew.countDown()
                enteredIsNew.await(200, TimeUnit.MILLISECONDS)
                !marked.get()
            }

            val monitor = createMonitor(racingDeduplicator)
            val monitorScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
            monitor.start(monitorScope)
            runCurrent()

            val futureTime = System.currentTimeMillis() / 1000 + 500
            val duplicateRecord = mockRecord("shared-tx-1", futureTime, bitcoinSource)

            launch(Dispatchers.Default) { btcFlow.emit(listOf(duplicateRecord)) }
            launch(Dispatchers.Default) { ethFlow.emit(listOf(duplicateRecord)) }

            try {
                enteredIsNew.await(1, TimeUnit.SECONDS)
                kotlinx.coroutines.delay(300)

                verify(exactly = 1) {
                    notificationManager.showTransactionNotification(
                        recordUid = "shared-tx-1",
                        title = any(),
                        text = any(),
                    )
                }
            } finally {
                monitor.stop()
                monitorScope.cancel()
            }
        }
}
