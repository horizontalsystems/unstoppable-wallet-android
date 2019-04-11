package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atMost
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import io.reactivex.Maybe
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class RateManagerTest {

    private lateinit var rateManager: RateManager

    private val networkManager = mock(INetworkManager::class.java)

    private val storage = mock(IRateStorage::class.java)
    private val mainHost = ServiceExchangeApi.HostType.MAIN
    private val fallbackHost = ServiceExchangeApi.HostType.FALLBACK

    @Before
    fun setup() {
        RxBaseTest.setup()

        rateManager = RateManager(storage, networkManager)
    }

    @Test
    fun refreshRates() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"
        val rates = hashMapOf("BTC" to "3981.05", "ETH" to "138.27")

        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Maybe.just(LatestRateData(rates, "USD", 1000L)))
        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Maybe.just(LatestRateData(rates, "USD", 1000L)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).saveLatest(Rate(coins[0], currencyCode, 3981.05.toBigDecimal(), 1000, true))
        verify(storage).saveLatest(Rate(coins[1], currencyCode, 138.27.toBigDecimal(), 1000, true))
        verify(storage, atMost(2)).saveLatest(any())
    }

    @Test
    fun refreshRates_oneMissingRate() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"
        val rates = hashMapOf("ETH" to "138.27")

        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Maybe.just(LatestRateData(rates, "USD", 1000L)))
        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Maybe.just(LatestRateData(rates, "USD", 1000L)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).saveLatest(Rate(coins[1], currencyCode, 138.27.toBigDecimal(), 1000, true))
        verify(storage, atMost(1)).saveLatest(any())
    }

    @Test
    fun refreshRates_oneEmptyRate() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"
        val rates = hashMapOf("BTC" to "", "ETH" to "138.27")

        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Maybe.just(LatestRateData(rates, "USD", 1000L)))
        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Maybe.just(LatestRateData(rates, "USD", 1000L)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).saveLatest(Rate(coins[1], currencyCode, 138.27.toBigDecimal(), 1000, true))
        verify(storage, atMost(1)).saveLatest(any())
    }

    @Test
    fun refreshRates_oneError() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Maybe.error(Exception()))
        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Maybe.error(Exception()))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage, never()).saveLatest(any())
    }

    @Test
    fun rateValueObservable() {
//        val coinCode = "BTC"
//        val currencyCode = "USD"
//        val timestamp = 23412L
//        val rate = mock(Rate::class.java)
//        val rateValue = 123.23.toBigDecimal()
//
//        whenever(rate.value).thenReturn(rateValue)
//        whenever(storage.rateMaybe(coinCode, currencyCode, timestamp)).thenReturn(Maybe.just(rate))
//
//        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
//                .test()
//                .assertResult(rateValue)
    }

    @Test
    fun rateValueObservable_noRate() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = System.currentTimeMillis()
        val rateValueFromNetwork = 123.2300.toBigDecimal()

        whenever(storage.rateMaybe(coinCode, currencyCode, timestamp)).thenReturn(Maybe.empty())
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Maybe.just(rateValueFromNetwork))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()

        verify(networkManager).getRateByHour(mainHost, coinCode, currencyCode, timestamp)
        verify(storage).save(Rate(coinCode, currencyCode, rateValueFromNetwork, timestamp, false))
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_earlierThen1Hour() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = System.currentTimeMillis()
        val latestRate = mock(Rate::class.java)
        val rateValue = 234.23.toBigDecimal()

        whenever(storage.rateMaybe(coinCode, currencyCode, timestamp)).thenReturn(Maybe.empty())
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Maybe.error(Exception()))
        whenever(latestRate.expired).thenReturn(true)
        whenever(latestRate.value).thenReturn(rateValue)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_notExpired() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1
        val latestRate = mock(Rate::class.java)
        val rateValue = 234.23.toBigDecimal()

        whenever(storage.rateMaybe(coinCode, currencyCode, timestamp)).thenReturn(Maybe.empty())
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Maybe.error(Exception()))
        whenever(latestRate.expired).thenReturn(false)
        whenever(latestRate.value).thenReturn(rateValue)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_expired() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1
        val latestRate = mock(Rate::class.java)

        whenever(storage.rateMaybe(coinCode, currencyCode, timestamp)).thenReturn(Maybe.empty())
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Maybe.error(Exception()))
        whenever(latestRate.expired).thenReturn(true)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_emptyLatestRate() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1

        whenever(storage.rateMaybe(coinCode, currencyCode, timestamp)).thenReturn(Maybe.empty())
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.empty())
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Maybe.error(Exception()))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

}