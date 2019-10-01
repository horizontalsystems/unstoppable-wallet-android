package io.horizontalsystems.bankwallet.core.managers

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.INetworkManager
import io.horizontalsystems.bankwallet.core.IRateStorage
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.LatestRateData
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Flowable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.verify
import retrofit2.HttpException
import java.net.SocketTimeoutException

class RateManagerTest {

    private lateinit var rateManager: RateManager

    private val networkManager = mock<INetworkManager>()

    private val httpException = mock<HttpException>()
    private val storage = mock<IRateStorage>()
    private val mainHost = ServiceExchangeApi.HostType.MAIN
    private val fallbackHost = ServiceExchangeApi.HostType.FALLBACK

    private val walletStorage = mock<WalletStorage>()
    private val currencyManager = mock<ICurrencyManager>()
    private val connectivityManager = mock<ConnectivityManager>()
    private val currencyCode = "USD"

    @Before
    fun setup() {
        RxBaseTest.setup()

        rateManager = RateManager(storage, networkManager, walletStorage, currencyManager, connectivityManager)

        val baseCurrency = mock<Currency>()

        whenever(connectivityManager.isConnected).thenReturn(true)
        whenever(currencyManager.baseCurrency).thenReturn(baseCurrency)
        whenever(baseCurrency.code).thenReturn(currencyCode)
    }

