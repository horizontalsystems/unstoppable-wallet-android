package io.horizontalsystems.bankwallet.modules.balance

import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import io.horizontalsystems.bankwallet.entities.Coin
import io.horizontalsystems.bankwallet.entities.CoinType
import io.horizontalsystems.bankwallet.entities.Wallet
import org.junit.Assert
import org.junit.Test

class BalanceSorterTest {

    private val sorter = BalanceSorter()

    @Test
    fun sort_byBalance_equalItemsSortedByName() {
        val btc = mockBalanceItem(0f, 0f, "Bitcoin")
        val zrx = mockBalanceItem(0f, 0f, "0x Protocol")
        val eth = mockBalanceItem(0f, 0f, "Ethereum")

        val items = listOf(btc, zrx, eth)
        val expected = listOf(zrx, btc, eth)

        val sorted = sorter.sort(items, BalanceSortType.Value)
        val sortedReversed = sorter.sort(items.reversed(), BalanceSortType.Value)

        Assert.assertArrayEquals(expected.toTypedArray(), sorted.toTypedArray())
        Assert.assertArrayEquals(expected.toTypedArray(), sortedReversed.toTypedArray())
    }

    @Test
    fun sort_WithZeroAndNull(){

        val item1 = mockBalanceItem(9f, null)
        val item2 = mockBalanceItem(0f, 0f)
        val item3 = mockBalanceItem(null, 0f)
        val item4 = mockBalanceItem(3f, 1f)

        val items = listOf(
                item1,
                item2,
                item3,
                item4
        )
        val expectedSortedList = listOf(
                item4,
                item1,
                item2,
                item3
        )

        val sorted = sorter.sort(items, BalanceSortType.Value)

        Assert.assertEquals(expectedSortedList, sorted)
    }

    @Test
    fun sort_WithNullFiatNull(){

        val item1 = mockBalanceItem(10f, null)
        val item2 = mockBalanceItem(9f, null)

        val items = listOf(
                item1,
                item2
        )
        val expectedSortedList = listOf(
                item1,
                item2
        )

        val sorted = sorter.sort(items, BalanceSortType.Value)

        Assert.assertEquals(expectedSortedList, sorted)
    }

    @Test
    fun sort_SecondOneHasRate(){

        val item1 = mockBalanceItem(9f, null)
        val item2 = mockBalanceItem(9f, 0f)

        val items = listOf(
                item1,
                item2
        )
        val expectedSortedList = listOf(
                item2,
                item1
        )

        val sorted = sorter.sort(items, BalanceSortType.Value)

        Assert.assertEquals(expectedSortedList, sorted)
    }

    @Test
    fun sort_WithZeroAndValues(){

        val item1 = mockBalanceItem(9f, 0f)
        val item2 = mockBalanceItem(3f, 12f)
        val item3 = mockBalanceItem(40f, null)

        val items = listOf(
                item1,
                item2,
                item3
        )
        val expectedSortedList = listOf(
                item2,
                item1,
                item3
        )

        val sorted = sorter.sort(items, BalanceSortType.Value)

        Assert.assertEquals(expectedSortedList, sorted)
    }

    @Test
    fun sort_WithNullBalance(){

        val item1 = mockBalanceItem(6f, 1f)
        val item2 = mockBalanceItem(3f, 12f)
        val item3 = mockBalanceItem(40f, null)
        val item4 = mockBalanceItem(0f, null)

        val items = listOf(
                item1,
                item2,
                item3,
                item4
        )
        val expectedSortedList = listOf(
                item2,
                item1,
                item3,
                item4
        )

        val sorted = sorter.sort(items, BalanceSortType.Value)

        Assert.assertEquals(expectedSortedList, sorted)
    }

    @Test
    fun sort_SecondOneHasRateButLessInBalance(){

        val item1 = mockBalanceItem(15f, null)
        val item2 = mockBalanceItem(9f, 0f)
        val item3 = mockBalanceItem(3f, 1f)

        val items = listOf(
                item1,
                item2,
                item3
        )
        val expectedSortedList = listOf(
                item3,
                item2,
                item1
        )

        val sorted = sorter.sort(items, BalanceSortType.Value)

        Assert.assertEquals(expectedSortedList, sorted)
    }

    private fun mockBalanceItem(balance: Float?, fiatValue: Float?, title: String = ""): BalanceModule.BalanceItem {
        return mock {
            on { this.balance } doReturn balance?.toBigDecimal()
            on { this.fiatValue } doReturn fiatValue?.toBigDecimal()

            on { wallet } doReturn Wallet(Coin("coinId", title, "code", 8, CoinType.Bitcoin), mock())
        }
    }
}
