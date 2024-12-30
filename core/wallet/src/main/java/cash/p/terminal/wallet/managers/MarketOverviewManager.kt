package cash.p.terminal.wallet.managers

import io.horizontalsystems.core.models.HsTimePeriod
import cash.p.terminal.wallet.models.MarketOverview
import cash.p.terminal.wallet.models.MarketOverviewResponse
import cash.p.terminal.wallet.providers.HsProvider
import io.reactivex.Single

class MarketOverviewManager(
    private val nftManager: NftManager,
    private val hsProvider: HsProvider
) {

    private fun marketOverview(response: MarketOverviewResponse): MarketOverview =
        MarketOverview(
            globalMarketPoints = response.globalMarketPoints,
            coinCategories = response.coinCategories,
            topPlatforms = response.topPlatforms.map { it.topPlatform },
            nftCollections = mapOf(
                HsTimePeriod.Day1 to nftManager.topCollections(response.nft.one_day),
                HsTimePeriod.Week1 to nftManager.topCollections(response.nft.seven_day),
                HsTimePeriod.Month1 to nftManager.topCollections(response.nft.thirty_day)
            ),
            topPairs = response.pairs
        )

    fun marketOverviewSingle(currencyCode: String): Single<MarketOverview> =
        hsProvider.marketOverviewSingle(currencyCode).map { marketOverview(it) }

}
