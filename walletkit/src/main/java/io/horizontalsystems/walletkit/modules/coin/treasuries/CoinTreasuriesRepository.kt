package io.horizontalsystems.walletkit.modules.coin.treasuries

import io.horizontalsystems.walletkit.core.managers.MarketKitWrapper
import io.horizontalsystems.walletkit.modules.coin.treasuries.CoinTreasuriesModule.TreasuryTypeFilter
import io.horizontalsystems.marketkit.models.CoinTreasury
import io.horizontalsystems.marketkit.models.CoinTreasury.TreasuryType

class CoinTreasuriesRepository(
    private val marketKit: MarketKitWrapper
) {
    private var cache: List<CoinTreasury> = listOf()

    private suspend fun getCoinTreasuries(coinUid: String, currencyCode: String, forceRefresh: Boolean): List<CoinTreasury> {
        if (forceRefresh || cache.isEmpty()) {
            cache = marketKit.treasuriesSingle(coinUid, currencyCode)
        }
        return cache
    }

    suspend fun coinTreasuriesSingle(
        coinUid: String,
        currencyCode: String,
        treasuryType: TreasuryTypeFilter,
        sortDescending: Boolean,
        forceRefresh: Boolean
    ): List<CoinTreasury> {
        val treasuries = getCoinTreasuries(coinUid, currencyCode, forceRefresh)
        val filteredTreasuries = treasuries.filter {
            when (treasuryType) {
                TreasuryTypeFilter.All -> true
                TreasuryTypeFilter.Public -> it.type == TreasuryType.Public
                TreasuryTypeFilter.Private -> it.type == TreasuryType.Private
                TreasuryTypeFilter.ETF -> it.type == TreasuryType.Etf
            }
        }
        return if (sortDescending) {
            filteredTreasuries.sortedByDescending { it.amount }
        } else {
            filteredTreasuries.sortedBy { it.amount }
        }
    }
}
