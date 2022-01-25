package io.horizontalsystems.bankwallet.modules.balance

import io.horizontalsystems.bankwallet.modules.balance2.BalanceModule2
import java.math.BigDecimal

class BalanceSorter {

    fun sort(items: Iterable<BalanceModule2.BalanceItem>, sortType: BalanceSortType): List<BalanceModule2.BalanceItem> {
        return when (sortType) {
            BalanceSortType.Value -> sortByBalance(items)
            BalanceSortType.Name -> items.sortedBy { it.wallet.coin.code }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.coinPrice?.diff }
        }
    }

    private fun sortByBalance(items: Iterable<BalanceModule2.BalanceItem>): List<BalanceModule2.BalanceItem> {
        val comparator =
                compareByDescending<BalanceModule2.BalanceItem> {
                    it.balanceData.available > BigDecimal.ZERO
                }.thenByDescending {
                    it.fiatValue ?: BigDecimal.ZERO > BigDecimal.ZERO
                }.thenByDescending {
                    it.fiatValue
                }.thenByDescending {
                    it.balanceData.available
                }.thenBy {
                    it.wallet.coin.name
                }

        return items.sortedWith(comparator)
    }
}
