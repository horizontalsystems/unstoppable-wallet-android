package io.horizontalsystems.bankwallet.modules.balance

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.horizontalsystems.bankwallet.core.AdapterState
import io.horizontalsystems.bankwallet.core.IAdapter
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.Rate
import io.horizontalsystems.bankwallet.modules.RxBaseTest
import io.horizontalsystems.bankwallet.modules.transactions.CoinCode
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import java.math.BigDecimal
import java.util.concurrent.TimeUnit

class BalancePresenterTest {

    private val interactor = mock(BalanceModule.IInteractor::class.java)
    private val view = mock(BalanceModule.IView::class.java)
    private val router = mock(BalanceModule.IRouter::class.java)
    private val dataSource = mock(BalanceModule.BalanceItemDataSource::class.java)
    private val factory = mock(BalanceViewItemFactory::class.java)

    private lateinit var presenter: BalancePresenter
    private val testScheduler = TestScheduler()

    @Before
    fun before() {
        RxBaseTest.setup()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }

        presenter = BalancePresenter(interactor, router, dataSource, factory)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        whenever(dataSource.balanceSortType).thenReturn(BalanceSortType.Default)

        presenter.viewDidLoad()
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)

        verify(interactor).initAdapters()
    }

    @Test
    fun itemsCount() {
        val itemsCount = 123

        whenever(dataSource.count).thenReturn(itemsCount)

        Assert.assertEquals(itemsCount, presenter.itemsCount)
    }

    @Test
    fun getViewItem() {
        val position = 324

        val item = mock(BalanceModule.BalanceItem::class.java)
        val viewItem = mock(BalanceViewItem::class.java)
        val currency = mock(Currency::class.java)

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(factory.createViewItem(item, currency)).thenReturn(viewItem)

        Assert.assertEquals(viewItem, presenter.getViewItem(position))
    }

    @Test
    fun getHeaderViewItem() {
        val items = listOf<BalanceModule.BalanceItem>()
        val viewItem = mock(BalanceHeaderViewItem::class.java)
        val currency = mock(Currency::class.java)

        whenever(dataSource.items).thenReturn(items)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(factory.createHeaderViewItem(items, currency)).thenReturn(viewItem)

        Assert.assertEquals(viewItem, presenter.getHeaderViewItem())
    }

    @Test
    fun didUpdateWallets() {
        val title = "title"
        val coinCode = "coinCode"
        val currencyCode = "currencyCode"
        val currency = mock(Currency::class.java)
        val coin = mock(Coin::class.java)
        val balance = BigDecimal(12.23)
        val state = mock(AdapterState::class.java)
        val coinCodes = listOf(coinCode)

        val adapter = mock(IAdapter::class.java)
        val adapters = listOf(adapter)

        val items = listOf(BalanceModule.BalanceItem(coin, balance, state))

        whenever(coin.code).thenReturn(coinCode)
        whenever(coin.title).thenReturn(title)
        whenever(adapter.wallet).thenReturn(coin)
        whenever(adapter.balance).thenReturn(balance)
        whenever(adapter.state).thenReturn(state)
        whenever(currency.code).thenReturn(currencyCode)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(dataSource.coinCodes).thenReturn(coinCodes)

        presenter.didUpdateAdapters(adapters)

        verify(dataSource).set(items)
        verify(interactor).fetchRates(currencyCode, coinCodes)
        verify(view).reload()
        verify(view).setSortingOn(false)
    }

    @Test
    fun didUpdateWallets_nullCurrency() {
        whenever(dataSource.currency).thenReturn(null)

        presenter.didUpdateAdapters(listOf())

        verify(interactor, never()).fetchRates(any(), any())
    }

    @Test
    fun didUpdateBalance() {
        val coinCode = "coinCode"
        val position = 5
        val balance = 123123.123.toBigDecimal()

        whenever(dataSource.getPosition(coinCode)).thenReturn(position)

        presenter.didUpdateBalance(coinCode, balance)

        verify(dataSource).setBalance(position, balance)
        verify(dataSource).addUpdatedPosition(position)
        verify(view).updateHeader()
    }

    @Test
    fun didUpdateState() {
        val coinCode = "ABC"
        val position = 5
        val state = AdapterState.Synced

        whenever(dataSource.getPosition(coinCode)).thenReturn(position)

        presenter.didUpdateState(coinCode, state)

        verify(dataSource).setState(position, state)
        verify(dataSource).addUpdatedPosition(position)
        verify(view).updateHeader()
    }

    @Test
    fun didUpdateCurrency() {
        val currencyCode = "USD"
        val currency = mock(Currency::class.java)
        val coinCodes = listOf<CoinCode>()

        whenever(currency.code).thenReturn(currencyCode)
        whenever(dataSource.coinCodes).thenReturn(coinCodes)

        presenter.didUpdateCurrency(currency)

        verify(dataSource).currency = currency
        verify(dataSource).clearRates()
        verify(interactor).fetchRates(currencyCode, coinCodes)
        verify(view).reload()
    }

    @Test
    fun didUpdateRate() {
        val coinCode = "ABC"
        val position = 5
        val rate = mock(Rate::class.java)

        whenever(rate.coinCode).thenReturn(coinCode)
        whenever(dataSource.getPosition(coinCode)).thenReturn(position)

        presenter.didUpdateRate(rate)

        verify(dataSource).setRate(position, rate)
        verify(dataSource).addUpdatedPosition(position)
        verify(view).updateHeader()
    }

    @Test
    fun onReceive() {
        val position = 5
        val coinCode = "coinCode"
        val item = mock(BalanceModule.BalanceItem::class.java)
        val coin = mock(Coin::class.java)

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(coin.code).thenReturn(coinCode)
        whenever(item.coin).thenReturn(coin)

        presenter.onReceive(position)

        verify(router).openReceiveDialog(coinCode)
    }

    @Test
    fun onPay() {
        val position = 5
        val coinCode = "coinCode"
        val item = mock(BalanceModule.BalanceItem::class.java)
        val coin = mock(Coin::class.java)

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(coin.code).thenReturn(coinCode)
        whenever(item.coin).thenReturn(coin)

        presenter.onPay(position)

        verify(router).openSendDialog(coinCode)
    }

    @Test
    fun onClear() {
        presenter.onClear()

        verify(interactor).clear()
    }

}
