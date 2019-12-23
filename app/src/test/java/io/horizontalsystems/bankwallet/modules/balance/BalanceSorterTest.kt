package io.horizontalsystems.bankwallet.modules.balance

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Test

class BalanceSorterTest {

    private val sorter = BalanceSorter()

    @Test
    fun sort_WithZeroAndNull(){

        val item1 = balMock(9f, null)
        val item2 = balMock(0f, 0f)
        val item3 = balMock(null, 0f)
        val item4 = balMock(3f, 1f)

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

        val item1 = balMock(10f, null)
        val item2 = balMock(9f, null)

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

        val item1 = balMock(9f, null)
        val item2 = balMock(9f, 0f)

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

        val item1 = balMock(9f, 0f)
        val item2 = balMock(3f, 12f)
        val item3 = balMock(40f, null)

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

        val item1 = balMock(6f, 1f)
        val item2 = balMock(3f, 12f)
        val item3 = balMock(40f, null)
        val item4 = balMock(0f, null)

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

        val item1 = balMock(15f, null)
        val item2 = balMock(9f, 0f)
        val item3 = balMock(3f, 1f)

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

    private fun balMock(balance: Float?, fiatValue: Float?): BalanceModule.BalanceItem {
        val mockedItem = mock<BalanceModule.BalanceItem>()
        whenever(mockedItem.balance).thenReturn(balance?.toBigDecimal())
        whenever(mockedItem.fiatValue).thenReturn(fiatValue?.toBigDecimal())
        return mockedItem
    }

}
