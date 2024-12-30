package cash.p.terminal.modules.balance

import cash.p.terminal.core.diff
import cash.p.terminal.core.order
import cash.p.terminal.wallet.BalanceSortType
import cash.p.terminal.wallet.balance.BalanceItem
import java.math.BigDecimal

class BalanceSorter {

    fun sort(items: Iterable<BalanceItem>, sortType: BalanceSortType): List<BalanceItem> {
        return when (sortType) {
            BalanceSortType.Value -> sortByBalance(items)
            BalanceSortType.Name -> items.sortedBy { it.wallet.coin.code }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.coinPrice?.diff }
        }
    }

    private fun sortByBalance(items: Iterable<BalanceItem>): List<BalanceItem> {
        val comparator =
                compareByDescending<BalanceItem> {
                    it.balanceData.available > BigDecimal.ZERO
                }.thenByDescending {
                    (it.fiatValue ?: BigDecimal.ZERO) > BigDecimal.ZERO
                }.thenByDescending {
                    it.fiatValue
                }.thenByDescending {
                    it.balanceData.available
                }.thenBy {
                    it.wallet.token.blockchainType.order
                }.thenBy {
                    it.wallet.coin.name
                }

        return items.sortedWith(comparator)
    }
}
