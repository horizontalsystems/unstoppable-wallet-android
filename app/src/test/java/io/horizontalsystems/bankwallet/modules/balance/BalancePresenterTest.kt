package io.horizontalsystems.bankwallet.modules.balance

/*class BalancePresenterTest {

    private val interactor = mock(BalanceModule.IInteractor::class.java)
    private val view = mock(BalanceModule.IView::class.java)
    private val router = mock(BalanceModule.IRouter::class.java)
    private val dataSource = mock(BalanceModule.DataSource::class.java)
    private val factory = mock(BalanceViewItemFactory::class.java)

    private lateinit var presenter: BalancePresenter
    private val testScheduler = TestScheduler()

    @Before
    fun before() {
        RxBaseTest.setup()
        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }

        presenter = BalancePresenter(interactor, router, dataSource, mock(IPredefinedAccountTypeManager::class.java), factory)
        presenter.view = view
    }

    @Test
    fun viewDidLoad() {
        whenever(dataSource.sortType).thenReturn(BalanceSortType.Name)

        presenter.viewDidLoad()
        testScheduler.advanceTimeBy(1, TimeUnit.MINUTES)

        verify(interactor).initWallets()
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
        val chartEnabled = false

        whenever(dataSource.items).thenReturn(items)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(factory.createHeaderViewItem(items, chartEnabled, currency)).thenReturn(viewItem)

        Assert.assertEquals(viewItem, presenter.getHeaderViewItem())
    }

    @Test
    fun didUpdateWallets() {
        val title = "title"
        val coinCode = "coinCode"
        val currencyCode = "currencyCode"
        val currency = mock(Currency::class.java)
        val account = mock(Account::class.java)
        val coin = mock(Coin::class.java)
        val wallet = mock(Wallet::class.java)
        val balanceAdapter = mock(IBalanceAdapter::class.java)
        val balance = BigDecimal(12.23)
        val state = mock(AdapterState::class.java)
        val coinCodes = listOf(coinCode)

        val wallets = listOf(wallet)

        val items = listOf(BalanceModule.BalanceItem(wallet, balance, state))

        whenever(interactor.getBalanceAdapterForWallet(wallet)).thenReturn(balanceAdapter)
        whenever(balanceAdapter.balance).thenReturn(balance)
        whenever(balanceAdapter.state).thenReturn(state)
        whenever(coin.code).thenReturn(coinCode)
        whenever(coin.title).thenReturn(title)
        whenever(wallet.coin).thenReturn(coin)
        whenever(wallet.account).thenReturn(account)
        whenever(account.isBackedUp).thenReturn(false)
        whenever(currency.code).thenReturn(currencyCode)
        whenever(dataSource.currency).thenReturn(currency)
        whenever(dataSource.coinCodes).thenReturn(coinCodes)

        presenter.didUpdateWallets(wallets)

        verify(dataSource).set(items)
        verify(interactor).fetchRates(currencyCode, coinCodes)
        verify(view).reload()
        verify(view).setSortingOn(false)
    }

    @Test
    fun didUpdateBalance() {
        val wallet = mock(Wallet::class.java)
        val position = 5
        val balance = 123123.123.toBigDecimal()

        whenever(dataSource.getPosition(wallet)).thenReturn(position)

        presenter.didUpdateBalance(wallet, balance)

        verify(dataSource).setBalance(position, balance)
        verify(dataSource).addUpdatedPosition(position)
        verify(view).updateHeader()
    }

    @Test
    fun didUpdateState() {
        val wallet = mock(Wallet::class.java)
        val position = 5
        val state = AdapterState.Synced

        whenever(dataSource.getPosition(wallet)).thenReturn(position)

        presenter.didUpdateState(wallet, state)

        verify(dataSource).setState(position, state)
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
        val rate = mock<Rate>()

        whenever(rate.coinCode).thenReturn(coinCode)
        whenever(dataSource.getPositionsByCoinCode(coinCode)).thenReturn(listOf(position))

        presenter.didUpdateRate(rate)

        verify(dataSource).setRate(position, rate)
    }

    @Test
    fun onReceive() {
        val position = 5
        val item = mock<BalanceModule.BalanceItem>()
        val wallet = mock<Wallet>()
        val account = mock<Account>()

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(item.wallet).thenReturn(wallet)
        whenever(wallet.account).thenReturn(account)
        whenever(account.isBackedUp).thenReturn(true)

        presenter.onReceive(position)

        verify(router).openReceiveDialog(wallet)
    }

    @Test
    fun onPay() {
        val position = 5
        val coinCode = "coinCode"
        val item = mock(BalanceModule.BalanceItem::class.java)
        val coin = mock(Coin::class.java)
        val wallet = mock(Wallet::class.java)

        whenever(dataSource.getItem(position)).thenReturn(item)
        whenever(item.wallet).thenReturn(wallet)

        presenter.onPay(position)

        verify(router).openSendDialog(wallet)
    }

    @Test
    fun onClear() {
        presenter.onClear()

        verify(interactor).clear()
    }

}

 */
