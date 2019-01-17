package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.atMost
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.LatestRate
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class RateManagerTest {

    private lateinit var rateManager: RateManager

    private val networkManager = mock(INetworkManager::class.java)

    private val storage = mock(IRateStorage::class.java)

    private val zeroRatesSubject = PublishSubject.create<List<Rate>>()

    @Before
    fun setup() {
        RxBaseTest.setup()

        whenever(storage.zeroRatesObservables()).thenReturn(zeroRatesSubject.toFlowable(BackpressureStrategy.DROP))

        rateManager = RateManager(storage, networkManager)
    }

    @Test
    fun refreshRates() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.just(LatestRate(123.12, 1000)))
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45, 2000)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).save(Rate(coins[0], currencyCode, 123.12, 1000, true))
        verify(storage).save(Rate(coins[1], currencyCode, 456.45, 2000, true))
        verify(storage, atMost(2)).save(any())
    }

    @Test
    fun refreshRates_oneEmpty() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.empty())
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45, 2000)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).save(Rate(coins[1], currencyCode, 456.45, 2000, true))
        verify(storage, atMost(1)).save(any())
    }

    @Test
    fun refreshRates_oneError() {
        val coins = listOf("BTC", "ETH")
        val currencyCode = "USD"

        whenever(networkManager.getLatestRate(coins[0], currencyCode)).thenReturn(Flowable.error(Exception()))
        whenever(networkManager.getLatestRate(coins[1], currencyCode)).thenReturn(Flowable.just(LatestRate(456.45, 2000)))

        rateManager.refreshLatestRates(coins, currencyCode)

        verify(storage).save(Rate(coins[1], currencyCode, 456.45, 2000, true))
        verify(storage, atMost(1)).save(any())
    }

    @Test
    fun rateValueObservable() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = 23412L
        val rate = mock(Rate::class.java)
        val rateValue = 123.23

        whenever(rate.value).thenReturn(rateValue)
        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf(rate)))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertValue(rateValue)

    }

    @Test
    fun rateValueObservable_zeroValue() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = 23412L
        val rate = mock(Rate::class.java)
        val rateValue = 0.0

        whenever(rate.value).thenReturn(rateValue)
        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf(rate)))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate() {
        val coinCode = "BTC"
        val currencyCode = "USD"
        val timestamp = 23412L

        whenever(storage.rateObservable(coinCode, currencyCode, timestamp)).thenReturn(Flowable.just(listOf()))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()

        verify(storage).save(Rate(coinCode, currencyCode, 0.0, timestamp, false))
    }

    @Test
    fun handleZeroRate() {
        val coinCode1 = "BTC"
        val currencyCode1 = "USD"
        val timestamp1 = 123L
        val fetchedRateValue1 = 123.123

        val coinCode2 = "ETH"
        val currencyCode2 = "EUR"
        val timestamp2 = 876L
        val fetchedRateValue2 = 23423.34

        val rate1 = Rate(coinCode1, currencyCode1, 0.0, timestamp1, false)
        val rate2 = Rate(coinCode2, currencyCode2, 0.0, timestamp2, false)

        whenever(networkManager.getRate(coinCode1, currencyCode1, timestamp1)).thenReturn(Flowable.just(fetchedRateValue1))
        whenever(networkManager.getRate(coinCode2, currencyCode2, timestamp2)).thenReturn(Flowable.just(fetchedRateValue2))

        zeroRatesSubject.onNext(listOf(rate1))
        zeroRatesSubject.onNext(listOf(rate2))

        verify(storage).save(Rate(coinCode1, currencyCode1, fetchedRateValue1, timestamp1, false))
        verify(storage).save(Rate(coinCode2, currencyCode2, fetchedRateValue2, timestamp2, false))
    }

}