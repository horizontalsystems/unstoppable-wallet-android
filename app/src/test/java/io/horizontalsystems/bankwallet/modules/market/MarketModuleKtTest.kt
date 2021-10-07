//package io.horizontalsystems.bankwallet.modules.market
//
//import io.horizontalsystems.bankwallet.entities.CurrencyValue
//import io.horizontalsystems.coinkit.models.CoinType
//import io.horizontalsystems.core.entities.Currency
//import junit.framework.TestCase
//import org.junit.Assert
//import org.junit.Test
//import java.math.BigDecimal
//
//class MarketModuleKtTest : TestCase() {
//
//    private val usd = Currency("USD", "$", 2)
//
//    private val itemNull = MarketItem(
//            score = null,
//            coinType = CoinType.Bitcoin,
//            coinCode = "BTC",
//            coinName = "Bitcoin",
//            volume = CurrencyValue(usd, BigDecimal.ZERO),
//            rate = CurrencyValue(usd, BigDecimal.ZERO),
//            diff = null,
//            marketCap = null
//    )
//    private val itemOne = MarketItem(
//            score = null,
//            coinType = CoinType.Bitcoin,
//            coinCode = "BTC",
//            coinName = "Bitcoin",
//            volume = CurrencyValue(usd, BigDecimal.ONE),
//            rate = CurrencyValue(usd, BigDecimal.ONE),
//            diff = BigDecimal.ONE,
//            marketCap = CurrencyValue(usd, BigDecimal.ONE)
//    )
//    private val itemTen = MarketItem(
//            score = null,
//            coinType = CoinType.Bitcoin,
//            coinCode = "BTC",
//            coinName = "Bitcoin",
//            volume = CurrencyValue(usd, BigDecimal.TEN),
//            rate = CurrencyValue(usd, BigDecimal.TEN),
//            diff = BigDecimal.TEN,
//            marketCap = CurrencyValue(usd, BigDecimal.TEN)
//    )
//
//    @Test
//    fun testSort_TopLosers() {
//        val list = listOf(itemOne, itemNull, itemTen)
//        val sorted = list.sort(SortingField.TopLosers)
//        val expected = listOf(itemOne, itemTen, itemNull)
//        Assert.assertEquals(expected, sorted)
//    }
//
//    @Test
//    fun testSort_TopGainers() {
//        val list = listOf(itemOne, itemNull, itemTen)
//        val sorted = list.sort(SortingField.TopGainers)
//        val expected = listOf(itemTen, itemOne, itemNull)
//        Assert.assertEquals(expected, sorted)
//    }
//
//    @Test
//    fun testSort_LowestCap() {
//        val list = listOf(itemOne, itemNull, itemTen)
//        val sorted = list.sort(SortingField.LowestCap)
//        val expected = listOf(itemOne, itemTen, itemNull)
//        Assert.assertEquals(expected, sorted)
//    }
//
//    @Test
//    fun testSort_HighestCap() {
//        val list = listOf(itemOne, itemNull, itemTen)
//        val sorted = list.sort(SortingField.HighestCap)
//        val expected = listOf(itemTen, itemOne, itemNull)
//        Assert.assertEquals(expected, sorted)
//    }
//
//    @Test
//    fun testSort_HighestVolume() {
//        val list = listOf(itemOne, itemNull, itemTen)
//        val sorted = list.sort(SortingField.HighestVolume)
//        val expected = listOf(itemTen, itemOne, itemNull)
//        Assert.assertEquals(expected, sorted)
//    }
//
//    @Test
//    fun testSort_LowestVolume() {
//        val list = listOf(itemOne, itemNull, itemTen)
//        val sorted = list.sort(SortingField.LowestVolume)
//        val expected = listOf(itemNull, itemOne, itemTen)
//        Assert.assertEquals(expected, sorted)
//    }
//
//}
