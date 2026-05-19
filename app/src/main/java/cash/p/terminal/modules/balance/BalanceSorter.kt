package cash.p.terminal.modules.balance

import cash.p.terminal.core.diffPercentage
import cash.p.terminal.core.order
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.wallet.balance.BalanceItem
import java.math.BigDecimal

class BalanceSorter {

    fun sort(items: Iterable<BalanceItem>, sortType: BalanceSortType): List<BalanceItem> {
        return when (sortType) {
            BalanceSortType.Value -> sortByBalance(items)
            BalanceSortType.Name -> items.sortedBy { it.wallet.coin.code }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.coinPrice?.diffPercentage }
        }
    }

    private fun sortByBalance(items: Iterable<BalanceItem>): List<BalanceItem> {
        val comparator =
                compareByDescending<BalanceItem> {
                    it.balanceData.total > BigDecimal.ZERO
                }.thenByDescending {
                    (it.balanceFiatTotal ?: BigDecimal.ZERO) > BigDecimal.ZERO
                }.thenByDescending {
                    it.balanceFiatTotal
                }.thenByDescending {
                    it.balanceData.total
                }.thenBy {
                    it.wallet.token.blockchainType.order
                }.thenBy {
                    it.wallet.coin.name
                }

        return items.sortedWith(comparator)
    }
}
