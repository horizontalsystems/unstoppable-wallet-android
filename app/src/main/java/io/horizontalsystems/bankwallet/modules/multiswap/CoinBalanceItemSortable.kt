package io.horizontalsystems.bankwallet.modules.multiswap

import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.sorting.SortComparators
import io.horizontalsystems.bankwallet.core.sorting.SortCriterion
import io.horizontalsystems.bankwallet.core.sorting.TokenSortContext
import io.horizontalsystems.bankwallet.core.sorting.tokenFilterRelevanceComparator

fun List<CoinBalanceItem>.sortedByCriteria(
    criteria: List<SortCriterion>,
    context: TokenSortContext = TokenSortContext()
): List<CoinBalanceItem> {
    if (criteria.isEmpty()) return this
    val comparator = criteria
        .map { it.toCoinBalanceItemComparator(context) }
        .reduce { acc, next -> acc.thenComparing(next) }
    return sortedWith(comparator)
}

private fun SortCriterion.toCoinBalanceItemComparator(ctx: TokenSortContext): Comparator<CoinBalanceItem> =
    when (this) {
        is SortCriterion.SameBlockchainFirst ->
            SortComparators.booleanFirst { it.token.blockchainType == blockchainType }
        is SortCriterion.FiatBalanceDescending ->
            SortComparators.nullableDecimalDescending { it.fiatBalanceValue?.value }
        is SortCriterion.BalanceDescending ->
            SortComparators.nullableDecimalDescending { it.balance }
        is SortCriterion.CodeAscending ->
            SortComparators.nullableStringAscending { it.token.coin.code }
        is SortCriterion.NameAscending ->
            SortComparators.nullableStringAscending { it.token.coin.name }
        is SortCriterion.BlockchainOrder ->
            compareBy { it.token.blockchainType.order }
        is SortCriterion.Badge ->
            SortComparators.nullableStringAscending { it.token.badge }
        is SortCriterion.CodeNativeFirst ->
            SortComparators.booleanFirst { it.token.type.isNative }
        is SortCriterion.Enabled ->
            SortComparators.booleanFirst { it.balance != null }
        is SortCriterion.FilterRelevance -> {
            val tokenComp = tokenFilterRelevanceComparator(ctx.filter)
            Comparator { a, b -> tokenComp.compare(a.token, b.token) }
        }
        else -> Comparator { _, _ -> 0 }
    }
