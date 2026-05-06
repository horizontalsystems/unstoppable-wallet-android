package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.core.sorting.SortCriterion
import io.horizontalsystems.bankwallet.core.sorting.sortedByCriteria

class BalanceSorter {

    fun sort(items: Iterable<BalanceModule.BalanceItem>, sortType: BalanceSortType): List<BalanceModule.BalanceItem> {
        val criteria = when (sortType) {
            BalanceSortType.Value -> VALUE_CRITERIA
            BalanceSortType.Name -> NAME_CRITERIA
            BalanceSortType.PercentGrowth -> PERCENT_GROWTH_CRITERIA
        }
        return items.toList().sortedByCriteria(criteria)
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
        val NAME_CRITERIA = listOf(SortCriterion.CodeAscending)
        val PERCENT_GROWTH_CRITERIA = listOf(SortCriterion.PercentGrowthDescending)
    }
}
