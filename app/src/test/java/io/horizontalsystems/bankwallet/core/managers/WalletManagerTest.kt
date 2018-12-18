package io.horizontalsystems.bankwallet.core.managers

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.factories.AdapterFactory
import io.horizontalsystems.bankwallet.entities.BitcoinType
import io.horizontalsystems.bankwallet.entities.BlockChain
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.EthereumType
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(WalletManager::class, HandlerThread::class)
class WalletManagerTest {

    private val adapterFactory = mock(AdapterFactory::class.java)
    private val handler = mock(Handler::class.java)
    private val looper = mock(Looper::class.java)
    private val handlerThread = mock(HandlerThread::class.java)
    private val words = listOf("one", "two", "three")
    private val enabledCoins = listOf(
            Coin("Bitcoin", "BTC", blockChain = BlockChain.Bitcoin(BitcoinType.Bitcoin)),
            Coin("Ethereum", "ETH", blockChain = BlockChain.Ethereum(EthereumType.Ethereum)))

    private lateinit var manager: WalletManager

    @Before
    fun setUp() {
        PowerMockito
                .whenNew(Handler::class.java)
                .withAnyArguments()
                .thenReturn(handler)

        PowerMockito
                .whenNew(HandlerThread::class.java)
                .withAnyArguments()
                .thenReturn(handlerThread)

        whenever(handlerThread.looper).thenReturn(looper)

        manager = WalletManager(adapterFactory)
    }

    @Test
    fun initWallets() {
//        manager.initWallets(words, enabledCoins, false)
//        Assert.assertEquals(manager.wallets[0].coinCode, enabledCoins[0].coinCode)
//        Assert.assertEquals(manager.wallets[0].title, enabledCoins[0].title)
//        Assert.assertEquals(manager.wallets[1].coinCode, enabledCoins[1].coinCode)
//        Assert.assertEquals(manager.wallets[1].title, enabledCoins[1].title)
    }

}