    @Test
    fun refreshRates() {
        val coinCodes = listOf("BTC", "ETH")
        val rates = hashMapOf("BTC" to "3981.05", "ETH" to "138.27")

        val coins = getMockCoinsForCoinCodes(coinCodes)
        whenever(walletStorage.enabledCoins()).thenReturn(coins)

        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Single.just(LatestRateData(rates, "USD", 1000L)))
        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Single.just(LatestRateData(rates, "USD", 1000L)))

        rateManager.syncLatestRates()

        verify(storage).saveLatest(Rate(coinCodes[0], currencyCode, 3981.05.toBigDecimal(), 1000, true))
        verify(storage).saveLatest(Rate(coinCodes[1], currencyCode, 138.27.toBigDecimal(), 1000, true))
        verify(storage, atMost(2)).saveLatest(any())
    }


    @Test
    fun refreshRates_oneMissingRate() {
        val coinCodes = listOf("BTC", "ETH")
        val rates = hashMapOf("ETH" to "138.27")

        val coins = getMockCoinsForCoinCodes(coinCodes)
        whenever(walletStorage.enabledCoins()).thenReturn(coins)

        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Single.just(LatestRateData(rates, "USD", 1000L)))
        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Single.just(LatestRateData(rates, "USD", 1000L)))

        rateManager.syncLatestRates()

        verify(storage).saveLatest(Rate(coinCodes[1], currencyCode, 138.27.toBigDecimal(), 1000, true))
        verify(storage, atMost(1)).saveLatest(any())
    }

    private fun getMockCoinsForCoinCodes(coinCodes: List<String>): List<Coin> {
        return coinCodes.map {
            val coin = mock<Coin>()

            whenever(coin.code).thenReturn(it)

            coin
        }
    }

    @Test
    fun refreshRates_oneEmptyRate() {
        val coinCodes = listOf("BTC", "ETH")
        val rates = hashMapOf("BTC" to "", "ETH" to "138.27")

        val coins = getMockCoinsForCoinCodes(coinCodes)
        whenever(walletStorage.enabledCoins()).thenReturn(coins)

        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Single.just(LatestRateData(rates, "USD", 1000L)))
        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Single.just(LatestRateData(rates, "USD", 1000L)))

        rateManager.syncLatestRates()

        verify(storage).saveLatest(Rate(coinCodes[1], currencyCode, 138.27.toBigDecimal(), 1000, true))
        verify(storage, atMost(1)).saveLatest(any())
    }

    @Test
    fun refreshRates_oneError() {
        val coinCodes = listOf("BTC", "ETH")

        val coins = getMockCoinsForCoinCodes(coinCodes)
        whenever(walletStorage.enabledCoins()).thenReturn(coins)

        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Single.error(Exception()))
        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Single.error(Exception()))

        rateManager.syncLatestRates()

        verify(storage, never()).saveLatest(any())
    }

    @Test
    fun refreshRates_fromFallbackHost() {
        val coinCodes = listOf("BTC", "ETH")
        val rates = hashMapOf("BTC" to "3981.05", "ETH" to "138.27")

        val coins = getMockCoinsForCoinCodes(coinCodes)
        whenever(walletStorage.enabledCoins()).thenReturn(coins)

        whenever(networkManager.getLatestRateData(fallbackHost, currencyCode)).thenReturn(Single.error(Exception()))
        whenever(networkManager.getLatestRateData(mainHost, currencyCode)).thenReturn(Single.just(LatestRateData(rates, "USD", 1000L)))

        rateManager.syncLatestRates()

        verify(storage).saveLatest(Rate(coinCodes[0], currencyCode, 3981.05.toBigDecimal(), 1000, true))
        verify(storage).saveLatest(Rate(coinCodes[1], currencyCode, 138.27.toBigDecimal(), 1000, true))
        verify(storage, atMost(2)).saveLatest(any())
    }

    @Test
    fun rateValueObservable() {
        val coinCode = "BTC"
        val timestamp = 23412L
        val rate = mock<Rate>()
        val rateValue = 123.23.toBigDecimal()

        whenever(rate.value).thenReturn(rateValue)
        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.just(rate))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.just(rateValue))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertResult(rateValue)
    }

    @Test
    fun rateValueObservable_noRate() {
        val coinCode = "BTC"
        val timestamp = System.currentTimeMillis()
        val rateValueFromNetwork = 123.2300.toBigDecimal()

        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.error(Exception()))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.just(rateValueFromNetwork))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()

        verify(networkManager).getRateByHour(mainHost, coinCode, currencyCode, timestamp)
        verify(storage).save(Rate(coinCode, currencyCode, rateValueFromNetwork, timestamp, false))
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_earlierThen1Hour() {
        val coinCode = "BTC"
        val timestamp = System.currentTimeMillis()
        val latestRate = mock<Rate>()
        val rateValue = 234.23.toBigDecimal()

        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(latestRate.expired).thenReturn(true)
        whenever(latestRate.value).thenReturn(rateValue)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_notExpired() {
        val coinCode = "BTC"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1
        val latestRate = mock<Rate>()
        val rateValue = 234.23.toBigDecimal()

        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(latestRate.expired).thenReturn(false)
        whenever(latestRate.value).thenReturn(rateValue)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_expired() {
        val coinCode = "BTC"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1
        val latestRate = mock<Rate>()

        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.just(latestRate))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(latestRate.expired).thenReturn(true)

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_noRate_latestRateFallback_notEarlierThen1Hour_emptyLatestRate() {
        val coinCode = "BTC"
        val timestamp = ((System.currentTimeMillis() / 1000) - 3600) + 1

        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.empty())
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()
                .assertNoValues()
    }

    @Test
    fun rateValueObservable_RateFromFallbackHost() {
        val coinCode = "BTC"
        val timestamp = System.currentTimeMillis()
        val rateValueFromNetwork = 234.23.toBigDecimal()

        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.empty())
        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(SocketTimeoutException()))
        whenever(networkManager.getRateByHour(fallbackHost, coinCode, currencyCode, timestamp)).thenReturn(Single.just(rateValueFromNetwork))
        whenever(networkManager.getRateByDay(fallbackHost, coinCode, currencyCode, timestamp)).thenReturn(Single.just(rateValueFromNetwork))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()

        verify(networkManager).getRateByHour(mainHost, coinCode, currencyCode, timestamp)
        verify(networkManager).getRateByHour(fallbackHost, coinCode, currencyCode, timestamp)
        verify(storage).save(Rate(coinCode, currencyCode, rateValueFromNetwork, timestamp, false))
    }

    @Test
    fun rateValueObservable_RateByDayFromFallbackHost() {
        val coinCode = "BTC"
        val timestamp = System.currentTimeMillis()
        val rateValueFromNetwork = 234.23.toBigDecimal()

        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.empty())
        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(SocketTimeoutException()))
        whenever(networkManager.getRateByHour(fallbackHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(networkManager.getRateByDay(fallbackHost, coinCode, currencyCode, timestamp)).thenReturn(Single.just(rateValueFromNetwork))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()

        verify(networkManager).getRateByHour(mainHost, coinCode, currencyCode, timestamp)
        verify(networkManager).getRateByHour(fallbackHost, coinCode, currencyCode, timestamp)
        verify(storage).save(Rate(coinCode, currencyCode, rateValueFromNetwork, timestamp, false))
    }

    @Test
    fun rateValueObservable_RateByDayFromMainHost() {
        val coinCode = "BTC"
        val timestamp = System.currentTimeMillis()
        val rateValueFromNetwork = 234.23.toBigDecimal()

        whenever(storage.latestRateObservable(coinCode, currencyCode)).thenReturn(Flowable.empty())
        whenever(storage.rateSingle(coinCode, currencyCode, timestamp)).thenReturn(Single.error(Exception()))
        whenever(networkManager.getRateByHour(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.error(httpException))
        whenever(networkManager.getRateByDay(mainHost, coinCode, currencyCode, timestamp)).thenReturn(Single.just(rateValueFromNetwork))
        whenever(networkManager.getRateByDay(fallbackHost, coinCode, currencyCode, timestamp)).thenReturn(Single.just(rateValueFromNetwork))

        rateManager.rateValueObservable(coinCode, currencyCode, timestamp)
                .test()

        verify(networkManager).getRateByDay(mainHost, coinCode, currencyCode, timestamp)
        verify(storage).save(Rate(coinCode, currencyCode, rateValueFromNetwork, timestamp, false))
    }

}
