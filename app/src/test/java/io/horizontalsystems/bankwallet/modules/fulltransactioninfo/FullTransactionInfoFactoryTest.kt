package io.horizontalsystems.bankwallet.modules.fulltransactioninfo

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.ITransactionDataProviderManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class FullTransactionInfoFactoryTest {

    private val btcFork = mock(FullTransactionInfoModule.BitcoinForksProvider::class.java)
    private val ethFork = mock(FullTransactionInfoModule.EthereumForksProvider::class.java)
    private val provider = mock(FullTransactionInfoModule.Provider::class.java)

    private val networkManager = mock(INetworkManager::class.java)
    private val dataProviderManager = mock(ITransactionDataProviderManager::class.java)
    private val providerName = "HorizontalSystems.xyz"
    private val btc = mock(Coin::class.java)
    private val bch = mock(Coin::class.java)
    private val eth = mock(Coin::class.java)

    private lateinit var fullTransactionInfoFactory: FullTransactionInfoFactory

    @Before
    fun setup() {
        whenever(provider.name).thenReturn(providerName)
        whenever(dataProviderManager.baseProvider(any()))
                .thenReturn(provider)

        whenever(btc.type).thenReturn(mock(CoinType.Bitcoin::class.java))
        whenever(bch.type).thenReturn(mock(CoinType.BitcoinCash::class.java))
        whenever(eth.type).thenReturn(mock(CoinType.Ethereum::class.java))

        whenever(btc.code).thenReturn("BTC")
        whenever(bch.code).thenReturn("BCH")
        whenever(eth.code).thenReturn("ETH")

        fullTransactionInfoFactory = FullTransactionInfoFactory(networkManager, dataProviderManager)
    }

    @Test
    fun providerFor_BTC() {
        whenever(dataProviderManager.bitcoin(providerName)).thenReturn(btcFork)

        fullTransactionInfoFactory.providerFor(btc)

        verify(dataProviderManager).bitcoin(providerName)
    }

    @Test
    fun providerFor_BCH() {
        whenever(dataProviderManager.bitcoinCash(providerName)).thenReturn(btcFork)

        fullTransactionInfoFactory.providerFor(bch)

        verify(dataProviderManager).bitcoinCash(providerName)
    }

    @Test
    fun providerFor_ETH() {
        whenever(dataProviderManager.ethereum(providerName)).thenReturn(ethFork)

        fullTransactionInfoFactory.providerFor(eth)

        verify(dataProviderManager).ethereum(providerName)
    }

}
