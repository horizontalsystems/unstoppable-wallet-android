package cash.p.terminal.core.notifications

import android.content.Context
import android.content.SharedPreferences
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
import cash.p.terminal.premium.domain.usecase.PremiumType
import cash.p.terminal.wallet.IWalletManager
import cash.p.terminal.wallet.MarketKitWrapper
import cash.p.terminal.wallet.Wallet
import io.horizontalsystems.core.CurrencyManager
import io.horizontalsystems.core.IAppNumberFormatter
import io.horizontalsystems.core.entities.Currency
import cash.p.terminal.wallet.models.CoinPrice
import cash.p.terminal.wallet.transaction.TransactionSource
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.coVerify
import io.mockk.coEvery
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionMonitorTest {

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
    private val pollingManager = mockk<TransactionPollingManager>(relaxed = true)

    private val bitcoinBlockchain = Blockchain(BlockchainType.Bitcoin, "Bitcoin", null)
    private val bitcoinSource = mockk<TransactionSource> {
        every { blockchain } returns bitcoinBlockchain
    }

    private lateinit var deduplicator: NotificationDeduplicator

    private val store = mutableMapOf<String, Any?>()
    private lateinit var prefs: SharedPreferences

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
        every { prefs.getLong(any(), any()) } answers {
            (store[firstArg()] as? Long) ?: secondArg()
        }

        deduplicator = NotificationDeduplicator(prefs)

        every { context.getString(R.string.App_Name) } returns "P.CASH"
        every { context.getString(R.string.push_notification_new_transaction) } returns "New transaction"

        every { localStorage.pushNotificationsEnabled } returns true
        every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
        every { localStorage.pushPollingInterval } returns PollingInterval.REALTIME
        every { localStorage.pushShowBlockchainName } returns true
        every { localStorage.pushShowCoinAmount } returns true
        every { localStorage.pushShowFiatAmount } returns true
        every { currencyManager.baseCurrency } returns Currency("USD", "$", 2, 0)
        every { numberFormatter.formatCoinFull(any(), any(), any()) } answers {
            "${firstArg<BigDecimal>().toPlainString()} ${secondArg<String>()}"
        }
        every { numberFormatter.formatFiatFull(any(), any()) } answers {
            "${secondArg<String>()}${firstArg<BigDecimal>().setScale(2, java.math.RoundingMode.HALF_UP)}"
        }

        every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE
    }

    private fun createMonitor() = TransactionMonitor(
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

    private fun mockAdapter(
        recordsFlow: Flow<List<TransactionRecord>> = MutableSharedFlow(),
    ): ITransactionsAdapter = mockk(relaxed = true) {
        every {
            getTransactionRecordsFlow(null, FilterTransactionType.All, null)
        } returns recordsFlow
    }

    private fun mockRecord(
        uid: String,
        timestamp: Long,
        source: TransactionSource = bitcoinSource,
        mainValue: TransactionValue? = null,
    ): TransactionRecord = mockk(relaxed = true) {
        every { this@mockk.uid } returns uid
        every { this@mockk.timestamp } returns timestamp
        every { this@mockk.source } returns source
        every { this@mockk.blockchainType } returns source.blockchain.type
        every { this@mockk.mainValue } returns mainValue
    }

    @Test
    fun launchPolling_doesNotPollImmediately() =
        runTest(StandardTestDispatcher()) {
            val btcWallet = mockWallet(BlockchainType.Bitcoin)
            every { localStorage.pushNotificationsEnabled } returns true
            every { localStorage.pushPollingInterval } returns PollingInterval.MIN_5
            every { localStorage.pushEnabledBlockchainUids } returns setOf("bitcoin")
            every { walletManager.activeWallets } returns listOf(btcWallet)
            every { checkPremiumUseCase.getPremiumType() } returns PremiumType.PIRATE
            coEvery { pollingManager.pollAll(any(), any()) } returns emptyList()

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            // At T=0, no polling
            coVerify(exactly = 0) { pollingManager.pollAll(any(), any()) }

            // Advance by 4 minutes
            advanceTimeBy(4 * 60 * 1000L)
            runCurrent()
            coVerify(exactly = 0) { pollingManager.pollAll(any(), any()) }

            // Advance to 5 minutes -> first poll
            advanceTimeBy(1 * 60 * 1000L)
            runCurrent()
            coVerify(exactly = 1) { pollingManager.pollAll(any(), any()) }

            monitor.stop()
        }

    @Test
    fun start_realtimePremiumExpires_invokesOnPremiumExpired() =
        runTest(StandardTestDispatcher()) {
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin)
            )

            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to mockAdapter()))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            // The first getPremiumType() call happens after the first 30-min delay.
            // Return NONE immediately to trigger onPremiumExpired on the first check.
            every { checkPremiumUseCase.getPremiumType() } returns PremiumType.NONE

            val monitor = createMonitor()
            var premiumExpiredCalled = false
            monitor.onPremiumExpired = { premiumExpiredCalled = true }
            monitor.start(backgroundScope)

            // Process initial setup (adapter discovery, collector launch)
            runCurrent()

            // Advance past the 30-minute premium check interval
            advanceTimeBy(30 * 60 * 1000L + 1)
            runCurrent()

            assertTrue(premiumExpiredCalled, "Expected onPremiumExpired to be called")

            monitor.stop()
        }

    @Test
    fun processRecords_showFiatAmountEnabled_includesFiatInNotificationText() =
        runTest(StandardTestDispatcher()) {
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin)
            )

            val recordsFlow = MutableSharedFlow<List<TransactionRecord>>()
            val adapter = mockAdapter(recordsFlow)
            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to adapter))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val mainValue = mockk<TransactionValue>(relaxed = true) {
                every { coinCode } returns "BTC"
                every { coinUid } returns "bitcoin"
                every { decimalValue } returns BigDecimal("0.5")
                every { decimals } returns 8
            }
            val futureTimestamp = System.currentTimeMillis() / 1000 + 1000
            val record = mockRecord(
                uid = "tx-1",
                timestamp = futureTimestamp,
                mainValue = mainValue,
            )

            every { marketKit.coinPrice("bitcoin", "USD") } returns CoinPrice(
                coinUid = "bitcoin",
                currencyCode = "USD",
                value = BigDecimal("50000"),
                diff1h = null,
                diff24h = null,
                diff7d = null,
                diff30d = null,
                diff1y = null,
                diffAll = null,
                timestamp = System.currentTimeMillis() / 1000,
            )

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            recordsFlow.emit(listOf(record))
            runCurrent()

            verify {
                notificationManager.showTransactionNotification(
                    recordUid = "tx-1",
                    title = any(),
                    text = match { it.contains("$25000.00") },
                )
            }

            monitor.stop()
        }

    @Test
    fun processRecords_allDisplayOptionsDisabled_usesGenericText() =
        runTest(StandardTestDispatcher()) {
            every { localStorage.pushShowBlockchainName } returns false
            every { localStorage.pushShowCoinAmount } returns false
            every { localStorage.pushShowFiatAmount } returns false

            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin)
            )

            val recordsFlow = MutableSharedFlow<List<TransactionRecord>>()
            val adapter = mockAdapter(recordsFlow)
            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to adapter))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val futureTimestamp = System.currentTimeMillis() / 1000 + 1000
            val record = mockRecord(uid = "tx-2", timestamp = futureTimestamp)

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            recordsFlow.emit(listOf(record))
            runCurrent()

            verify {
                notificationManager.showTransactionNotification(
                    recordUid = "tx-2",
                    title = "P.CASH",
                    text = "New transaction",
                )
            }

            monitor.stop()
        }

    @Test
    fun processRecords_eurCurrency_usesEuroSymbol() =
        runTest(StandardTestDispatcher()) {
            every { currencyManager.baseCurrency } returns Currency("EUR", "€", 2, 0)
            every { localStorage.pushShowBlockchainName } returns false
            every { localStorage.pushShowCoinAmount } returns false
            every { localStorage.pushShowFiatAmount } returns true

            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin)
            )

            val recordsFlow = MutableSharedFlow<List<TransactionRecord>>()
            val adapter = mockAdapter(recordsFlow)
            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to adapter))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val futureTimestamp = System.currentTimeMillis() / 1000 + 1000
            val mainValue = mockk<TransactionValue>(relaxed = true) {
                every { coinUid } returns "bitcoin"
                every { coinCode } returns "BTC"
                every { decimalValue } returns BigDecimal("1.0")
                every { decimals } returns 8
            }
            val record = mockRecord(uid = "tx-eur", timestamp = futureTimestamp, mainValue = mainValue)

            every { marketKit.coinPrice("bitcoin", "EUR") } returns CoinPrice(
                coinUid = "bitcoin",
                currencyCode = "EUR",
                value = BigDecimal("45000"),
                diff1h = null,
                diff24h = null,
                diff7d = null,
                diff30d = null,
                diff1y = null,
                diffAll = null,
                timestamp = System.currentTimeMillis() / 1000,
            )

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            recordsFlow.emit(listOf(record))
            runCurrent()

            verify {
                notificationManager.showTransactionNotification(
                    recordUid = "tx-eur",
                    title = any(),
                    text = match { it.contains("€45000.00") && !it.contains("$") },
                )
            }

            monitor.stop()
        }

    @Test
    fun start_initialBatch_doesNotNotifyHistoricalTransactions() =
        runTest(StandardTestDispatcher()) {
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin)
            )

            val recordsFlow = MutableSharedFlow<List<TransactionRecord>>()
            val adapter = mockAdapter(recordsFlow)
            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to adapter))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val record = mockRecord(uid = "old-tx-1", timestamp = 1000L)

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            recordsFlow.emit(listOf(record))
            runCurrent()

            verify(exactly = 0) {
                notificationManager.showTransactionNotification(any(), any(), any())
            }

            monitor.stop()
        }

    @Test
    fun start_restart_resetsLastCheckTimeToNow() =
        runTest(StandardTestDispatcher()) {
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin)
            )

            val recordsFlow = MutableSharedFlow<List<TransactionRecord>>()
            val adapter = mockAdapter(recordsFlow)
            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to adapter))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val monitor = createMonitor()

            // First session
            monitor.start(backgroundScope)
            runCurrent()
            monitor.stop()

            // Second session — old transactions (timestamp in the past) must be filtered
            monitor.start(backgroundScope)
            runCurrent()

            val pastTimestamp = System.currentTimeMillis() / 1000 - 3600
            recordsFlow.emit(listOf(mockRecord(uid = "old-tx", timestamp = pastTimestamp)))
            runCurrent()

            verify(exactly = 0) {
                notificationManager.showTransactionNotification(any(), any(), any())
            }

            monitor.stop()
        }

    @Test
    fun start_historicalBatchDoesNotRollbackLastCheckTime() =
        runTest(StandardTestDispatcher()) {
            every { walletManager.activeWallets } returns listOf(
                mockWallet(BlockchainType.Bitcoin)
            )

            val recordsFlow = MutableSharedFlow<List<TransactionRecord>>()
            val adapter = mockAdapter(recordsFlow)
            val adaptersFlow = MutableStateFlow(mapOf(bitcoinSource to adapter))
            every { transactionAdapterManager.adaptersReadyFlow } returns adaptersFlow

            val monitor = createMonitor()
            monitor.start(backgroundScope)
            runCurrent()

            // First emit: old transactions from a year ago
            val oldTimestamp = System.currentTimeMillis() / 1000 - 365 * 24 * 3600
            recordsFlow.emit(listOf(
                mockRecord(uid = "old-1", timestamp = oldTimestamp),
                mockRecord(uid = "old-2", timestamp = oldTimestamp + 100),
            ))
            runCurrent()

            // Second emit: another old transaction slightly newer than the first batch
            // Without monotonic guard, this would pass because lastCheckTime
            // was rolled back to oldTimestamp+100
            recordsFlow.emit(listOf(
                mockRecord(uid = "old-3", timestamp = oldTimestamp + 200),
            ))
            runCurrent()

            verify(exactly = 0) {
                notificationManager.showTransactionNotification(any(), any(), any())
            }

            monitor.stop()
        }
}
