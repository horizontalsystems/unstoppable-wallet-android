package io.horizontalsystems.bankwallet.core.sorting

import io.horizontalsystems.marketkit.models.BlockchainType

sealed class SortCriterion {
    object NonZeroBalanceFirst : SortCriterion()
    object HasPriceFirst : SortCriterion()
    object FiatBalanceDescending : SortCriterion()
    object BalanceDescending : SortCriterion()
    object PercentGrowthDescending : SortCriterion()
    object MarketCapRank : SortCriterion()
    object BlockchainOrder : SortCriterion()
    object Badge : SortCriterion()
    object CodeAscending : SortCriterion()
    object NameAscending : SortCriterion()
    object Enabled : SortCriterion()
    object FilterRelevance : SortCriterion()
    object CodeNativeFirst : SortCriterion()
    data class SameBlockchainFirst(val blockchainType: BlockchainType) : SortCriterion()
}
