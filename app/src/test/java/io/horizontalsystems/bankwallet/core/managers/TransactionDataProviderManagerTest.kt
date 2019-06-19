package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.BitcoinForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule.EthereumForksProvider
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers.HorsysBitcoinProvider
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionDataProviderManagerTest {

    private val appConfig = mock(IAppConfigProvider::class.java)
    private val localStorage = mock(ILocalStorage::class.java)

    private val btc = mock(Coin::class.java)
    private val bch = mock(Coin::class.java)
    private val eth = mock(Coin::class.java)

    private lateinit var dataProviderManager: TransactionDataProviderManager

    @Before
    fun setup() {
        whenever(btc.type).thenReturn(mock(CoinType.Bitcoin::class.java))
        whenever(bch.type).thenReturn(mock(CoinType.BitcoinCash::class.java))
        whenever(eth.type).thenReturn(mock(CoinType.Ethereum::class.java))

        dataProviderManager = TransactionDataProviderManager(appConfig, localStorage)
    }

    @Test
    fun providers() {
        dataProviderManager.providers(btc).forEach {
            assertThat(it, instanceOf(BitcoinForksProvider::class.java))
        }
        dataProviderManager.providers(bch).forEach {
            assertThat(it, instanceOf(BitcoinForksProvider::class.java))
        }
        dataProviderManager.providers(eth).forEach {
            assertThat(it, instanceOf(EthereumForksProvider::class.java))
        }
    }

    @Test
    fun baseProvider() {
        assertThat(dataProviderManager.baseProvider(btc), instanceOf(BitcoinForksProvider::class.java))
        assertThat(dataProviderManager.baseProvider(bch), instanceOf(BitcoinForksProvider::class.java))
        assertThat(dataProviderManager.baseProvider(eth), instanceOf(EthereumForksProvider::class.java))
    }

    @Test
    fun setBaseProvider() {
        dataProviderManager.setBaseProvider("BTC.com", btc)

        verify(localStorage).baseBitcoinProvider = "BTC.com"
    }

    @Test
    fun bitcoin() {
        listOf("HorizontalSystems.xyz", "BlockChair.com", "Btc.com").forEach {
            val bitcoinProvider = dataProviderManager.bitcoin(it)
            assertEquals(it, bitcoinProvider.name)
        }
    }

    @Test
    fun bitcoinCash() {
        listOf("Blockdozer.com", "BlockChair.com", "Btc.com").forEach {
            val bitcoinCashProvider = dataProviderManager.bitcoinCash(it)

            assertEquals(it, bitcoinCashProvider.name)
        }
    }

    @Test
    fun ethereum() {
        listOf("Etherscan.io", "BlockChair.com").forEach {
            val bitcoinCashProvider = dataProviderManager.ethereum(it)

            assertEquals(it, bitcoinCashProvider.name)
        }
    }

    @Test
    fun getProvider_MainNet() {
        whenever(appConfig.testMode).thenReturn(false)

        val bitcoin = dataProviderManager.bitcoin("HorizontalSystems.xyz")

        assertThat(bitcoin, instanceOf(HorsysBitcoinProvider::class.java))
        assertEquals("https://btc.horizontalsystems.xyz/tx/abc", bitcoin.url("abc"))
    }

    @Test
    fun getProvider_TestNet() {
        whenever(appConfig.testMode).thenReturn(true)

        dataProviderManager = TransactionDataProviderManager(appConfig, localStorage)

        val bitcoin = dataProviderManager.bitcoin("HorizontalSystems.xyz")

        assertThat(bitcoin, instanceOf(HorsysBitcoinProvider::class.java))
        assertEquals("http://btc-testnet.horizontalsystems.xyz/tx/abc", bitcoin.url("abc"))
    }
}
