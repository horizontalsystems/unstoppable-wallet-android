package io.horizontalsystems.bankwallet.modules.market.tvl

import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.iconPlaceholder
import io.horizontalsystems.bankwallet.core.imageUrl
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString

class TvlViewItemFactory {
    private val cache: MutableMap<Int, TvlModule.CoinTvlViewItem> = hashMapOf()

    fun tvlData(
        chain: TvlModule.Chain,
        chains: List<TvlModule.Chain>,
        sortDescending: Boolean,
        tvlItems: List<TvlModule.MarketTvlItem>
    ) = TvlModule.TvlData(
        Select(chain, chains),
        sortDescending,
        tvlItems.mapNotNull {
            coinTvlViewItem(it)
        })

    private fun coinTvlViewItem(item: TvlModule.MarketTvlItem): TvlModule.CoinTvlViewItem? {
        if (!cache.containsKey(item.hashCode())) {
            val viewItem = TvlModule.CoinTvlViewItem(
                item.fullCoin?.coin?.uid,
                tvl = item.tvl,
                tvlChangePercent = item.diffPercent,
                tvlChangeAmount = item.diff,
                rank = item.rank,
                name = item.fullCoin?.coin?.name ?: item.name,
                chain = if (item.chains.size > 1)
                    TranslatableString.ResString(R.string.TvlRank_MultiChain)
                else
                    TranslatableString.PlainString(item.chains.first()),
                iconUrl = item.fullCoin?.coin?.imageUrl ?: item.iconUrl,
                iconPlaceholder = item.fullCoin?.iconPlaceholder
            )

            cache[item.hashCode()] = viewItem
        }
        return cache[item.hashCode()]
    }
}
