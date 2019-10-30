package io.horizontalsystems.bankwallet.modules.balance

class BalanceSorter : BalanceModule.IBalanceSorter {

    override fun sort(items: List<BalanceModule.BalanceItem>, sortType: BalanceSortType): List<BalanceModule.BalanceItem> {
        return when (sortType) {
            BalanceSortType.Value -> items.sortedByDescending { it.fiatValue }
            BalanceSortType.Name -> items.sortedBy { it.wallet.coin.title }
            BalanceSortType.PercentGrowth -> items.sortedByDescending { it.marketInfo?.diff }
        }
    }
}
