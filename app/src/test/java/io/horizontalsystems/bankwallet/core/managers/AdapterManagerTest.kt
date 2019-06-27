//package io.horizontalsystems.bankwallet.core.managers
//
//import com.nhaarman.mockito_kotlin.verify
//import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
//import com.nhaarman.mockito_kotlin.whenever
//import io.horizontalsystems.bankwallet.core.IAdapter
//import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
//import io.horizontalsystems.bankwallet.entities.AuthData
//import io.horizontalsystems.bankwallet.entities.Coin
//import io.horizontalsystems.bankwallet.entities.CoinType
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.mock
//
//class AdapterManagerTest {
//    private val coinManager = mock(CoinManager::class.java)
//    private val authManager = mock(AuthManager::class.java)
//    private val adapterFactory = mock(AdapterFactory::class.java)
//
//    private val authData = mock(AuthData::class.java)
//    private val adapter = mock(IAdapter::class.java)
//    private val coin = mock(Coin::class.java)
//
//    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
//    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
//
//    private lateinit var adapterManager: AdapterManager
//
//    @Before
//    fun setup() {
//        whenever(authManager.authData).thenReturn(authData)
//        whenever(coin.code).thenReturn(bitCoin.code)
//        whenever(adapter.coin).thenReturn(coin)
//
//        adapterManager = AdapterManager(coinManager, authManager, adapterFactory)
//    }
//
//    @Test
//    fun initAdapters() {
//        whenever(coinManager.wallets).thenReturn(listOf(bitCoin))
//
//        adapterManager.initAdapters()
//        verify(adapterFactory).adapterForCoin(bitCoin, authData)
//        verifyNoMoreInteractions(adapterFactory)
//    }
//
//    @Test
//    fun initWallets_addNew() {
//        whenever(coinManager.wallets).thenReturn(listOf(ethereumCoin))
//
//        adapterManager.adapters = listOf(adapter)
//        adapterManager.initAdapters()
//
//        verify(adapterFactory).adapterForCoin(ethereumCoin, authData)
//        verify(adapterFactory).unlinkAdapter(adapter)
//    }
//}
