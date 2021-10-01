package io.horizontalsystems.bankwallet.modules.market.search

import io.horizontalsystems.bankwallet.core.Clearable
import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.marketkit.models.FullCoin

class MarketSearchService(private val coinManager: ICoinManager) : Clearable {

    fun getCoinsByQuery(query: String): List<FullCoin> {
        return coinManager.fullCoins(query, 100)
    }

    override fun clear() = Unit
}
