package io.horizontalsystems.bankwallet.modules.transactions

import com.nhaarman.mockito_kotlin.*
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.core.IAdapterManager
import io.horizontalsystems.bankwallet.core.ICurrencyManager
import io.horizontalsystems.bankwallet.core.managers.NetworkAvailabilityManager
import io.horizontalsystems.bankwallet.core.managers.RateManager
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.TransactionRecord
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class TransactionsInteractorTest {

    private val adapterManager = mock(IAdapterManager::class.java)
    private val currencyManager = mock(ICurrencyManager::class.java)
    private val rateManager = mock(RateManager::class.java)
    private val delegate = mock(TransactionsModule.IInteractorDelegate::class.java)
    private val networkAvailability = mock(NetworkAvailabilityManager::class.java)

    private val adapter1 = mock(IAdapter::class.java)
    private val adapter2 = mock(IAdapter::class.java)
    private val coin1 = mock(Coin::class.java)
    private val coin2 = mock(Coin::class.java)
    private val coin3 = mock(Coin::class.java)
    private val adaptersUpdatedSignal = PublishSubject.create<Unit>()
    private val baseCurrencyUpdatedSignal = PublishSubject.create<Unit>()
    private val networkAvailabilitySignal = PublishSubject.create<Unit>()

    private lateinit var interactor: TransactionsInteractor

    @Before
    fun before() {
        RxBaseTest.setup()

        interactor = TransactionsInteractor(adapterManager, currencyManager, rateManager, networkAvailability)
        interactor.delegate = delegate

        whenever(adapterManager.adaptersUpdatedSignal).thenReturn(adaptersUpdatedSignal)
        whenever(currencyManager.baseCurrencyUpdatedSignal).thenReturn(baseCurrencyUpdatedSignal)
        whenever(networkAvailability.networkAvailabilitySignal).thenReturn(networkAvailabilitySignal)

        whenever(coin1.code).thenReturn("BTC")
        whenever(coin2.code).thenReturn("ETH")
        whenever(coin3.code).thenReturn("BCH")

        whenever(adapter1.coin).thenReturn(coin1)
        whenever(adapter2.coin).thenReturn(coin2)
        whenever(adapter1.transactionRecordsSubject).thenReturn(PublishSubject.create())
        whenever(adapter2.transactionRecordsSubject).thenReturn(PublishSubject.create())
    }

    @Test
    fun fetchLastBlockHeights_update() {
        val lastBlockHeightUpdatedSignal1 = PublishSubject.create<Unit>()
        val lastBlockHeightUpdated = 345345
        val adapter1 = mock(IAdapter::class.java)
        val adapters = listOf(adapter1)
        val coinCode1 = "BTC"

        val mockSubject = mock<PublishSubject<Unit>>()
        whenever(mockSubject.throttleLast(any(), any())).thenReturn(lastBlockHeightUpdatedSignal1)

        whenever(coin1.code).thenReturn(coinCode1)
        whenever(adapter1.lastBlockHeightUpdatedSignal).thenReturn(mockSubject)
        whenever(adapter1.lastBlockHeight).thenReturn(lastBlockHeightUpdated)
        whenever(adapter1.coin).thenReturn(coin1)
        whenever(adapterManager.adapters).thenReturn(adapters)

        interactor.fetchLastBlockHeights()

        lastBlockHeightUpdatedSignal1.onNext(Unit)

        verify(delegate).onUpdateLastBlockHeight(coin1, lastBlockHeightUpdated)
    }

    @Test
    fun initialFetch() {
        val adapters = listOf(adapter1)
        val lastBlockHeight1 = 123
        val confirmationsThreshold1 = 6
        val coinCode1 = "BTC"
        val allCoinData = listOf(Triple(coin1, confirmationsThreshold1, lastBlockHeight1))

        whenever(coin1.code).thenReturn(coinCode1)
        whenever(adapter1.lastBlockHeight).thenReturn(lastBlockHeight1)
        whenever(adapter1.confirmationsThreshold).thenReturn(confirmationsThreshold1)
        whenever(adapterManager.adapters).thenReturn(adapters)

        interactor.initialFetch()

        verify(delegate).onUpdateCoinsData(allCoinData)
    }

    @Test
    fun initialFetch_handleTransactionUpdates() {
        val adapters = listOf(adapter1)
        val coinCode1 = "BTC"

        val transactionRecordsSubject = PublishSubject.create<List<TransactionRecord>>()
        val transactionRecords1 = listOf(mock(TransactionRecord::class.java))

        whenever(coin1.code).thenReturn(coinCode1)
        whenever(adapter1.transactionRecordsSubject).thenReturn(transactionRecordsSubject)
        whenever(adapterManager.adapters).thenReturn(adapters)

        interactor.initialFetch()

        transactionRecordsSubject.onNext(transactionRecords1)

        verify(delegate).didUpdateRecords(transactionRecords1, coin1)
    }

    @Test
    fun initialFetch_baseCurrencyUpdate() {
        interactor.initialFetch()

        baseCurrencyUpdatedSignal.onNext(Unit)

        verify(delegate).onUpdateBaseCurrency()
    }

    @Test
    fun initialFetch_walletsUpdated() {
        val adapters: List<IAdapter> = listOf()
        val adaptersUpdated: List<IAdapter> = listOf(adapter1)
        val lastBlockHeight1 = 123
        val confirmationsThreshold1 = 6
        val coinCode1 = "BTC"
        val allCoinData = listOf(Triple(coin1, confirmationsThreshold1, lastBlockHeight1))

        whenever(coin1.code).thenReturn(coinCode1)
        whenever(adapter1.lastBlockHeight).thenReturn(lastBlockHeight1)
        whenever(adapter1.confirmationsThreshold).thenReturn(confirmationsThreshold1)
        whenever(adapterManager.adapters).thenReturn(adapters, adaptersUpdated)

        interactor.initialFetch()

        adaptersUpdatedSignal.onNext(Unit)

        verify(delegate).onUpdateCoinsData(listOf())
        verify(delegate).onUpdateCoinsData(allCoinData)
    }

    @Test
    fun setSelectedCoinCodes() {
        interactor.setSelectedCoinCodes(listOf(coin1))

        verify(delegate).onUpdateSelectedCoinCodes(listOf(coin1))
    }

    @Test
    fun setSelectedCoinCodes_empty() {
        val adapters = listOf(adapter1, adapter2)
        val allCoinCodes = listOf(coin1, coin2)

        whenever(coin1.code).thenReturn("BTC")
        whenever(coin2.code).thenReturn("ETH")
        whenever(adapterManager.adapters).thenReturn(adapters)

        interactor.setSelectedCoinCodes(listOf())

        verify(delegate).onUpdateSelectedCoinCodes(allCoinCodes)
    }

    @Test
    fun fetchItems() {
        val hashFrom1 = null
        val hashFrom2 = "hashFrom2"

        val limit1 = 5
        val limit2 = 12

        val fetchDataList = listOf(
                TransactionsModule.FetchData(coin1, hashFrom1, limit1),
                TransactionsModule.FetchData(coin2, hashFrom2, limit2),
                TransactionsModule.FetchData(coin3, null, 17)
        )
        val transactionRecords1 = listOf<TransactionRecord>(mock(TransactionRecord::class.java))
        val transactionRecords2 = listOf<TransactionRecord>(mock(TransactionRecord::class.java))

        whenever(coin1.code).thenReturn("BTC")
        whenever(coin2.code).thenReturn("ETH")
        whenever(adapterManager.adapters).thenReturn(listOf(adapter1, adapter2))
        whenever(adapter1.getTransactionsObservable(hashFrom1, limit1)).thenReturn(Single.just(transactionRecords1))
        whenever(adapter2.getTransactionsObservable(hashFrom2, limit2)).thenReturn(Single.just(transactionRecords2))

        interactor.fetchRecords(fetchDataList)

        val records = mapOf(coin1 to transactionRecords1, coin2 to transactionRecords2, coin3 to listOf())

        verify(delegate).didFetchRecords(records)
    }

    @Test
    fun fetchRates() {
        val coinCode1 = "BTC"
        val currencyCode = "USD"
        val timestamp1 = 123456L
        val timestamp2 = 34556L
        val currency = mock(Currency::class.java)
        val coin = Coin("Bitcoin", coinCode1, CoinType.Bitcoin)

        val rate1Value = 213.123.toBigDecimal()
        val rate2Value = 234.12.toBigDecimal()

        whenever(currency.code).thenReturn(currencyCode)
        whenever(currencyManager.baseCurrency).thenReturn(currency)

        whenever(rateManager.rateValueObservable(coinCode1, currencyCode, timestamp1)).thenReturn(Maybe.just(rate1Value))
        whenever(rateManager.rateValueObservable(coinCode1, currencyCode, timestamp2)).thenReturn(Maybe.just(rate2Value))

        interactor.fetchRate(coin, timestamp1)
        interactor.fetchRate(coin, timestamp2)

        verify(rateManager).rateValueObservable(coinCode1, currencyCode, timestamp1)
        verify(rateManager).rateValueObservable(coinCode1, currencyCode, timestamp2)

        verify(delegate).didFetchRate(rate1Value, coin, currency, timestamp1)
        verify(delegate).didFetchRate(rate2Value, coin, currency, timestamp2)
    }

    @Test
    fun fetchRates_duplicate() {
        val coinCode1 = "BTC"
        val currencyCode = "USD"
        val timestamp1 = 123456L
        val timestamp2 = 34556L
        val timestamp3 = 123123L
        val currency = mock(Currency::class.java)
        val coin = Coin("Bitcoin", coinCode1, CoinType.Bitcoin)

        whenever(currency.code).thenReturn(currencyCode)
        whenever(currencyManager.baseCurrency).thenReturn(currency)

        whenever(rateManager.rateValueObservable(coinCode1, currencyCode, timestamp1)).thenReturn(Maybe.empty())
        whenever(rateManager.rateValueObservable(coinCode1, currencyCode, timestamp2)).thenReturn(Maybe.empty())
        whenever(rateManager.rateValueObservable(coinCode1, currencyCode, timestamp3)).thenReturn(Maybe.empty())

        interactor.fetchRate(coin, timestamp1)
        interactor.fetchRate(coin, timestamp2)
        interactor.fetchRate(coin, timestamp3)
        interactor.fetchRate(coin, timestamp2)

        verify(rateManager).rateValueObservable(coinCode1, currencyCode, timestamp1)
        verify(rateManager).rateValueObservable(coinCode1, currencyCode, timestamp2)
        verify(rateManager).rateValueObservable(coinCode1, currencyCode, timestamp3)
    }

    @Test
    fun fetchRates_sameTimestamp_baseCurencyChanged() {
        interactor.initialFetch()

        val coinCode1 = "BTC"
        val currencyCode = "USD"
        val timestamp1 = 123456L
        val currency = mock(Currency::class.java)
        val coin = Coin("Bitcoin", coinCode1, CoinType.Bitcoin)


        whenever(currency.code).thenReturn(currencyCode)
        whenever(currencyManager.baseCurrency).thenReturn(currency)

        whenever(rateManager.rateValueObservable(coinCode1, currencyCode, timestamp1)).thenReturn(Maybe.empty())

        interactor.fetchRate(coin, timestamp1)

        baseCurrencyUpdatedSignal.onNext(Unit)

        interactor.fetchRate(coin, timestamp1)

        verify(rateManager, times(2)).rateValueObservable(coinCode1, currencyCode, timestamp1)
    }

}
