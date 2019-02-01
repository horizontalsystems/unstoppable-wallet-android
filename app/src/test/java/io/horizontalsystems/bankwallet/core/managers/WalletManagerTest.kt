package io.horizontalsystems.bankwallet.core.managers

//import com.nhaarman.mockito_kotlin.verify
//import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
//import com.nhaarman.mockito_kotlin.whenever
//import io.horizontalsystems.bankwallet.core.factories.WalletFactory
//import io.horizontalsystems.bankwallet.entities.AuthData
//import io.horizontalsystems.bankwallet.entities.Coin
//import io.horizontalsystems.bankwallet.entities.CoinType
//import io.horizontalsystems.bankwallet.entities.Wallet
//import org.junit.Before
//import org.junit.Test
//import org.mockito.Mockito.mock

class WalletManagerTest {
//    private val coinManager = mock(CoinManager::class.java)
//    private val authManager = mock(AuthManager::class.java)
//    private val walletFactory = mock(WalletFactory::class.java)
//
//    private val authData = mock(AuthData::class.java)
//    private val wallet = mock(Wallet::class.java)
//
//    private val bitCoin = Coin("Bitcoin", "BTC", CoinType.Bitcoin)
//    private val ethereumCoin = Coin("Ethereum", "ETH", CoinType.Ethereum)
//
//    private lateinit var walletManager: WalletManager
//
//    @Before
//    fun setup() {
//        whenever(authManager.authData).thenReturn(authData)
//        whenever(wallet.coinCode).thenReturn(bitCoin.code)
//
//        walletManager = WalletManager(coinManager, authManager, walletFactory)
//    }
//
//    @Test
//    fun initWallets() {
//        whenever(coinManager.coins).thenReturn(listOf(bitCoin))
//
//        walletManager.initWallets()
//        verify(walletFactory).createWallet(bitCoin, authData)
//        verifyNoMoreInteractions(walletFactory)
//    }
//
//    @Test
//    fun initWallets_addNew() {
//        whenever(coinManager.coins).thenReturn(listOf(ethereumCoin))
//
//        walletManager.wallets = listOf(wallet)
//        walletManager.initWallets()
//
//        verify(walletFactory).createWallet(ethereumCoin, authData)
//        verify(walletFactory).unlinkWallet(wallet)
//    }
}
