package io.horizontalsystems.bankwallet.modules.fulltransactioninfo.providers

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ProvidersMapTest {
    private lateinit var providersMap: ProvidersMap

    @Before
    fun setup() {
        providersMap = ProvidersMap()
    }

    @Test
    fun bitcoin() {
        listOf("HorizontalSystems.xyz", "BlockChair.com", "BlockExplorer.com", "Btc.com").forEach {
            val bitcoinProvider = providersMap.bitcoin(it)
            assertEquals(it, bitcoinProvider.name)
        }
    }

    @Test
    fun bitcoinCash() {
        listOf("HorizontalSystems.xyz", "BlockChair.com", "BlockExplorer.com", "Btc.com").forEach {
            val bitcoinCashProvider = providersMap.bitcoinCash(it)

            assertEquals(it, bitcoinCashProvider.name)
        }
    }

    @Test
    fun ethereum() {
        listOf("HorizontalSystems.xyz", "Etherscan.io", "BlockChair.com").forEach {
            val bitcoinCashProvider = providersMap.ethereum(it)

            assertEquals(it, bitcoinCashProvider.name)
        }
    }
}
