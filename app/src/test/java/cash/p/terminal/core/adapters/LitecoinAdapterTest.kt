package cash.p.terminal.core.adapters

import cash.p.terminal.R
import cash.p.terminal.core.IFeeRateProvider
import cash.p.terminal.core.LocalizedException
import cash.p.terminal.core.TestDispatcherProvider
import cash.p.terminal.core.TransactionExplorerData
import cash.p.terminal.core.UnsupportedAccountException
import cash.p.terminal.entities.TransactionValue
import cash.p.terminal.entities.transactionrecords.TransactionRecordType
import cash.p.terminal.entities.transactionrecords.bitcoin.BitcoinTransactionRecord
import cash.p.terminal.wallet.Account
import cash.p.terminal.wallet.AccountOrigin
import cash.p.terminal.wallet.AccountType
import cash.p.terminal.wallet.AdapterState
import cash.p.terminal.wallet.Token
import cash.p.terminal.wallet.WalletFactory
import cash.p.terminal.wallet.entities.Coin
import cash.p.terminal.wallet.entities.TokenType
import cash.p.terminal.wallet.policy.HardwareWalletTokenPolicy
import cash.p.terminal.modules.transactions.FilterTransactionType
import cash.p.terminal.modules.transactions.TransactionStatus as RecordTransactionStatus
import io.horizontalsystems.bitcoincore.BitcoinCore
import io.horizontalsystems.bitcoincore.exceptions.AddressFormatException
import io.horizontalsystems.bitcoincore.models.BalanceInfo
import io.horizontalsystems.bitcoincore.models.BlockInfo
import io.horizontalsystems.bitcoincore.models.TransactionInfo
import io.horizontalsystems.bitcoincore.models.TransactionOutputInfo
import io.horizontalsystems.bitcoincore.models.TransactionFilterType
import io.horizontalsystems.bitcoincore.models.TransactionStatus
import io.horizontalsystems.bitcoincore.models.TransactionType
import io.horizontalsystems.bitcoincore.models.TransactionDataSortType
import io.horizontalsystems.bitcoincore.storage.UnspentOutputInfo
import io.horizontalsystems.bitcoincore.storage.UtxoFilters
import io.horizontalsystems.core.BackgroundManager
import io.horizontalsystems.core.DefaultDispatcherProvider
import io.horizontalsystems.core.DispatcherProvider
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.litecoinkit.LitecoinBalance
import io.horizontalsystems.litecoinkit.LitecoinKit
import io.horizontalsystems.litecoinkit.LitecoinMwebState
import io.horizontalsystems.litecoinkit.LitecoinSendInfo
import io.horizontalsystems.litecoinkit.LitecoinSendResult
import io.horizontalsystems.litecoinkit.LitecoinSendSource
import io.horizontalsystems.litecoinkit.mweb.MwebBalance
import io.horizontalsystems.litecoinkit.mweb.MwebDebugInfo
import io.horizontalsystems.litecoinkit.mweb.MwebError
import io.horizontalsystems.litecoinkit.mweb.MwebNetworkPolicy
import io.horizontalsystems.litecoinkit.mweb.MwebPendingTransaction
import io.horizontalsystems.litecoinkit.mweb.MwebSendInfo
import io.horizontalsystems.litecoinkit.mweb.MwebSendResult
import io.horizontalsystems.litecoinkit.mweb.MwebSyncState
import io.horizontalsystems.litecoinkit.mweb.MwebTransaction
import io.horizontalsystems.litecoinkit.mweb.MwebTransactionKind
import io.horizontalsystems.litecoinkit.mweb.MwebTransactionType
import io.horizontalsystems.litecoinkit.mweb.MwebUtxo
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.reactivex.Single
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertFailsWith

@OptIn(ExperimentalCoroutinesApi::class)
class LitecoinAdapterTest {

    private val kit = mockk<LitecoinKit>(relaxed = true)
    private val backgroundManager = mockk<BackgroundManager>(relaxed = true)
    private val feeRateProvider = mockk<IFeeRateProvider>(relaxed = true)

    @Test
    fun stop_litecoinAdapter_disposesKit() {
        val adapter = createPublicAdapter()

        adapter.stop()

        verify(exactly = 1) { kit.dispose() }
    }

    // Right after broadcasting an MWEB transaction the SDK marks the spent UTXOs as
    // spent immediately, while the change output stays unconfirmed in the mempool
    // until the next MWEB block (~2.5 min). The user expects "Available Balance" on
    // the Send screen to keep reflecting their funds, but balanceData.available
    // currently uses only MwebBalance.confirmed and therefore shows 0 for that whole
    // window.
    @Test
    fun balanceData_mwebOnlyUnconfirmedChange_exposesUnconfirmedAsAvailable() {
        val adapter = createMwebAdapter()
        every { kit.litecoinBalance } returns LitecoinBalance(
            publicSpendable = 0,
            publicUnspendable = 0,
            mweb = MwebBalance(confirmed = 0, unconfirmed = 2_600_000)
        )

        val balance = adapter.balanceData

        assertEquals(BigDecimal("0.02600000"), balance.available)
        assertEquals(BigDecimal.ZERO, balance.notRelayed)
        assertEquals(BigDecimal("0.02600000"), balance.total)
    }

    @Test
    fun balanceState_mwebSyncBehindPublicTip_reportsSyncing() {
        val adapter = createMwebAdapter()

        every { kit.lastBlockInfo } returns BlockInfo("hash", 1_000, 1_000)
        every { kit.mwebState } returns mwebState(MwebSyncState(1_000, 900, 900))

        adapter.onKitStateUpdate(BitcoinCore.KitState.Synced)
        adapter.onMwebSyncStateUpdate(MwebSyncState(1_000, 900, 900))

        assertTrue(adapter.balanceState is AdapterState.Syncing)
        assertTrue(adapter.transactionsState is AdapterState.Syncing)
    }

    @Test
    fun balanceState_mwebWaitingForPublicHeaders_usesPublicHeaderSyncState() {
        val adapter = createMwebAdapter()
        val syncState = MwebSyncState(2_955_000, 0, 0)

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", 2_955_000, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(syncState)

        adapter.onKitStateUpdate(BitcoinCore.KitState.Syncing(0.824, maxBlockHeight = 3_107_000))
        adapter.onMwebSyncStateUpdate(syncState)

        val state = adapter.balanceState as AdapterState.Syncing
        assertEquals(82.0, state.progress)
        assertEquals(152_000L, state.blocksRemained)
    }

    @Test
    fun balanceState_mwebHeadersStarted_usesStagedMwebProgress() {
        val adapter = createMwebAdapter()
        val tipHeight = 3_107_000
        val activationHeight = MwebNetworkPolicy.MAINNET_ACTIVATION_HEIGHT
        val syncState = MwebSyncState(
            blockHeaderHeight = tipHeight,
            mwebHeaderHeight = activationHeight + 50_000,
            mwebUtxosHeight = activationHeight + 10_000
        )

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", tipHeight, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(syncState)

        adapter.onKitStateUpdate(BitcoinCore.KitState.Synced)
        adapter.onMwebSyncStateUpdate(syncState)

        val totalBlocks = tipHeight - activationHeight
        val expectedProgress = listOf(
            1.0,
            50_000.toDouble() / totalBlocks.toDouble(),
            10_000.toDouble() / totalBlocks.toDouble()
        ).average() * 100.0
        val state = adapter.balanceState as AdapterState.Syncing

        assertEquals(expectedProgress, requireNotNull(state.progress), 0.0000001)
        assertEquals(null, state.blocksRemained)
    }

