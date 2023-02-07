//package cash.p.terminal.core.managers
//
//import com.nhaarman.mockito_kotlin.verify
//import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
//import com.nhaarman.mockito_kotlin.whenever
//import cash.p.terminal.core.IAdapter
//import cash.p.terminal.core.Wallet
//import cash.p.terminal.core.factories.AdapterFactory
//import cash.p.terminal.entities.AuthData
//import cash.p.terminal.entities.Coin
//import cash.p.terminal.entities.CoinType
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.mock
//
//class AdapterManagerTest {
//    private val coinManager = mock(WalletManager::class.java)
//    private val authManager = mock(AuthManager::class.java)
//    private val adapterFactory = mock(AdapterFactory::class.java)
//    private val ethereumKitManager = mock(EthereumKitManager::class.java)
//
//    private val authData = mock(AuthData::class.java)
//    private val adapter = mock(IAdapter::class.java)
//    private val walletBTC = mock(Wallet::class.java)
//    private val walletEth = mock(Wallet::class.java)
//
//    private val bitcoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
//    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
//
//    private lateinit var adapterManager: AdapterManager
//
//    @Before
//    fun setup() {
//        whenever(authManager.authData).thenReturn(authData)
//        whenever(walletBTC.coin).thenReturn(bitcoin)
//        whenever(walletEth.coin).thenReturn(ethereumCoin)
//        whenever(adapter.wallet).thenReturn(walletBTC)
//
//        adapterManager = AdapterManager(coinManager, authManager, adapterFactory, ethereumKitManager)
//    }
//
//    @Test
//    fun initAdapters() {
//        whenever(coinManager.wallets).thenReturn(listOf(walletBTC))
//
//        adapterManager.initAdapters()
//
//        verify(adapterFactory).adapterForCoin(walletBTC)
//        verifyNoMoreInteractions(adapterFactory)
//    }
//
//    @Test
//    fun initWallets_addNew() {
//        whenever(coinManager.wallets).thenReturn(listOf(walletEth))
//
//        adapterManager.adapters = listOf(adapter)
//        adapterManager.initAdapters()
//
//        verify(adapterFactory).adapterForCoin(walletEth)
//        verify(adapterFactory).unlinkAdapter(adapter)
//    }
//}
