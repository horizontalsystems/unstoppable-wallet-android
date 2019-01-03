package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class RateManagerTest {

    private lateinit var rateManager: RateManager

    private val networkManager = mock(INetworkManager::class.java)

    private val storage = mock(IRateStorage::class.java)

    @Before
    fun setup() {
        RxBaseTest.setup()
    }

    @Test
    fun refreshRates() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        rateManager = RateManager(
                storage,
                networkManager
                )

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.just(LatestRate(123.12, 1000)))
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45, 2000)))

        rateManager.refreshRates(coins, currencyCode)

        verify(storage).save(Rate(coins[0], currencyCode, 123.12, 1000))
        verify(storage).save(Rate(coins[1], currencyCode, 456.45, 2000))
        verifyNoMoreInteractions(storage)
    }

    @Test
    fun refreshRates_oneEmpty() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        rateManager = RateManager(
                storage,
                networkManager
                )

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.empty())
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45, 2000)))

        rateManager.refreshRates(coins, currencyCode)

        verify(storage).save(Rate(coins[1], currencyCode, 456.45, 2000))
        verifyNoMoreInteractions(storage)
    }

    @Test
    fun refreshRates_oneError() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        rateManager = RateManager(
                storage,
                networkManager
                )

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.error(Exception()))
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45, 2000)))

        rateManager.refreshRates(coins, currencyCode)

        verify(storage).save(Rate(coins[1], currencyCode, 456.45, 2000))
        verifyNoMoreInteractions(storage)
    }

}