    @Test
    fun balanceState_mwebCaughtUp_reportsSynced() {
        val adapter = createMwebAdapter()

        every { kit.lastBlockInfo } returns BlockInfo("hash", 1_000, 1_000)
        every { kit.mwebState } returns mwebState(MwebSyncState(1_000, 1_000, 1_000))

        adapter.onKitStateUpdate(BitcoinCore.KitState.Synced)
        adapter.onMwebSyncStateUpdate(MwebSyncState(1_000, 1_000, 1_000))

        assertEquals(AdapterState.Synced, adapter.balanceState)
        assertEquals(AdapterState.Synced, adapter.transactionsState)
    }

    @Test
    fun balanceState_mwebCaughtUpWhilePublicWaitingForPeers_reportsConnecting() {
        val adapter = createMwebAdapter()

        every { kit.mwebState } returns mwebState(MwebSyncState(1_000, 1_000, 1_000))

        adapter.onKitStateUpdate(BitcoinCore.KitState.Syncing(0.0, BitcoinCore.SyncSubstatus.WaitingForPeers(0, 10)))
        adapter.onMwebSyncStateUpdate(MwebSyncState(1_000, 1_000, 1_000))

        assertEquals(AdapterState.Connecting, adapter.balanceState)
        assertEquals(AdapterState.Connecting, adapter.transactionsState)
    }

    @Test
    fun balanceState_mwebStateRestoredFromDbBeforePublicLastBlock_reportsConnecting() {
        val adapter = createMwebAdapter()
        val syncState = MwebSyncState(3_100_000, 3_100_000, 3_100_000)

        every { kit.lastBlockInfo } returns null
        every { kit.mwebState } returns mwebState(syncState)

        adapter.onKitStateUpdate(BitcoinCore.KitState.Synced)
        adapter.onMwebSyncStateUpdate(syncState)

        assertEquals(AdapterState.Connecting, adapter.balanceState)
        assertEquals(AdapterState.Connecting, adapter.transactionsState)
    }

    @Test
    fun balanceState_publicSyncedMwebUtxosLagBehind_reportsSyncingWithProgress() {
        val adapter = createMwebAdapter()
        val tipHeight = 3_107_000
        val syncState = MwebSyncState(
            blockHeaderHeight = tipHeight,
            mwebHeaderHeight = tipHeight,
            mwebUtxosHeight = tipHeight - 10_000
        )

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", tipHeight, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(syncState)

        adapter.onKitStateUpdate(BitcoinCore.KitState.Synced)
        adapter.onMwebSyncStateUpdate(syncState)

        val state = adapter.balanceState as AdapterState.Syncing
        val progress = requireNotNull(state.progress)
        assertTrue(progress > 0.0)
        assertTrue(progress < 100.0)
    }

    @Test
    fun balanceAndTransactionsState_mwebMode_reusesComputedAdapterState() {
        val adapter = createMwebAdapter()
        val tipHeight = 3_107_000
        val syncState = MwebSyncState(tipHeight, tipHeight, tipHeight)

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", tipHeight, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(syncState)

        adapter.onKitStateUpdate(BitcoinCore.KitState.Synced)
        adapter.onMwebSyncStateUpdate(syncState)
        clearMocks(kit, answers = false)

        assertEquals(AdapterState.Synced, adapter.balanceState)
        assertEquals(AdapterState.Synced, adapter.transactionsState)

        verify(exactly = 1) { kit.lastBlockInfo }
    }

    @Test
    fun lastBlockInfo_publicMode_usesPublicHeight() {
        val adapter = createPublicAdapter()

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", 3_104_327, 1_778_315_996L)

        assertEquals(3_104_327, adapter.lastBlockInfo?.height)
        assertEquals(1_778_315_996L, adapter.lastBlockInfo?.timestamp)
    }

    @Test
    fun lastBlockInfo_mwebMode_usesLatestMwebUtxosHeight() {
        val adapter = createMwebAdapter()

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", 3_104_327, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(MwebSyncState(3_104_330, 3_104_330, 3_104_330))

        adapter.onMwebSyncStateUpdate(MwebSyncState(3_104_333, 3_104_333, 3_104_333))

        assertEquals(3_104_333, adapter.lastBlockInfo?.height)
        assertEquals(1_778_315_996L, adapter.lastBlockInfo?.timestamp)
    }

    @Test
    fun lastBlockInfo_mwebModeBeforeSyncCallback_usesStoredMwebUtxosHeight() {
        val adapter = createMwebAdapter()

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", 3_104_327, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(MwebSyncState(3_104_333, 3_104_333, 3_104_333))

        assertEquals(3_104_333, adapter.lastBlockInfo?.height)
    }

    @Test
    fun lastBlockInfo_mwebModeZeroUtxosHeight_fallsBackToPublicHeight() {
        val adapter = createMwebAdapter()

        every { kit.lastBlockInfo } returns BlockInfo("public-hash", 3_104_327, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(MwebSyncState(3_104_333, 3_104_333, 0))

        assertEquals(3_104_327, adapter.lastBlockInfo?.height)
        assertEquals(1_778_315_996L, adapter.lastBlockInfo?.timestamp)
    }

    @Test
    fun getTransactions_mwebConfirmedHeightReachedThreshold_reportsCompleted() = runTest {
        val adapter = createMwebAdapter()
        every { kit.lastBlockInfo } returns BlockInfo("public-hash", 100, 1_778_315_996L)
        every { kit.mwebState } returns mwebState(
            MwebSyncState(102, 102, 102),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-outgoing:confirmed-pegout",
                    type = MwebTransactionType.Outgoing,
                    kind = MwebTransactionKind.MwebToPublic,
                    amount = 184_700,
                    fee = 462,
                    address = PUBLIC_ADDRESS,
                    transactionHash = "confirmed-pegout-hash",
                    outputIds = emptyList(),
                    inputOutputIds = listOf("spent-output"),
                    timestamp = 10_000,
                    height = 100,
                    pending = false
                )
            )
        )

