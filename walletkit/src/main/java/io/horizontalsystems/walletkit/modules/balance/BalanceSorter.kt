package io.horizontalsystems.walletkit.modules.balance

import io.horizontalsystems.walletkit.core.sorting.SortCriterion
import io.horizontalsystems.walletkit.core.sorting.sortedByCriteria

class BalanceSorter {

    fun sort(items: Iterable<BalanceModule.BalanceItem>): List<BalanceModule.BalanceItem> {
        return items.toList().sortedByCriteria(VALUE_CRITERIA)
    }

    companion object {
        val VALUE_CRITERIA = listOf(
            SortCriterion.NonZeroBalanceFirst,
            SortCriterion.HasPriceFirst,
            SortCriterion.FiatBalanceDescending,
            SortCriterion.BalanceDescending,
            SortCriterion.BlockchainOrder,
            SortCriterion.NameAscending,
        )
        val SEND_CRITERIA = listOf(
            SortCriterion.NonZeroBalanceFirst,
            SortCriterion.FiatBalanceDescending,
            SortCriterion.BalanceDescending,
            SortCriterion.MarketCapRank,
            SortCriterion.NameAscending,
        )
    }
}
