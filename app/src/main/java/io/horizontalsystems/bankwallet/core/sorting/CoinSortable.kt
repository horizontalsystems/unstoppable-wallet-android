package io.horizontalsystems.bankwallet.core.sorting

import io.horizontalsystems.bankwallet.core.badge
import io.horizontalsystems.bankwallet.core.diff
import io.horizontalsystems.bankwallet.core.isNative
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.modules.balance.BalanceModule
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.Token
import java.math.BigDecimal
import java.util.Locale

// ---- BalanceItem ----------------------------------------------------------------

fun List<BalanceModule.BalanceItem>.sortedByCriteria(
    criteria: List<SortCriterion>
): List<BalanceModule.BalanceItem> {
    if (criteria.isEmpty()) return this
    val comparator = criteria
        .map { it.toBalanceItemComparator() }
        .reduce { acc, next -> acc.thenComparing(next) }
    return sortedWith(comparator)
}

private fun SortCriterion.toBalanceItemComparator(): Comparator<BalanceModule.BalanceItem> =
    when (this) {
        is SortCriterion.NonZeroBalanceFirst ->
            SortComparators.booleanFirst { it.balanceData.total > BigDecimal.ZERO }
        is SortCriterion.HasPriceFirst ->
            SortComparators.booleanFirst { (it.balanceFiatTotal ?: BigDecimal.ZERO) > BigDecimal.ZERO }
        is SortCriterion.FiatBalanceDescending ->
            SortComparators.nullableDecimalDescending { it.balanceFiatTotal }
        is SortCriterion.BalanceDescending ->
            SortComparators.nullableDecimalDescending { it.balanceData.total }
        is SortCriterion.PercentGrowthDescending ->
            SortComparators.nullableDecimalDescendingNullLast { it.coinPrice?.diff }
        is SortCriterion.MarketCapRank ->
            SortComparators.nullableIntAscending { it.wallet.coin.marketCapRank }
        is SortCriterion.BlockchainOrder ->
            compareBy { it.wallet.token.blockchainType.order }
        is SortCriterion.CodeAscending ->
            SortComparators.nullableStringAscending { it.wallet.coin.code }
        is SortCriterion.NameAscending ->
            SortComparators.nullableStringAscending { it.wallet.coin.name }
        else -> Comparator { _, _ -> 0 }
    }

// ---- Token ----------------------------------------------------------------------

fun List<Token>.sortedByCriteria(
    criteria: List<SortCriterion>,
    context: TokenSortContext = TokenSortContext()
): List<Token> {
    if (criteria.isEmpty()) return this
    val comparator = criteria
        .map { it.toTokenComparator(context) }
        .reduce { acc, next -> acc.thenComparing(next) }
    return sortedWith(comparator)
}

private fun SortCriterion.toTokenComparator(ctx: TokenSortContext): Comparator<Token> =
    when (this) {
        is SortCriterion.Enabled ->
            SortComparators.booleanFirst { ctx.enabledTokens.contains(it) }
        is SortCriterion.SameBlockchainFirst ->
            SortComparators.booleanFirst { it.blockchainType == blockchainType }
        is SortCriterion.FiatBalanceDescending ->
            SortComparators.nullableDecimalDescending { ctx.fiatValues[it] }
        is SortCriterion.MarketCapRank ->
            SortComparators.nullableIntAscending { it.coin.marketCapRank }
        is SortCriterion.BlockchainOrder ->
            compareBy { it.blockchainType.order }
        is SortCriterion.Badge ->
            SortComparators.nullableStringAscending { it.badge }
        is SortCriterion.CodeAscending ->
            SortComparators.nullableStringAscending { it.coin.code }
        is SortCriterion.NameAscending ->
            SortComparators.nullableStringAscending { it.coin.name }
        is SortCriterion.CodeNativeFirst ->
            SortComparators.booleanFirst { it.type.isNative }
        is SortCriterion.FilterRelevance ->
            tokenFilterRelevanceComparator(ctx.filter)
        else -> Comparator { _, _ -> 0 }
    }

// ---- FullCoin -------------------------------------------------------------------

fun List<FullCoin>.sortedByCriteria(
    criteria: List<SortCriterion>,
    context: FullCoinSortContext = FullCoinSortContext()
): List<FullCoin> {
    if (criteria.isEmpty()) return this
    val comparator = criteria
        .map { it.toFullCoinComparator(context) }
        .reduce { acc, next -> acc.thenComparing(next) }
    return sortedWith(comparator)
}

private fun SortCriterion.toFullCoinComparator(ctx: FullCoinSortContext): Comparator<FullCoin> =
    when (this) {
        is SortCriterion.Enabled ->
            SortComparators.booleanFirst { ctx.activeCoins.contains(it) }
        is SortCriterion.FiatBalanceDescending ->
            SortComparators.nullableDecimalDescending { ctx.fiatValues[it] }
        is SortCriterion.MarketCapRank ->
            SortComparators.nullableIntAscending { it.coin.marketCapRank }
        is SortCriterion.BlockchainOrder ->
            compareBy { it.tokens.minOfOrNull { t -> t.blockchainType.order } ?: Int.MAX_VALUE }
        is SortCriterion.CodeAscending ->
            SortComparators.nullableStringAscending { it.coin.code }
        is SortCriterion.NameAscending ->
            SortComparators.nullableStringAscending { it.coin.name }
        is SortCriterion.CodeNativeFirst ->
            SortComparators.booleanFirst { fullCoin -> fullCoin.tokens.any { it.type.isNative } }
        is SortCriterion.FilterRelevance ->
            fullCoinFilterRelevanceComparator(ctx.filter)
        else -> Comparator { _, _ -> 0 }
    }

// ---- Filter relevance helpers ---------------------------------------------------

internal fun tokenFilterRelevanceComparator(filter: String): Comparator<Token> {
    if (filter.isBlank()) return Comparator { _, _ -> 0 }
    val lowercased = filter.lowercase(Locale.ENGLISH)
    return compareByDescending<Token> { it.coin.code.lowercase(Locale.ENGLISH) == lowercased }
        .thenByDescending { it.coin.code.lowercase(Locale.ENGLISH).startsWith(lowercased) }
        .thenByDescending { it.coin.name.lowercase(Locale.ENGLISH).startsWith(lowercased) }
}

private fun fullCoinFilterRelevanceComparator(filter: String): Comparator<FullCoin> {
    if (filter.isBlank()) return Comparator { _, _ -> 0 }
    val lowercased = filter.lowercase(Locale.ENGLISH)
    return compareByDescending<FullCoin> { it.coin.code.lowercase(Locale.ENGLISH) == lowercased }
        .thenByDescending { it.coin.code.lowercase(Locale.ENGLISH).startsWith(lowercased) }
        .thenByDescending { it.coin.name.lowercase(Locale.ENGLISH).startsWith(lowercased) }
}
