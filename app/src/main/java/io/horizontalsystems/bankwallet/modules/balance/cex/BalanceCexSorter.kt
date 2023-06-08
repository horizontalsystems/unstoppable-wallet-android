package cash.p.terminal.modules.balance.cex

import cash.p.terminal.modules.balance.BalanceSortType
import java.math.BigDecimal

class BalanceCexSorter {

    fun sort(items: Iterable<BalanceCexViewItem>, sortType: BalanceSortType): List<BalanceCexViewItem> {
        return when (sortType) {
            BalanceSortType.Value -> sortByBalance(items)
            BalanceSortType.Name -> items.sortedBy { it.coinCode }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.diff }
        }
    }

    private fun sortByBalance(items: Iterable<BalanceCexViewItem>): List<BalanceCexViewItem> {
        val comparator =
                compareByDescending<BalanceCexViewItem> {
                    it.balanceCexItem.balance > BigDecimal.ZERO
                }.thenByDescending {
                    (it.fiatValue ?: BigDecimal.ZERO) > BigDecimal.ZERO
                }.thenByDescending {
                    it.fiatValue
                }.thenByDescending {
                    it.balanceCexItem.balance
                }.thenBy {
                    it.balanceCexItem.coin.code
                }

        return items.sortedWith(comparator)
    }
}
