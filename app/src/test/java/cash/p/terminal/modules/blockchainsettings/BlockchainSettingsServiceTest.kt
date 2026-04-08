package cash.p.terminal.modules.blockchainsettings

import cash.p.terminal.core.managers.BtcBlockchainManager
import cash.p.terminal.core.managers.EvmBlockchainManager
import cash.p.terminal.core.managers.EvmSyncSourceManager
import cash.p.terminal.core.managers.SolanaRpcSourceManager
import cash.p.terminal.entities.BtcRestoreMode
import cash.p.terminal.entities.EvmSyncSource
import cash.p.terminal.modules.blockchainsettings.BlockchainSettingsModule.BlockchainItem
import cash.p.terminal.wallet.MarketKitWrapper
import io.horizontalsystems.core.entities.Blockchain
import io.horizontalsystems.core.entities.BlockchainType
import io.horizontalsystems.solanakit.models.RpcSource
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import io.reactivex.subjects.PublishSubject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BlockchainSettingsServiceTest {

    private val btcBlockchainManager = mockk<BtcBlockchainManager>()
    private val evmBlockchainManager = mockk<EvmBlockchainManager>()
    private val evmSyncSourceManager = mockk<EvmSyncSourceManager>()
    private val solanaRpcSourceManager = mockk<SolanaRpcSourceManager>()
    private val marketKit = mockk<MarketKitWrapper>()

    private var service: BlockchainSettingsService? = null

    @Before
    fun setUp() {
        every { btcBlockchainManager.restoreModeUpdatedObservable } returns PublishSubject.create()
        every { btcBlockchainManager.transactionSortModeUpdatedObservable } returns PublishSubject.create()
        every { evmSyncSourceManager.syncSourceObservable } returns PublishSubject.create()
        every { solanaRpcSourceManager.rpcSourceUpdateObservable } returns PublishSubject.create()
    }

    @After
    fun tearDown() {
        service?.stop()
        unmockkAll()
    }

    @Test
    fun start_supportedBlockchains_buildsSortedBlockchainItems() {
        val bitcoin = blockchain(BlockchainType.Bitcoin)
        val bitcoinCash = blockchain(BlockchainType.BitcoinCash)
        val ethereum = blockchain(BlockchainType.Ethereum)
        val gnosis = blockchain(BlockchainType.Gnosis)
        val solana = blockchain(BlockchainType.Solana)
        val tron = blockchain(BlockchainType.Tron)
        val stellar = blockchain(BlockchainType.Stellar)

        val ethereumSyncSource = mockk<EvmSyncSource>(relaxed = true)
        val gnosisSyncSource = mockk<EvmSyncSource>(relaxed = true)

        every { btcBlockchainManager.allBlockchains } returns listOf(bitcoinCash, bitcoin)
        every { btcBlockchainManager.restoreMode(BlockchainType.Bitcoin) } returns BtcRestoreMode.Hybrid
        every { btcBlockchainManager.restoreMode(BlockchainType.BitcoinCash) } returns BtcRestoreMode.Blockchair

        every { evmBlockchainManager.allBlockchains } returns listOf(gnosis, ethereum)
        every { evmSyncSourceManager.getSyncSource(BlockchainType.Ethereum) } returns ethereumSyncSource
        every { evmSyncSourceManager.getSyncSource(BlockchainType.Gnosis) } returns gnosisSyncSource

        every { solanaRpcSourceManager.blockchain } returns solana
        every { solanaRpcSourceManager.rpcSource } returns RpcSource.TritonOne

        every { marketKit.blockchains(BlockchainSettingsModule.statusOnlyBlockchainTypes.map { it.uid }) } returns listOf(
            stellar,
            tron,
        )

        val service = createService()

        service.start()

        val blockchainItems = service.blockchainItems
        assertEquals(7, blockchainItems.size)

        val bitcoinItem = blockchainItems[0] as BlockchainItem.Btc
        assertEquals(bitcoin, bitcoinItem.blockchain)
        assertEquals(BtcRestoreMode.Hybrid, bitcoinItem.restoreMode)

        val ethereumItem = blockchainItems[1] as BlockchainItem.Evm
        assertEquals(ethereum, ethereumItem.blockchain)
        assertEquals(ethereumSyncSource, ethereumItem.syncSource)

        val tronItem = blockchainItems[2] as BlockchainItem.StatusOnly
        assertEquals(tron, tronItem.blockchain)

        val solanaItem = blockchainItems[3] as BlockchainItem.Solana
        assertEquals(solana, solanaItem.blockchain)
        assertEquals(RpcSource.TritonOne, solanaItem.rpcSource)

        val stellarItem = blockchainItems[4] as BlockchainItem.StatusOnly
        assertEquals(stellar, stellarItem.blockchain)

        val bitcoinCashItem = blockchainItems[5] as BlockchainItem.Btc
        assertEquals(bitcoinCash, bitcoinCashItem.blockchain)
        assertEquals(BtcRestoreMode.Blockchair, bitcoinCashItem.restoreMode)

        val gnosisItem = blockchainItems[6] as BlockchainItem.Evm
        assertEquals(gnosis, gnosisItem.blockchain)
        assertEquals(gnosisSyncSource, gnosisItem.syncSource)

        verify(exactly = 1) {
            marketKit.blockchains(BlockchainSettingsModule.statusOnlyBlockchainTypes.map { it.uid })
        }
    }

    @Test
    fun start_missingSolanaBlockchain_excludesSolanaItem() {
        val bitcoin = blockchain(BlockchainType.Bitcoin)
        val ethereum = blockchain(BlockchainType.Ethereum)
        val tron = blockchain(BlockchainType.Tron)
        val ethereumSyncSource = mockk<EvmSyncSource>(relaxed = true)

        every { btcBlockchainManager.allBlockchains } returns listOf(bitcoin)
        every { btcBlockchainManager.restoreMode(BlockchainType.Bitcoin) } returns BtcRestoreMode.Hybrid

        every { evmBlockchainManager.allBlockchains } returns listOf(ethereum)
        every { evmSyncSourceManager.getSyncSource(BlockchainType.Ethereum) } returns ethereumSyncSource

        every { solanaRpcSourceManager.blockchain } returns null
        every { solanaRpcSourceManager.rpcSource } returns RpcSource.TritonOne

        every { marketKit.blockchains(BlockchainSettingsModule.statusOnlyBlockchainTypes.map { it.uid }) } returns listOf(tron)

        val service = createService()

        service.start()

        assertEquals(3, service.blockchainItems.size)
        assertTrue(service.blockchainItems.none { it is BlockchainItem.Solana })
    }

    private fun createService(): BlockchainSettingsService {
        return BlockchainSettingsService(
            btcBlockchainManager = btcBlockchainManager,
            evmBlockchainManager = evmBlockchainManager,
            evmSyncSourceManager = evmSyncSourceManager,
            solanaRpcSourceManager = solanaRpcSourceManager,
            marketKit = marketKit,
        ).also {
            service = it
        }
    }

    private fun blockchain(type: BlockchainType) = Blockchain(
        type = type,
        name = type.uid,
        eip3091url = null,
    )
}