        val record = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        ).single()

        assertEquals(RecordTransactionStatus.Completed, record.status(adapter.lastBlockInfo?.height))
    }

    @Test
    fun onMwebSyncStateUpdate_mwebUtxosHeightChanged_emitsLastBlockUpdate() {
        val adapter = createMwebAdapter()
        val observer = adapter.lastBlockUpdatedFlowable.test()

        adapter.onMwebSyncStateUpdate(MwebSyncState(1_000, 1_000, 1_000))
        observer.assertValueCount(1)

        adapter.onMwebSyncStateUpdate(MwebSyncState(1_001, 1_001, 1_000))
        observer.assertValueCount(1)

        adapter.onMwebSyncStateUpdate(MwebSyncState(1_001, 1_001, 1_001))
        observer.assertValueCount(2)
    }

    @Test
    fun availableBalance_mwebFullBalancePreviewFails_calculatesInBackgroundAndReturnsCachedValue() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createMwebAdapter(TestDispatcherProvider(dispatcher, this))
        val filters = UtxoFilters()
        var balanceUpdates = 0
        val balanceUpdatesJob = launch {
            adapter.balanceUpdatedFlow.collect {
                balanceUpdates += 1
            }
        }
        advanceUntilIdle()

        every { kit.litecoinBalance } returns LitecoinBalance(
            publicSpendable = 0,
            publicUnspendable = 0,
            mweb = MwebBalance(confirmed = 1_000, unconfirmed = 0)
        )
        every {
            kit.sendInfo(
                value = 1_000,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } throws MwebError.InsufficientFunds()
        every {
            kit.sendInfo(
                value = 1,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(totalFee = 100)
        every {
            kit.sendInfo(
                value = 900,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(totalFee = 100)

        val availableBalance = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals(BigDecimal.ZERO, availableBalance)
        verify(exactly = 0) {
            kit.sendInfo(
                value = 1_000,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }

        advanceUntilIdle()

        val cachedAvailableBalance = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals(BigDecimal("0.00000900"), cachedAvailableBalance)
        assertEquals(1, balanceUpdates)
        verify(exactly = 1) {
            kit.sendInfo(
                value = 1_000,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }
        balanceUpdatesJob.cancel()
    }

    @Test
    fun availableBalance_mwebDryRunReturnsZero_doesNotCacheZeroAndRecalculatesOnNextCall() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createMwebAdapter(TestDispatcherProvider(dispatcher, this))
        val filters = UtxoFilters()
        val balanceUpdatesJob = launch { adapter.balanceUpdatedFlow.collect { } }
        advanceUntilIdle()

        every { kit.litecoinBalance } returns LitecoinBalance(
            publicSpendable = 0,
            publicUnspendable = 0,
            mweb = MwebBalance(confirmed = 1_000, unconfirmed = 0)
        )
        // Dry-run is unable to spend anything to this destination (e.g. peg-out maturity
        // not yet reached) -> every sendInfo throws -> calculated max == 0.
        every {
            kit.sendInfo(
                value = any(),
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } throws MwebError.InsufficientFunds()

        val first = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )
        assertEquals(BigDecimal.ZERO, first)
        advanceUntilIdle()

        // After the first async DONE the result is 0 -> we expect it NOT to be cached.
        // The next call must MISS again and trigger a fresh dry-run.
        val second = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )
        assertEquals(BigDecimal.ZERO, second)
        advanceUntilIdle()

        verify(atLeast = 2) {
            kit.sendInfo(
                value = 1_000,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }
        balanceUpdatesJob.cancel()
    }

    @Test
    fun onMwebSyncStateUpdate_mwebUtxosHeightChanged_invalidatesAvailableBalanceCache() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createMwebAdapter(TestDispatcherProvider(dispatcher, this))
        val filters = UtxoFilters()
        val balanceUpdatesJob = launch { adapter.balanceUpdatedFlow.collect { } }
        advanceUntilIdle()

        every { kit.litecoinBalance } returns LitecoinBalance(
            publicSpendable = 0,
            publicUnspendable = 0,
            mweb = MwebBalance(confirmed = 1_000, unconfirmed = 0)
        )
        every {
            kit.sendInfo(
                value = 1_000,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } throws MwebError.InsufficientFunds()
        every {
            kit.sendInfo(
                value = 1,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(totalFee = 100)
        every {
            kit.sendInfo(
                value = 900,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(totalFee = 100)

        adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )
        advanceUntilIdle()

        // Sanity: value is now cached.
        val cached = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )
        assertEquals(BigDecimal("0.00000900"), cached)

        // Tip advances -> existing cache entries become stale and must be invalidated.
        adapter.onMwebSyncStateUpdate(MwebSyncState(1_001, 1_001, 1_001))

        val afterTip = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )
        // Cache was cleared by the tip change -> first call after invalidation is a MISS.
        assertEquals(BigDecimal.ZERO, afterTip)
        advanceUntilIdle()

        // Recalculated.
        verify(atLeast = 2) {
            kit.sendInfo(
                value = 1_000,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }
        balanceUpdatesJob.cancel()
    }

    @Test
    fun getTransactions_mwebTransactions_mapsIncomingAndOutgoing() = runTest {
        val adapter = createMwebAdapter()
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = mwebHistoryTransactions()
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        assertEquals(listOf("mweb-outgoing:outgoing-hash", "mweb-incoming:receive-output"), records.map { it.uid })
        val outgoing = records[0] as BitcoinTransactionRecord
        val incoming = records[1] as BitcoinTransactionRecord
        assertEquals(TransactionRecordType.BITCOIN_OUTGOING, outgoing.transactionRecordType)
        assertEquals(BigDecimal("-0.00000500"), (outgoing.mainValue as TransactionValue.CoinValue).value)
        assertEquals("outgoing-hash", outgoing.transactionHash)
        assertEquals(listOf("ltc1destination"), outgoing.to)
        assertEquals(BigDecimal("0.00000010"), (outgoing.fee as TransactionValue.CoinValue).value)
        assertFalse(outgoing.showRawTransaction)
        assertEquals(TransactionRecordType.BITCOIN_INCOMING, incoming.transactionRecordType)
        assertEquals(BigDecimal("0.00001000"), (incoming.mainValue as TransactionValue.CoinValue).value)
        assertEquals("receive-output", incoming.transactionHash)
        assertEquals(listOf("ltcmweb1receive"), incoming.to)
    }

    @Test
    fun getTransactions_mwebOutgoingFilter_returnsOnlyOutgoing() = runTest {
        val adapter = createMwebAdapter()
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = mwebHistoryTransactions()
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.Outgoing,
            address = null
        )

        assertEquals(listOf("mweb-outgoing:outgoing-hash"), records.map { it.uid })
    }

    @Test
    fun getTransactionExplorerData_mwebConfirmedPegIn_returnsBlockchairAndMwebExplorer() = runTest {
        val adapter = createMwebAdapter()
        val transaction = mwebTransaction(
            uid = "mweb-incoming:output-id",
            type = MwebTransactionType.Incoming,
            kind = MwebTransactionKind.PublicToMweb,
            amount = 1_000,
            fee = null,
            address = "ltcmweb1receive",
            transactionHash = "peg-in-hash",
            outputIds = listOf("output-id"),
            timestamp = 10_000,
            height = 900,
            pending = false
        )
        every { kit.mwebState } returns mwebState(MwebSyncState(900, 900, 900), transactions = listOf(transaction))

        val record = adapter.getTransactions(
            from = null,
            token = null,
            limit = 1,
            transactionType = FilterTransactionType.All,
            address = null
        ).first()

        assertEquals(
            listOf(
                TransactionExplorerData("blockchair.com", "https://blockchair.com/litecoin/transaction/peg-in-hash"),
                TransactionExplorerData("mwebexplorer.com", "https://www.mwebexplorer.com/blocks/block/900")
            ),
            adapter.getTransactionExplorerData(record)
        )
    }

    @Test
    fun getTransactionExplorerData_mwebPendingPegIn_returnsBlockchairOnly() = runTest {
        val adapter = createMwebAdapter()
        val transaction = mwebTransaction(
            uid = "mweb-outgoing:peg-in-hash",
            type = MwebTransactionType.Outgoing,
            kind = MwebTransactionKind.PublicToMweb,
            amount = 1_000,
            fee = 10,
            address = "ltcmweb1receive",
            transactionHash = "peg-in-hash",
            outputIds = listOf("output-id"),
            timestamp = 10_000,
            height = null,
            pending = true
        )
        every { kit.mwebState } returns mwebState(MwebSyncState(0, 0, 0), transactions = listOf(transaction))

        val record = adapter.getTransactions(
            from = null,
            token = null,
            limit = 1,
            transactionType = FilterTransactionType.All,
            address = null
        ).first()

        assertEquals(
            listOf(TransactionExplorerData("blockchair.com", "https://blockchair.com/litecoin/transaction/peg-in-hash")),
            adapter.getTransactionExplorerData(record)
        )
    }

    @Test
    fun getTransactionExplorerData_mwebOutputOnly_returnsMwebExplorerOnlyWhenConfirmed() = runTest {
        val adapter = createMwebAdapter()
        val transaction = mwebTransaction(
            uid = "mweb-incoming:output-id",
            type = MwebTransactionType.Incoming,
            kind = MwebTransactionKind.Incoming,
            amount = 1_000,
            fee = null,
            address = "ltcmweb1receive",
            transactionHash = null,
            outputIds = listOf("output-id"),
            timestamp = 10_000,
            height = 900,
            pending = false
        )
        every { kit.mwebState } returns mwebState(MwebSyncState(900, 900, 900), transactions = listOf(transaction))

        val record = adapter.getTransactions(
            from = null,
            token = null,
            limit = 1,
            transactionType = FilterTransactionType.All,
            address = null
        ).first()

        assertEquals(
            listOf(TransactionExplorerData("mwebexplorer.com", "https://www.mwebexplorer.com/blocks/block/900")),
            adapter.getTransactionExplorerData(record)
        )
    }

    @Test
    fun getTransactionExplorerData_mwebOutputOnlyPending_returnsEmptyList() = runTest {
        val adapter = createMwebAdapter()
        val transaction = mwebTransaction(
            uid = "mweb-incoming:output-id",
            type = MwebTransactionType.Incoming,
            kind = MwebTransactionKind.Incoming,
            amount = 1_000,
            fee = null,
            address = "ltcmweb1receive",
            transactionHash = null,
            outputIds = listOf("output-id"),
            timestamp = 10_000,
            height = null,
            pending = true
        )
        every { kit.mwebState } returns mwebState(MwebSyncState(0, 0, 0), transactions = listOf(transaction))

        val record = adapter.getTransactions(
            from = null,
            token = null,
            limit = 1,
            transactionType = FilterTransactionType.All,
            address = null
        ).first()

        assertEquals(emptyList<TransactionExplorerData>(), adapter.getTransactionExplorerData(record))
    }

    @Test
    fun getTransactionExplorerData_mwebBlankCanonicalHash_ignoresBlockchair() = runTest {
        val adapter = createMwebAdapter()
        val transaction = mwebTransaction(
            uid = "mweb-incoming:output-id",
            type = MwebTransactionType.Incoming,
            kind = MwebTransactionKind.PublicToMweb,
            amount = 1_000,
            fee = null,
            address = "ltcmweb1receive",
            transactionHash = "",
            outputIds = listOf("output-id"),
            timestamp = 10_000,
            height = 900,
            pending = false
        )
        every { kit.mwebState } returns mwebState(MwebSyncState(900, 900, 900), transactions = listOf(transaction))

        val record = adapter.getTransactions(
            from = null,
            token = null,
            limit = 1,
            transactionType = FilterTransactionType.All,
            address = null
        ).first()

        assertEquals("output-id", record.transactionHash)
        assertEquals(
            listOf(TransactionExplorerData("mwebexplorer.com", "https://www.mwebexplorer.com/blocks/block/900")),
            adapter.getTransactionExplorerData(record)
        )
    }

    @Test
    fun send_mwebToPublicMissingCanonicalHash_returnsLocalTransactionUid() = runTest {
        val adapter = createMwebAdapter()
        val filters = UtxoFilters()
        every { kit.mwebState } returns mwebState(
            MwebSyncState(900, 900, 900),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-outgoing:1778417318",
                    type = MwebTransactionType.Outgoing,
                    kind = MwebTransactionKind.MwebToPublic,
                    amount = 100,
                    fee = 1,
                    address = PUBLIC_ADDRESS,
                    transactionHash = null,
                    outputIds = emptyList(),
                    timestamp = 1_778_417_318,
                    height = null,
                    pending = true
                )
            )
        )
        stubMwebSendResult(
            address = PUBLIC_ADDRESS,
            source = LitecoinSendSource.Mweb,
            filters = filters,
            result = MwebSendResult(
                canonicalTransactionHash = null,
                rawTransaction = byteArrayOf(1, 2, 3),
                outputIds = emptyList()
            )
        )

        val transactionHash = adapter.send(
            amount = BigDecimal("0.00000100"),
            address = PUBLIC_ADDRESS,
            memo = null,
            feeRate = 1,
            unspentOutputs = null,
            pluginData = null,
            transactionSorting = null,
            rbfEnabled = false,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals("mweb-outgoing:1778417318", transactionHash)
    }

    @Test
    fun send_mwebToPublicMultipleLocalMatches_returnsClosestTransactionUid() = runTest {
        val adapter = createMwebAdapter()
        val filters = UtxoFilters()
        val sentAt = System.currentTimeMillis() / 1000
        every { kit.mwebState } returns mwebState(
            MwebSyncState(900, 900, 900),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-outgoing:older",
                    type = MwebTransactionType.Outgoing,
                    kind = MwebTransactionKind.MwebToPublic,
                    amount = 100,
                    fee = 1,
                    address = PUBLIC_ADDRESS,
                    transactionHash = null,
                    outputIds = emptyList(),
                    timestamp = sentAt - 300,
                    height = null,
                    pending = true
                ),
                mwebTransaction(
                    uid = "mweb-outgoing:current",
                    type = MwebTransactionType.Outgoing,
                    kind = MwebTransactionKind.MwebToPublic,
                    amount = 100,
                    fee = 1,
                    address = PUBLIC_ADDRESS,
                    transactionHash = null,
                    outputIds = emptyList(),
                    timestamp = sentAt,
                    height = null,
                    pending = true
                )
            )
        )
        stubMwebSendResult(
            address = PUBLIC_ADDRESS,
            source = LitecoinSendSource.Mweb,
            filters = filters,
            result = MwebSendResult(
                canonicalTransactionHash = null,
                rawTransaction = byteArrayOf(1, 2, 3),
                outputIds = emptyList()
            )
        )

        val transactionHash = adapter.send(
            amount = BigDecimal("0.00000100"),
            address = PUBLIC_ADDRESS,
            memo = null,
            feeRate = 1,
            unspentOutputs = null,
            pluginData = null,
            transactionSorting = null,
            rbfEnabled = false,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals("mweb-outgoing:current", transactionHash)
    }

    @Test
    fun send_mwebMissingHashAndOutputIdsWithoutLocalRecord_returnsRawTransactionFallback() = runTest {
        val adapter = createMwebAdapter()
        val filters = UtxoFilters()
        stubMwebSendResult(
            address = PUBLIC_ADDRESS,
            source = LitecoinSendSource.Mweb,
            filters = filters,
            result = MwebSendResult(
                canonicalTransactionHash = null,
                rawTransaction = byteArrayOf(1, 2, 3),
                outputIds = emptyList()
            )
        )

        val transactionHash = adapter.send(
            amount = BigDecimal("0.00000100"),
            address = PUBLIC_ADDRESS,
            memo = null,
            feeRate = 1,
            unspentOutputs = null,
            pluginData = null,
            transactionSorting = null,
            rbfEnabled = false,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals(
            "mweb-local:039058c6f2c0cb492c533b0a4d14ef77cc0f78abccced5287d84a1a2011cfb81",
            transactionHash
        )
    }

    @Test
    fun send_mwebToMwebWithoutCanonicalHash_returnsOutputId() = runTest {
        val adapter = createMwebAdapter()
        val filters = UtxoFilters()
        stubMwebSendResult(
            address = MWEB_ADDRESS,
            source = LitecoinSendSource.Mweb,
            filters = filters,
            result = MwebSendResult(
                canonicalTransactionHash = null,
                rawTransaction = byteArrayOf(1, 2, 3),
                outputIds = listOf("created-output-id")
            )
        )

        val transactionHash = adapter.send(
            amount = BigDecimal("0.00000100"),
            address = MWEB_ADDRESS,
            memo = null,
            feeRate = 1,
            unspentOutputs = null,
            pluginData = null,
            transactionSorting = null,
            rbfEnabled = false,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals("created-output-id", transactionHash)
    }

    private fun stubMwebSendResult(
        address: String,
        source: LitecoinSendSource,
        filters: UtxoFilters,
        result: MwebSendResult,
        rbfEnabled: Boolean = false,
        changeToFirstInput: Boolean = false
    ) {
        coEvery {
            kit.send(
                address = address,
                memo = null,
                value = 100,
                source = source,
                feeRate = 1,
                sortType = TransactionDataSortType.Shuffle,
                unspentOutputs = null,
                pluginData = emptyMap(),
                rbfEnabled = rbfEnabled,
                changeToFirstInput = changeToFirstInput,
                filters = filters
            )
        } returns LitecoinSendResult.Mweb(result)
    }

    @Test
    fun bitcoinFeeInfo_mwebPublicDestination_calculatesInBackgroundAndUsesMwebSource() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createMwebAdapter(TestDispatcherProvider(dispatcher, this))
        val filters = UtxoFilters()
        every {
            kit.sendInfo(
                value = 100,
                address = PUBLIC_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(totalFee = 10)

        val initialFeeInfo = adapter.bitcoinFeeInfo(
            amount = BigDecimal("0.00000100"),
            feeRate = 1,
            address = PUBLIC_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            filters = filters
        )

        assertEquals(null, initialFeeInfo)
        verify(exactly = 0) {
            kit.sendInfo(
                value = 100,
                address = PUBLIC_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }

        advanceUntilIdle()

        val feeInfo = adapter.bitcoinFeeInfo(
            amount = BigDecimal("0.00000100"),
            feeRate = 1,
            address = PUBLIC_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            filters = filters
        )

        assertEquals(BigDecimal("0.00000010"), feeInfo?.fee)
        verify(exactly = 1) {
            kit.sendInfo(
                value = 100,
                address = PUBLIC_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }
    }

    @Test
    fun send_mwebPublicDestination_usesMwebSource() = runTest {
        val adapter = createMwebAdapter()
        val filters = UtxoFilters()
        coEvery {
            kit.send(
                address = PUBLIC_ADDRESS,
                memo = null,
                value = 100,
                source = LitecoinSendSource.Mweb,
                feeRate = 1,
                sortType = TransactionDataSortType.Shuffle,
                unspentOutputs = null,
                pluginData = emptyMap(),
                rbfEnabled = false,
                changeToFirstInput = false,
                filters = filters
            )
        } returns LitecoinSendResult.Mweb(
            MwebSendResult(
                canonicalTransactionHash = "mweb-to-public-hash",
                rawTransaction = byteArrayOf(1),
                outputIds = emptyList()
            )
        )

        val transactionHash = adapter.send(
            amount = BigDecimal("0.00000100"),
            address = PUBLIC_ADDRESS,
            memo = null,
            feeRate = 1,
            unspentOutputs = null,
            pluginData = null,
            transactionSorting = null,
            rbfEnabled = true,
            changeToFirstInput = true,
            utxoFilters = filters
        )

        assertEquals("mweb-to-public-hash", transactionHash)
    }

    @Test
    fun firstAddress_mwebToken_throwsUnsupportedAccountException() {
        assertFailsWith<UnsupportedAccountException> {
            LitecoinAdapter.firstAddress(wallet(TokenType.Mweb).account.type, TokenType.Mweb)
        }
    }

    @Test
    fun validate_publicMwebDestinationWithoutMwebEngine_acceptsAddress() {
        val adapter = createPublicAdapter()
        every { kit.isMwebAddress(MWEB_ADDRESS) } returns true

        adapter.validate(MWEB_ADDRESS, null)

        verify(exactly = 0) { kit.validateAddress(any(), any()) }
    }

    @Test
    fun validate_publicInvalidNonMwebDestination_delegatesToPublicValidation() {
        val adapter = createPublicAdapter()
        every { kit.isMwebAddress(INVALID_ADDRESS) } returns false
        every { kit.validateAddress(INVALID_ADDRESS, emptyMap()) } throws AddressFormatException("invalid")

        assertFailsWith<AddressFormatException> {
            adapter.validate(INVALID_ADDRESS, null)
        }
    }

    @Test
    fun getTransactions_publicWithoutMwebTransactions_keepsPublicAmount() = runTest {
        val adapter = createPublicAdapter()
        val publicTransaction = transactionInfo(
            uid = "public-send",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = PUBLIC_ADDRESS,
            timestamp = 10_000
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicTransaction))
        every { kit.mwebState } returns null

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        val record = records.single() as BitcoinTransactionRecord
        assertEquals(BigDecimal("-0.03509574"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun getTransactions_publicAdapterStoppedBeforeMwebAdjustment_keepsPublicRecords() = runTest {
        val adapter = createPublicAdapter()
        val publicTransaction = transactionInfo(
            uid = "public-send",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = PUBLIC_ADDRESS,
            timestamp = 10_000
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicTransaction))
        every { kit.mwebState } throws IllegalStateException("MWEB engine handle is released")

        adapter.stop()

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        val record = records.single() as BitcoinTransactionRecord
        assertEquals(BigDecimal("-0.03509574"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun getTransactions_mwebAdapterStoppedBeforeRecordsLoad_returnsEmptyRecords() = runTest {
        val adapter = createMwebAdapter()
        every { kit.mwebState } throws IllegalStateException("MWEB engine handle is released")

        adapter.stop()

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        assertTrue(records.isEmpty())
    }

    @Test
    fun getTransactions_publicMwebPegIn_usesMwebAmount() = runTest {
        val adapter = createPublicAdapter()
        val publicTransaction = transactionInfo(
            uid = "public-pegin",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = null,
            timestamp = 10_000
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicTransaction))
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-incoming:mweb-hash",
                    type = MwebTransactionType.Incoming,
                    kind = MwebTransactionKind.PublicToMweb,
                    amount = 882_613,
                    fee = 1_000,
                    address = MWEB_ADDRESS,
                    transactionHash = "mweb-hash",
                    outputIds = listOf("output-id"),
                    timestamp = 10_004,
                    height = 900,
                    pending = false
                )
            )
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        val record = records.single() as BitcoinTransactionRecord
        assertEquals(BigDecimal("-0.00882613"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun getTransactions_publicMwebPegInWithChange_usesMwebAmountAndHidesChange() = runTest {
        val adapter = createPublicAdapter()
        val publicPegIn = transactionInfo(
            uid = "public-pegin",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = null,
            timestamp = 10_000
        )
        val publicChange = transactionInfo(
            uid = "public-change",
            transactionHash = "public-change-hash",
            amount = 2_626_961,
            toAddress = PUBLIC_CHANGE_ADDRESS,
            timestamp = 10_003,
            type = TransactionType.Incoming,
            outputMine = true,
            changeOutput = true
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicPegIn, publicChange))
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-incoming:mweb-hash",
                    type = MwebTransactionType.Incoming,
                    kind = MwebTransactionKind.PublicToMweb,
                    amount = 882_613,
                    fee = 1_000,
                    address = MWEB_ADDRESS,
                    transactionHash = "mweb-hash",
                    outputIds = listOf("output-id"),
                    timestamp = 10_004,
                    height = 900,
                    pending = false
                )
            )
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        val record = records.single() as BitcoinTransactionRecord
        assertEquals("public-pegin", record.uid)
        assertEquals(BigDecimal("-0.00882613"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun getTransactions_publicMwebPegInBeforeMwebRecord_usesNetPublicAmountAndHidesChange() = runTest {
        val adapter = createPublicAdapter()
        val publicPegIn = transactionInfo(
            uid = "public-pegin",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = null,
            timestamp = 10_000
        )
        val publicChange = transactionInfo(
            uid = "public-change",
            transactionHash = "public-change-hash",
            amount = 2_626_961,
            toAddress = PUBLIC_CHANGE_ADDRESS,
            timestamp = 10_003,
            type = TransactionType.Incoming,
            outputMine = true,
            changeOutput = true
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicPegIn, publicChange))
        every { kit.mwebState } returns null

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        val record = records.single() as BitcoinTransactionRecord
        assertEquals("public-pegin", record.uid)
        assertEquals(BigDecimal("-0.00882613"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun getTransactions_publicMwebPegInMarkerWithoutChangeOrMweb_keepsPublicAmount() = runTest {
        val adapter = createPublicAdapter()
        val publicPegIn = transactionInfo(
            uid = "public-pegin",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = null,
            timestamp = 10_000
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicPegIn))
        every { kit.mwebState } returns null

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        val record = records.single() as BitcoinTransactionRecord
        assertEquals("public-pegin", record.uid)
        assertEquals(BigDecimal("-0.03509574"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun getTransactions_publicMultipleMwebPegInMarkers_reusesMwebRecordOnce() = runTest {
        val adapter = createPublicAdapter()
        val firstPegIn = transactionInfo(
            uid = "public-pegin-a",
            transactionHash = "public-hash-a",
            amount = 4_000_000,
            toAddress = null,
            timestamp = 10_000
        )
        val secondPegIn = transactionInfo(
            uid = "public-pegin-b",
            transactionHash = "public-hash-b",
            amount = 2_000_000,
            toAddress = null,
            timestamp = 10_001
        )
        val firstChange = transactionInfo(
            uid = "public-change-a",
            transactionHash = "public-change-hash-a",
            amount = 3_000_000,
            toAddress = PUBLIC_CHANGE_ADDRESS,
            timestamp = 10_002,
            type = TransactionType.Incoming,
            outputMine = true,
            changeOutput = true
        )
        val secondChange = transactionInfo(
            uid = "public-change-b",
            transactionHash = "public-change-hash-b",
            amount = 1_500_000,
            toAddress = PUBLIC_CHANGE_ADDRESS,
            timestamp = 10_003,
            type = TransactionType.Incoming,
            outputMine = true,
            changeOutput = true
        )
        every { kit.transactions(null, null, 10) } returns Single.just(
            listOf(firstPegIn, secondPegIn, firstChange, secondChange)
        )
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-incoming:mweb-hash",
                    type = MwebTransactionType.Incoming,
                    kind = MwebTransactionKind.PublicToMweb,
                    amount = 1_000_000,
                    fee = 1_000,
                    address = MWEB_ADDRESS,
                    transactionHash = "mweb-hash",
                    outputIds = listOf("output-id"),
                    timestamp = 10_004,
                    height = 900,
                    pending = false
                )
            )
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        assertEquals(listOf("public-pegin-a", "public-pegin-b"), records.map { it.uid })
        assertEquals(
            listOf(BigDecimal("-0.01000000"), BigDecimal("-0.00500000")),
            records.map { (it.mainValue as TransactionValue.CoinValue).value }
        )
    }

    @Test
    fun getTransactions_publicMwebPegInChangeTie_prefersClosestNetAmount() = runTest {
        val adapter = createPublicAdapter()
        val firstPegIn = transactionInfo(
            uid = "public-pegin-a",
            transactionHash = "public-hash-a",
            amount = 5_000_000,
            toAddress = null,
            timestamp = 10_000
        )
        val secondPegIn = transactionInfo(
            uid = "public-pegin-b",
            transactionHash = "public-hash-b",
            amount = 4_000_000,
            toAddress = null,
            timestamp = 10_001
        )
        val secondChange = transactionInfo(
            uid = "public-change-b",
            transactionHash = "public-change-hash-b",
            amount = 3_500_000,
            toAddress = PUBLIC_CHANGE_ADDRESS,
            timestamp = 10_002,
            type = TransactionType.Incoming,
            outputMine = true,
            changeOutput = true
        )
        val firstChange = transactionInfo(
            uid = "public-change-a",
            transactionHash = "public-change-hash-a",
            amount = 4_000_000,
            toAddress = PUBLIC_CHANGE_ADDRESS,
            timestamp = 10_002,
            type = TransactionType.Incoming,
            outputMine = true,
            changeOutput = true
        )
        every { kit.transactions(null, null, 10) } returns Single.just(
            listOf(firstPegIn, secondPegIn, secondChange, firstChange)
        )
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-incoming:mweb-hash",
                    type = MwebTransactionType.Incoming,
                    kind = MwebTransactionKind.PublicToMweb,
                    amount = 1_000_000,
                    fee = 1_000,
                    address = MWEB_ADDRESS,
                    transactionHash = "mweb-hash",
                    outputIds = listOf("output-id"),
                    timestamp = 10_004,
                    height = 900,
                    pending = false
                )
            )
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        assertEquals(listOf("public-pegin-a", "public-pegin-b"), records.map { it.uid })
        assertEquals(
            listOf(BigDecimal("-0.01000000"), BigDecimal("-0.00500000")),
            records.map { (it.mainValue as TransactionValue.CoinValue).value }
        )
    }

    @Test
    fun getTransactions_publicMwebChangeWithMwebRecord_hidesChangeWithoutMarker() = runTest {
        val adapter = createPublicAdapter()
        val publicChange = transactionInfo(
            uid = "public-change",
            transactionHash = "public-change-hash",
            amount = 2_626_961,
            toAddress = PUBLIC_CHANGE_ADDRESS,
            timestamp = 10_003,
            type = TransactionType.Incoming,
            outputMine = true,
            changeOutput = true
        )
        every { kit.transactions(null, TransactionFilterType.Incoming, 10) } returns Single.just(listOf(publicChange))
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-incoming:mweb-hash",
                    type = MwebTransactionType.Incoming,
                    kind = MwebTransactionKind.PublicToMweb,
                    amount = 882_613,
                    fee = 1_000,
                    address = MWEB_ADDRESS,
                    transactionHash = "mweb-hash",
                    outputIds = listOf("output-id"),
                    timestamp = 10_004,
                    height = 900,
                    pending = false
                )
            )
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.Incoming,
            address = null
        )

        assertTrue(records.isEmpty())
    }

    @Test
    fun getTransactions_publicRegularIncomingNearMwebPegIn_keepsIncoming() = runTest {
        val adapter = createPublicAdapter()
        val publicPegIn = transactionInfo(
            uid = "public-pegin",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = null,
            timestamp = 10_000
        )
        val regularIncoming = transactionInfo(
            uid = "regular-incoming",
            transactionHash = "regular-incoming-hash",
            amount = 2_626_961,
            toAddress = PUBLIC_ADDRESS,
            timestamp = 10_003,
            type = TransactionType.Incoming,
            outputMine = true
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicPegIn, regularIncoming))
        every { kit.mwebState } returns null

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        assertEquals(listOf("public-pegin", "regular-incoming"), records.map { it.uid })
    }

    @Test
    fun onTransactionsUpdate_publicMwebPegIn_emitsMwebAmount() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createPublicAdapter(TestDispatcherProvider(dispatcher, this))
        val publicTransaction = transactionInfo(
            uid = "public-pegin",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = null,
            timestamp = 10_000
        )
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-incoming:mweb-hash",
                    type = MwebTransactionType.Incoming,
                    kind = MwebTransactionKind.PublicToMweb,
                    amount = 882_613,
                    fee = 1_000,
                    address = MWEB_ADDRESS,
                    transactionHash = "mweb-hash",
                    outputIds = listOf("output-id"),
                    timestamp = 10_004,
                    height = 900,
                    pending = false
                )
            )
        )
        val recordsDeferred = async {
            adapter.getTransactionRecordsFlow(
                token = null,
                transactionType = FilterTransactionType.All,
                address = null
            ).first()
        }
        advanceUntilIdle()

        adapter.onTransactionsUpdate(inserted = listOf(publicTransaction), updated = emptyList())
        verify(exactly = 0) { kit.mwebState }
        advanceUntilIdle()

        val record = recordsDeferred.await().single() as BitcoinTransactionRecord
        assertEquals(BigDecimal("-0.00882613"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun onMwebUtxosUpdate_publicMode_doesNotEmitEmptyTransactionList() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createPublicAdapter(TestDispatcherProvider(dispatcher, this))
        val emissions = mutableListOf<List<Any>>()
        val job = launch {
            adapter.getTransactionRecordsFlow(
                token = null,
                transactionType = FilterTransactionType.All,
                address = null
            ).collect { records ->
                emissions.add(records)
            }
        }
        advanceUntilIdle()

        adapter.onMwebUtxosUpdate(emptyList())
        advanceUntilIdle()

        assertTrue(emissions.isEmpty())
        job.cancel()
    }

    @Test
    fun getTransactions_publicRegularOutgoingWithMwebPegIn_keepsPublicAmount() = runTest {
        val adapter = createPublicAdapter()
        val publicTransaction = transactionInfo(
            uid = "public-send",
            transactionHash = "public-hash",
            amount = 3_509_574,
            toAddress = PUBLIC_ADDRESS,
            timestamp = 10_000
        )
        every { kit.transactions(null, null, 10) } returns Single.just(listOf(publicTransaction))
        every { kit.mwebState } returns mwebState(
            MwebSyncState(1_000, 1_000, 1_000),
            transactions = listOf(
                mwebTransaction(
                    uid = "mweb-incoming:mweb-hash",
                    type = MwebTransactionType.Incoming,
                    kind = MwebTransactionKind.PublicToMweb,
                    amount = 882_613,
                    fee = 1_000,
                    address = MWEB_ADDRESS,
                    transactionHash = "mweb-hash",
                    outputIds = listOf("output-id"),
                    timestamp = 10_004,
                    height = 900,
                    pending = false
                )
            )
        )

        val records = adapter.getTransactions(
            from = null,
            token = null,
            limit = 10,
            transactionType = FilterTransactionType.All,
            address = null
        )

        val record = records.single() as BitcoinTransactionRecord
        assertEquals(BigDecimal("-0.03509574"), (record.mainValue as TransactionValue.CoinValue).value)
    }

    @Test
    fun bitcoinFeeInfo_publicMwebDestination_calculatesInBackgroundAndUsesPublicMwebSource() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createPublicAdapter(TestDispatcherProvider(dispatcher, this))
        val filters = UtxoFilters()
        val selectedTransactionHash = byteArrayOf(1, 2, 3)
        val selectedInfo = unspentOutputInfo(
            transactionHash = selectedTransactionHash,
            outputIndex = 1
        )

        every { kit.isMwebAddress(MWEB_ADDRESS) } returns true
        every {
            kit.sendInfo(
                value = 100,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Public,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(
            totalFee = 10,
            selectedPublicUtxos = listOf(selectedInfo)
        )

        val initialFeeInfo = adapter.bitcoinFeeInfo(
            amount = BigDecimal("0.00000100"),
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            filters = filters
        )

        assertEquals(null, initialFeeInfo)
        verify(exactly = 0) {
            kit.sendInfo(
                value = 100,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Public,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }

        advanceUntilIdle()

        val feeInfo = adapter.bitcoinFeeInfo(
            amount = BigDecimal("0.00000100"),
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            filters = filters
        )

        assertEquals(BigDecimal("0.00000010"), feeInfo?.fee)
        assertEquals(1, feeInfo?.selectedUtxoCount)
        verify(exactly = 1) {
            kit.sendInfo(
                value = 100,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Public,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        }
    }

    @Test
    fun availableBalance_publicMwebDestination_calculatesInBackgroundAndReturnsCachedValue() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val adapter = createPublicAdapter(
            dispatcherProvider = TestDispatcherProvider(dispatcher, this)
        )
        val filters = UtxoFilters()
        var balanceUpdates = 0
        val balanceUpdatesJob = launch {
            adapter.balanceUpdatedFlow.collect {
                balanceUpdates += 1
            }
        }
        advanceUntilIdle()

        every { kit.isMwebAddress(MWEB_ADDRESS) } returns true
        every { kit.litecoinBalance } returns LitecoinBalance(
            publicSpendable = 1_000,
            publicUnspendable = 0,
            mweb = null
        )
        every {
            kit.sendInfo(
                value = 1_000,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Public,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } throws MwebError.InsufficientFunds()
        every {
            kit.sendInfo(
                value = 1,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Public,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(totalFee = 100)
        every {
            kit.sendInfo(
                value = 900,
                address = MWEB_ADDRESS,
                memo = null,
                source = LitecoinSendSource.Public,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = emptyMap(),
                changeToFirstInput = false,
                filters = filters
            )
        } returns mwebSendInfo(totalFee = 100)

        val availableBalance = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals(BigDecimal.ZERO, availableBalance)
        advanceUntilIdle()

        val cachedAvailableBalance = adapter.availableBalance(
            feeRate = 1,
            address = MWEB_ADDRESS,
            memo = null,
            unspentOutputs = null,
            pluginData = null,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals(BigDecimal("0.00000900"), cachedAvailableBalance)
        assertEquals(1, balanceUpdates)
        balanceUpdatesJob.cancel()
    }

    @Test
    fun send_publicMwebDestination_usesPublicMwebSource() = runTest {
        val adapter = createPublicAdapter()
        val filters = UtxoFilters()
        every { kit.isMwebAddress(MWEB_ADDRESS) } returns true
        coEvery {
            kit.send(
                address = MWEB_ADDRESS,
                memo = null,
                value = 100,
                source = LitecoinSendSource.Public,
                feeRate = 1,
                sortType = TransactionDataSortType.Shuffle,
                unspentOutputs = null,
                pluginData = emptyMap(),
                rbfEnabled = true,
                changeToFirstInput = false,
                filters = filters
            )
        } returns LitecoinSendResult.Mweb(
            MwebSendResult(
                canonicalTransactionHash = "public-to-mweb-hash",
                rawTransaction = byteArrayOf(1),
                outputIds = listOf("output-id")
            )
        )

        val transactionHash = adapter.send(
            amount = BigDecimal("0.00000100"),
            address = MWEB_ADDRESS,
            memo = null,
            feeRate = 1,
            unspentOutputs = null,
            pluginData = null,
            transactionSorting = null,
            rbfEnabled = true,
            changeToFirstInput = false,
            utxoFilters = filters
        )

        assertEquals("public-to-mweb-hash", transactionHash)
    }

    @Test
    fun send_publicMwebDestinationAmountOverflow_throwsAndDoesNotCallKitSend() = runTest {
        val adapter = createPublicAdapter()
        val filters = UtxoFilters()
        every { kit.isMwebAddress(MWEB_ADDRESS) } returns true
        coEvery {
            kit.send(
                address = MWEB_ADDRESS,
                memo = null,
                value = any(),
                source = LitecoinSendSource.Public,
                feeRate = 1,
                sortType = TransactionDataSortType.Shuffle,
                unspentOutputs = null,
                pluginData = emptyMap(),
                rbfEnabled = true,
                changeToFirstInput = false,
                filters = filters
            )
        } returns LitecoinSendResult.Mweb(
            MwebSendResult(
                canonicalTransactionHash = "unexpected-hash",
                rawTransaction = byteArrayOf(1),
                outputIds = listOf("output-id")
            )
        )

        val error = assertFailsWith<LocalizedException> {
            adapter.send(
                amount = BigDecimal("92233720368.54775808"),
                address = MWEB_ADDRESS,
                memo = null,
                feeRate = 1,
                unspentOutputs = null,
                pluginData = null,
                transactionSorting = null,
                rbfEnabled = true,
                changeToFirstInput = false,
                utxoFilters = filters
            )
        }
        assertEquals(R.string.litecoin_mweb_invalid_amount, error.errorTextRes)
        coVerify(exactly = 0) {
            kit.send(
                address = MWEB_ADDRESS,
                memo = null,
                value = any(),
                source = LitecoinSendSource.Public,
                feeRate = 1,
                sortType = TransactionDataSortType.Shuffle,
                unspentOutputs = null,
                pluginData = emptyMap(),
                rbfEnabled = true,
                changeToFirstInput = false,
                filters = filters
            )
        }
    }

    private fun createMwebAdapter(
        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
    ): LitecoinAdapter {
        every { kit.litecoinBalance } returns LitecoinBalance(
            publicSpendable = 0,
            publicUnspendable = 0,
            mweb = MwebBalance(confirmed = 0, unconfirmed = 0)
        )
        every { kit.balance } returns BalanceInfo(0, 0, 0)
        every { kit.mwebState } returns mwebState(MwebSyncState(0, 0, 0))

        return LitecoinAdapter(
            kit = kit,
            syncMode = BitcoinCore.SyncMode.Api(),
            backgroundManager = backgroundManager,
            wallet = wallet(TokenType.Mweb),
            mode = LitecoinAdapter.Mode.Mweb,
            dispatcherProvider = dispatcherProvider,
            feeRateProvider = feeRateProvider
        )
    }

    private fun createPublicAdapter(
        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
    ): LitecoinAdapter {
        every { kit.litecoinBalance } returns LitecoinBalance(
            publicSpendable = 0,
            publicUnspendable = 0,
            mweb = null
        )
        every { kit.balance } returns BalanceInfo(0, 0, 0)

        return LitecoinAdapter(
            kit = kit,
            syncMode = BitcoinCore.SyncMode.Api(),
            backgroundManager = backgroundManager,
            wallet = wallet(TokenType.Derived(TokenType.Derivation.Bip84)),
            mode = LitecoinAdapter.Mode.Public(TokenType.Derivation.Bip84),
            dispatcherProvider = dispatcherProvider,
            feeRateProvider = feeRateProvider
        )
    }

    private fun wallet(tokenType: TokenType) = requireNotNull(
        WalletFactory(mockk<HardwareWalletTokenPolicy>(relaxed = true)).create(
            token = Token(
                coin = Coin("litecoin", "Litecoin", "LTC"),
                blockchain = Blockchain(BlockchainType.Litecoin, "Litecoin", null),
                type = tokenType,
                decimals = 8
            ),
            account = Account(
                id = "account-id",
                name = "Account",
                type = AccountType.Mnemonic(List(12) { "word$it" }, ""),
                origin = AccountOrigin.Created,
                level = 0,
                isBackedUp = false,
                isFileBackedUp = false
            ),
            hardwarePublicKey = null
        )
    )

    private fun mwebState(
        syncState: MwebSyncState,
        utxos: List<MwebUtxo> = emptyList(),
        transactions: List<MwebTransaction> = emptyList()
    ) = LitecoinMwebState(
        balance = MwebBalance(0, 0),
        syncState = syncState,
        debugInfo = MwebDebugInfo(
            state = syncState,
            peerAddress = null,
            addressPoolSize = 0,
            unspentUtxoCount = utxos.count { !it.spent },
            pendingTransactionCount = 0,
            nativeVersion = "test"
        ),
        utxos = utxos,
        pendingTransactions = emptyList<MwebPendingTransaction>(),
        transactions = transactions
    )

    private fun mwebHistoryTransactions() = listOf(
        mwebTransaction(
            uid = "mweb-incoming:receive-output",
            type = MwebTransactionType.Incoming,
            kind = MwebTransactionKind.Incoming,
            amount = 1_000,
            fee = null,
            address = "ltcmweb1receive",
            transactionHash = null,
            outputIds = listOf("receive-output"),
            timestamp = 10_000,
            height = 900,
            pending = false
        ),
        mwebTransaction(
            uid = "mweb-outgoing:outgoing-hash",
            type = MwebTransactionType.Outgoing,
            kind = MwebTransactionKind.MwebToPublic,
            amount = 500,
            fee = 10,
            address = "ltc1destination",
            transactionHash = "outgoing-hash",
            outputIds = listOf("change-output"),
            inputOutputIds = listOf("spent-output"),
            timestamp = 10_100,
            height = null,
            pending = true
        )
    )

    private fun mwebTransaction(
        uid: String,
        type: MwebTransactionType,
        kind: MwebTransactionKind,
        amount: Long,
        fee: Long?,
        address: String?,
        transactionHash: String?,
        outputIds: List<String>,
        inputOutputIds: List<String> = emptyList(),
        timestamp: Long,
        height: Int?,
        pending: Boolean
    ) = MwebTransaction(
        uid = uid,
        type = type,
        kind = kind,
        amount = amount,
        fee = fee,
        address = address,
        canonicalTransactionHash = transactionHash,
        outputIds = outputIds,
        inputOutputIds = inputOutputIds,
        height = height,
        timestamp = timestamp,
        pending = pending
    )

    private fun mwebSendInfo(
        totalFee: Long,
        selectedPublicUtxos: List<UnspentOutputInfo> = emptyList()
    ) = LitecoinSendInfo.Mweb(
        MwebSendInfo(
            selectedPublicUtxos = selectedPublicUtxos,
            selectedMwebUtxos = emptyList(),
            normalFee = 0,
            mwebFee = totalFee,
            totalFee = totalFee,
            changeValue = null,
            changeAddress = null
        )
    )

    private fun unspentOutputInfo(
        transactionHash: ByteArray,
        outputIndex: Int
    ) = UnspentOutputInfo(
        outputIndex = outputIndex,
        transactionHash = transactionHash,
        timestamp = 0,
        address = PUBLIC_ADDRESS,
        value = 100
    )

    private fun transactionInfo(
        uid: String,
        transactionHash: String,
        amount: Long,
        toAddress: String?,
        timestamp: Long,
        type: TransactionType = TransactionType.Outgoing,
        outputMine: Boolean = false,
        changeOutput: Boolean = false,
    ) = TransactionInfo(
        uid = uid,
        transactionHash = transactionHash,
        transactionIndex = 0,
        inputs = emptyList(),
        outputs = listOf(
            TransactionOutputInfo(
                mine = outputMine,
                changeOutput = changeOutput,
                value = amount,
                address = toAddress,
                memo = null
            )
        ),
        amount = amount,
        type = type,
        fee = 10,
        blockHeight = null,
        timestamp = timestamp,
        status = TransactionStatus.NEW
    )

    private companion object {
        const val PUBLIC_ADDRESS = "ltc-public-destination"
        const val PUBLIC_CHANGE_ADDRESS = "ltc-public-change"
        const val MWEB_ADDRESS = "ltcmweb1destination"
        const val INVALID_ADDRESS = "invalid-litecoin-address"
    }
}
