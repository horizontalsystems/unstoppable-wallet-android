package io.horizontalsystems.bankwallet.modules.balance

import java.math.BigDecimal

class BalanceSorter {

    fun sort(items: Iterable<BalanceModule.BalanceItem>, sortType: BalanceSortType): List<BalanceModule.BalanceItem> {
        return when (sortType) {
            BalanceSortType.Value -> sortByBalance(items)
            BalanceSortType.Name -> items.sortedBy { it.wallet.coin.code }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.coinPrice?.diff }
        }
    }

    private fun sortByBalance(items: Iterable<BalanceModule.BalanceItem>): List<BalanceModule.BalanceItem> {
        val comparator =
                compareByDescending<BalanceModule.BalanceItem> {
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
