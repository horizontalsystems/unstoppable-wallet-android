package io.horizontalsystems.bankwallet.modules.balance

import java.math.BigDecimal

class BalanceSorter : BalanceModule.IBalanceSorter {

    override fun sort(items: List<BalanceModule.BalanceItem>, sortType: BalanceSortType): List<BalanceModule.BalanceItem> {
        return when (sortType) {
            BalanceSortType.Value -> sortByBalance(items)
            BalanceSortType.Name -> items.sortedBy { it.wallet.coin.title }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.marketInfo?.diff }
        }
    }

    private fun sortByBalance(items: List<BalanceModule.BalanceItem>): List<BalanceModule.BalanceItem> {
        return items.sortedWith(compareBy(
                {
                    if ((it.balance ?: BigDecimal.ZERO) > BigDecimal.ZERO) 1 else 0
                },
                {
                    if ((it.fiatValue?: BigDecimal.ZERO) > BigDecimal.ZERO) 1 else 0
                },
                {
                    it.fiatValue
                },
                {
                    it.balance
                }
        )).reversed()
    }

}
