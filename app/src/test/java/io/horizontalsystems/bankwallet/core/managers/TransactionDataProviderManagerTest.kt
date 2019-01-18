package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verify
import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.modules.fulltransactioninfo.FullTransactionInfoModule
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionDataProviderManagerTest {

    private val localStorage = mock(ILocalStorage::class.java)

    private lateinit var dataProviderManager: TransactionDataProviderManager

    @Before
    fun setup() {
        dataProviderManager = TransactionDataProviderManager(localStorage)
    }

    @Test
    fun providers() {
        assertEquals(TransactionDataProviderManager.bitcoinProviders, dataProviderManager.providers("BTC"))
        assertEquals(TransactionDataProviderManager.bitcoinCashProviders, dataProviderManager.providers("BCH"))
        assertEquals(TransactionDataProviderManager.ethereumProviders, dataProviderManager.providers("ETH"))
    }

    @Test
    fun baseProvider() {
        assertThat(dataProviderManager.baseProvider("BTC"), instanceOf(FullTransactionInfoModule.BitcoinForksProvider::class.java))
        assertThat(dataProviderManager.baseProvider("BCH"), instanceOf(FullTransactionInfoModule.BitcoinForksProvider::class.java))
        assertThat(dataProviderManager.baseProvider("ETH"), instanceOf(FullTransactionInfoModule.EthereumForksProvider::class.java))
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
}
