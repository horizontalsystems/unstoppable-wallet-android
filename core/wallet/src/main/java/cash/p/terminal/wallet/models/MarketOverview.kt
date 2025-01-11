package cash.p.terminal.wallet.models

import io.horizontalsystems.core.models.HsTimePeriod

data class MarketOverview(
    val globalMarketPoints: List<GlobalMarketPoint>,
    val coinCategories: List<CoinCategory>,
    val topPlatforms: List<TopPlatform>,
    val nftCollections: Map<HsTimePeriod, List<NftTopCollection>>,
    val topPairs: List<TopPair>
)
