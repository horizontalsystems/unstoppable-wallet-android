package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.IAppConfigProvider
import io.horizontalsystems.bankwallet.core.ILocalStorage
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

    private lateinit var dataProviderManager: TransactionDataProviderManager

    @Before
    fun setup() {
        dataProviderManager = TransactionDataProviderManager(appConfig, localStorage)
    }

    @Test
    fun providers() {
        dataProviderManager.providers("BTC").forEach {
            assertThat(it, instanceOf(BitcoinForksProvider::class.java))
        }
        dataProviderManager.providers("BCH").forEach {
            assertThat(it, instanceOf(BitcoinForksProvider::class.java))
        }
        dataProviderManager.providers("ETH").forEach {
            assertThat(it, instanceOf(EthereumForksProvider::class.java))
        }
    }

    @Test
    fun baseProvider() {
        assertThat(dataProviderManager.baseProvider("BTC"), instanceOf(BitcoinForksProvider::class.java))
        assertThat(dataProviderManager.baseProvider("BCH"), instanceOf(BitcoinForksProvider::class.java))
        assertThat(dataProviderManager.baseProvider("ETH"), instanceOf(EthereumForksProvider::class.java))
    }

    @Test
    fun setBaseProvider() {
        dataProviderManager.setBaseProvider("BTC.com", "BTC")

        verify(localStorage).baseBitcoinProvider = "BTC.com"
    }

    @Test
    fun bitcoin() {
        listOf("HorizontalSystems.xyz", "BlockChair.com", "BlockExplorer.com", "Btc.com").forEach {
            val bitcoinProvider = dataProviderManager.bitcoin(it)
            assertEquals(it, bitcoinProvider.name)
        }
    }

    @Test
    fun bitcoinCash() {
        listOf("HorizontalSystems.xyz", "BlockChair.com", "BlockExplorer.com", "Btc.com").forEach {
            val bitcoinCashProvider = dataProviderManager.bitcoinCash(it)

            assertEquals(it, bitcoinCashProvider.name)
        }
    }

    @Test
    fun ethereum() {
        listOf("HorizontalSystems.xyz", "Etherscan.io", "BlockChair.com").forEach {
            val bitcoinCashProvider = dataProviderManager.ethereum(it)

            assertEquals(it, bitcoinCashProvider.name)
        }
    }

    @Test
    fun getProvider_MainNet() {
        whenever(appConfig.testMode).thenReturn(false)

        val bitcoin = dataProviderManager.bitcoin("HorizontalSystems.xyz")

        assertThat(bitcoin, instanceOf(HorsysBitcoinProvider::class.java))
        assertEquals(bitcoin.url("abc"), "https://btc.horizontalsystems.xyz/tx/abc")
    }

    @Test
    fun getProvider_TestNet() {
        whenever(appConfig.testMode).thenReturn(true)

        val bitcoin = dataProviderManager.bitcoin("HorizontalSystems.xyz")

        assertThat(bitcoin, instanceOf(HorsysBitcoinProvider::class.java))
        assertEquals(bitcoin.url("abc"), "http://btc-testnet.horizontalsystems.xyz/tx/abc")
    }
}
