package io.horizontalsystems.bankwallet.modules.balance

/*class BalanceViewItemFactoryTest {

    private val factory = BalanceViewItemFactory()
    val currency = mock(Currency::class.java)
    val coin = mock(Coin::class.java)
    val wallet = mock(Wallet::class.java)
    val state = mock(AdapterState::class.java)
    val coinCode = "coinCode"
    val coinTitle = "coinTitle"

    @Before
    fun setup() {
        whenever(coin.code).thenReturn(coinCode)
        whenever(coin.title).thenReturn(coinTitle)
        whenever(wallet.coin).thenReturn(coin)
    }

    @Test
    fun createViewItem_coinValue() {
        val balance = 12.23.toBigDecimal()
        val item = BalanceModule.BalanceItem(wallet, balance, state)

        val viewItem = factory.createViewItem(item, null)

        Assert.assertEquals(CoinValue(coin, balance), viewItem.coinValue)
    }

    @Test
    fun createViewItem_rateExpired_noRate() {
        val item = BalanceModule.BalanceItem(wallet, BigDecimal.ZERO, state)

        val viewItem = factory.createViewItem(item, null)

        Assert.assertFalse(viewItem.rateExpired)
    }

    @Test
    fun createViewItem_rateExpired_withRate() {
        val rate = mock(Rate::class.java)
        val item = BalanceModule.BalanceItem(wallet, BigDecimal.ZERO, state, rate)

        whenever(rate.expired).thenReturn(true)

        val viewItem = factory.createViewItem(item, null)

        Assert.assertTrue(viewItem.rateExpired)
    }

    @Test
    fun createViewItem_state() {
        val state = AdapterState.Synced
        val item = BalanceModule.BalanceItem(wallet, BigDecimal.ZERO, state)

        val viewItem = factory.createViewItem(item, null)

        Assert.assertEquals(state, viewItem.state)
    }

    @Test
    fun createViewItem_exchangeValue_currencyValue_noRate_withCurrency() {
        val item = BalanceModule.BalanceItem(wallet, BigDecimal.ZERO, state)

        val viewItem = factory.createViewItem(item, currency)

        Assert.assertNull(viewItem.exchangeValue)
        Assert.assertNull(viewItem.currencyValue)
    }

    @Test
    fun createViewItem_exchangeValue_currencyValue_withRate_noCurrency() {
        val rate = Rate("coinCode", "", 123.123.toBigDecimal(), 123L, false)
        val item = BalanceModule.BalanceItem(wallet, BigDecimal.ZERO, state, rate)

        val viewItem = factory.createViewItem(item, null)

        Assert.assertNull(viewItem.exchangeValue)
        Assert.assertNull(viewItem.currencyValue)
    }

    @Test
    fun createViewItem_exchangeValue_currencyValue() {
        val balance = 234.345.toBigDecimal()
        val rate = 123.123123.toBigDecimal()
        val exchangeValue = CurrencyValue(currency, rate)
        val currencyValue = CurrencyValue(currency, rate * balance)
        val item = BalanceModule.BalanceItem(wallet, balance, state, Rate("coinCode", "", rate, 123L, false))

        val viewItem = factory.createViewItem(item, currency)

        Assert.assertEquals(exchangeValue, viewItem.exchangeValue)
        Assert.assertEquals(currencyValue, viewItem.currencyValue)
    }

    @Test
    fun createHeaderViewItem_currencyValue_nullCurrency() {
        val viewItem = factory.createHeaderViewItem(listOf(), false, null)

        Assert.assertNull(viewItem.currencyValue)
    }

    @Test
    fun createHeaderViewItem_currencyValue() {
        val currency = mock(Currency::class.java)

        val balance1 = 10.toBigDecimal()
        val rate1 = 123.toBigDecimal()
        val rateObject1 = mock(Rate::class.java)
        val balanceItem1 = mock(BalanceModule.BalanceItem::class.java)

        val balance2 = 13.toBigDecimal()
        val rate2 = 1245.toBigDecimal()
        val rateObject2 = mock(Rate::class.java)
        val balanceItem2 = mock(BalanceModule.BalanceItem::class.java)

        val expectedCurrencyValue = CurrencyValue(currency, balance1 * rate1 + balance2 * rate2)

        whenever(balanceItem1.balance).thenReturn(balance1)
        whenever(balanceItem1.rate).thenReturn(rateObject1)
        whenever(rateObject1.value).thenReturn(rate1)

        whenever(balanceItem2.balance).thenReturn(balance2)
        whenever(balanceItem2.rate).thenReturn(rateObject2)
        whenever(rateObject2.value).thenReturn(rate2)

        val viewItem = factory.createHeaderViewItem(listOf(balanceItem1, balanceItem2), false, currency)

        Assert.assertEquals(expectedCurrencyValue, viewItem.currencyValue)
    }

    @Test
    fun createHeaderViewItem_currencyValue_withNoRate() {
        val currency = mock(Currency::class.java)

        val balance1 = 10.toBigDecimal()
        val rate1 = 123.toBigDecimal()
        val rateObject1 = mock(Rate::class.java)
        val balanceItem1 = mock(BalanceModule.BalanceItem::class.java)

        val balance2 = 13.toBigDecimal()
        val balanceItem2 = mock(BalanceModule.BalanceItem::class.java)

        val expectedCurrencyValue = CurrencyValue(currency, balance1 * rate1)

        whenever(balanceItem1.balance).thenReturn(balance1)
        whenever(balanceItem1.rate).thenReturn(rateObject1)
        whenever(rateObject1.value).thenReturn(rate1)

        whenever(balanceItem2.balance).thenReturn(balance2)
        whenever(balanceItem2.rate).thenReturn(null)

        val viewItem = factory.createHeaderViewItem(listOf(balanceItem1, balanceItem2), false, currency)

        Assert.assertEquals(expectedCurrencyValue, viewItem.currencyValue)
    }

    @Test
    fun createHeaderViewItem_upToDate() {
        val currency = mock(Currency::class.java)

        val balance1 = 10.toBigDecimal()
        val rate1 = 123.toBigDecimal()
        val rateObject1 = mock(Rate::class.java)
        val balanceItem1 = mock(BalanceModule.BalanceItem::class.java)

        val balance2 = 13.toBigDecimal()
        val rate2 = 1245.toBigDecimal()
        val rateObject2 = mock(Rate::class.java)
        val balanceItem2 = mock(BalanceModule.BalanceItem::class.java)

        whenever(balanceItem1.balance).thenReturn(balance1)
        whenever(balanceItem1.state).thenReturn(AdapterState.Synced)
        whenever(balanceItem1.rate).thenReturn(rateObject1)
        whenever(rateObject1.value).thenReturn(rate1)

        whenever(balanceItem2.balance).thenReturn(balance2)
        whenever(balanceItem2.state).thenReturn(AdapterState.Synced)
        whenever(balanceItem2.rate).thenReturn(rateObject2)
        whenever(rateObject2.value).thenReturn(rate2)

        val viewItem = factory.createHeaderViewItem(listOf(balanceItem1, balanceItem2), false, currency)

        Assert.assertTrue(viewItem.upToDate)
    }

    @Test
    fun createHeaderViewItem_upToDate_rateExpired() {
        val currency = mock(Currency::class.java)

        val balance1 = 10.toBigDecimal()
        val rate1 = 123.toBigDecimal()
        val rateObject1 = mock(Rate::class.java)
        val balanceItem1 = mock(BalanceModule.BalanceItem::class.java)

        val balance2 = 13.toBigDecimal()
        val rate2 = 1245.toBigDecimal()
        val rateObject2 = mock(Rate::class.java)
        val balanceItem2 = mock(BalanceModule.BalanceItem::class.java)

        whenever(balanceItem1.balance).thenReturn(balance1)
        whenever(balanceItem1.rate).thenReturn(rateObject1)
        whenever(rateObject1.value).thenReturn(rate1)
        whenever(rateObject1.expired).thenReturn(false)

        whenever(balanceItem2.balance).thenReturn(balance2)
        whenever(balanceItem2.rate).thenReturn(rateObject2)
        whenever(rateObject2.value).thenReturn(rate2)
        whenever(rateObject2.expired).thenReturn(true)

        val viewItem = factory.createHeaderViewItem(listOf(balanceItem1, balanceItem2), false, currency)

        Assert.assertFalse(viewItem.upToDate)
    }

    @Test
    fun createHeaderViewItem_upToDate_itemNotSynced() {
        val currency = mock(Currency::class.java)

        val balance1 = 10.toBigDecimal()
        val rate1 = 123.toBigDecimal()
        val rateObject1 = mock(Rate::class.java)
        val balanceItem1 = mock(BalanceModule.BalanceItem::class.java)

        val balance2 = 13.toBigDecimal()
        val rate2 = 1245.toBigDecimal()
        val rateObject2 = mock(Rate::class.java)
        val balanceItem2 = mock(BalanceModule.BalanceItem::class.java)

        whenever(balanceItem1.balance).thenReturn(balance1)
        whenever(balanceItem1.state).thenReturn(AdapterState.Synced)
        whenever(balanceItem1.rate).thenReturn(rateObject1)
        whenever(rateObject1.value).thenReturn(rate1)
        whenever(rateObject1.expired).thenReturn(false)

        whenever(balanceItem2.balance).thenReturn(balance2)
        whenever(balanceItem2.rate).thenReturn(rateObject2)
        whenever(balanceItem2.state).thenReturn(AdapterState.NotSynced)
        whenever(rateObject2.value).thenReturn(rate2)
        whenever(rateObject2.expired).thenReturn(false)

        val viewItem = factory.createHeaderViewItem(listOf(balanceItem1, balanceItem2), false, currency)

        Assert.assertFalse(viewItem.upToDate)
    }

    @Test
    fun createHeaderViewItem_upToDate_noRate() {
        val currency = mock(Currency::class.java)

        val balance1 = 10.toBigDecimal()
        val rate1 = 123.toBigDecimal()
        val rateObject1 = mock(Rate::class.java)
        val balanceItem1 = mock(BalanceModule.BalanceItem::class.java)

        val balance2 = 13.toBigDecimal()
        val balanceItem2 = mock(BalanceModule.BalanceItem::class.java)

        whenever(balanceItem1.balance).thenReturn(balance1)
        whenever(balanceItem1.rate).thenReturn(rateObject1)
        whenever(rateObject1.value).thenReturn(rate1)

        whenever(balanceItem2.balance).thenReturn(balance2)
        whenever(balanceItem2.rate).thenReturn(null)

        val viewItem = factory.createHeaderViewItem(listOf(balanceItem1, balanceItem2), false, currency)

        Assert.assertFalse(viewItem.upToDate)
    }

    @Test
    fun createHeaderViewItem_upToDate_noRate_zeroBalance() {
        val currency = mock(Currency::class.java)

        val balanceItem = mock(BalanceModule.BalanceItem::class.java)

        whenever(balanceItem.balance).thenReturn(BigDecimal.ZERO)
        whenever(balanceItem.rate).thenReturn(null)

        val viewItem = factory.createHeaderViewItem(listOf(balanceItem), false, currency)

        Assert.assertTrue(viewItem.upToDate)
    }

}*/