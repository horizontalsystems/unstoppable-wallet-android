package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.DrawableRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewItem
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewViewItem
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.LinkType
import io.horizontalsystems.marketkit.models.MarketInfoOverview
import io.horizontalsystems.marketkit.models.TokenHolders
import java.math.BigDecimal
import java.net.URI

data class ChartInfoData(
    val chartData: ChartData,
    val maxValue: String?,
    val minValue: String?
)

data class MarketTickerViewItem(
    val market: String,
    val marketImageUrl: String?,
    val pair: String,
    val rate: String,
    val volume: String,
    val tradeUrl: String?,
    val badge: TranslatableString?
)

sealed class RoiViewItem {
    class HeaderRowViewItem(
        val title: String,
        val periods: List<HsTimePeriod>,
    ) : RoiViewItem()

    class RowViewItem(
        val title: String,
        val values: List<BigDecimal?>,
    ) : RoiViewItem()
}
open class ContractInfo(
    val rawValue: String,
    val imgUrl: String,
    val explorerUrl: String?,
    val name: String? = null,
    val schema: String? = null
) {
    val shortened = rawValue.shorten()
}

data class CoinDataItem(
    val title: String,
    val value: String? = null,
    val valueLabeled: String? = null,
    val valueLabeledBackground: Int? = null,
    val valueDecorated: Boolean = false,
    @DrawableRes val icon: Int? = null,
    val rankLabel: String? = null
)

class MajorHolderItem(
    val index: Int,
    val address: String,
    val balance: String,
    val sharePercent: String
)

data class CoinLink(
    val url: String,
    val linkType: LinkType,
    val title: String,
    val icon: Int,
)

class CoinViewFactory(
    private val currency: Currency,
    private val numberFormatter: IAppNumberFormatter
) {

    fun getRoi(performance: Map<String, Map<HsTimePeriod, BigDecimal>>): List<RoiViewItem> {
        val rows = mutableListOf<RoiViewItem>()

        val timePeriods = performance.map { it.value.keys }.flatten().distinct()
        rows.add(RoiViewItem.HeaderRowViewItem("ROI", timePeriods))
        performance.forEach { (vsCurrency, performanceVsCurrency) ->
            if (performanceVsCurrency.isNotEmpty()) {
                val values = timePeriods.map { performanceVsCurrency[it] }
                rows.add(RoiViewItem.RowViewItem("vs ${vsCurrency.uppercase()}", values))
            }
        }

        return rows
    }

    fun getOverviewViewItem(item: CoinOverviewItem): CoinOverviewViewItem {
        val overview = item.marketInfoOverview

        return CoinOverviewViewItem(
            roi = getRoi(overview.performance),
            links = getLinks(overview, item.guideUrl),
            about = overview.description,
            marketData = getMarketItems(item),
            marketCapRank = item.marketInfoOverview.marketCapRank
        )
    }

    fun getTop10Share(number: BigDecimal) : String = numberFormatter.format(number, 0, 2, suffix = "%",)

    fun getHoldersCount(number: BigDecimal) : String = numberFormatter.formatNumberShort(number, 2)

    fun getCoinMajorHolders(tokenHolders: TokenHolders): List<MajorHolderItem> {
        val list = mutableListOf<MajorHolderItem>()
        if (tokenHolders.topHolders.isEmpty()) {
            return list
        }

        tokenHolders.topHolders
            .sortedByDescending { it.percentage }
            .forEachIndexed { index, holder ->
                val shareFormatted = numberFormatter.format(holder.percentage, 0, 4, suffix = "%")
                val balanceFormatted = numberFormatter.formatNumberShort(holder.balance, 2)
                list.add(
                    MajorHolderItem(
                        index + 1,
                        holder.address,
                        balanceFormatted,
                        shareFormatted
                    )
                )
            }

        return list
    }

    private fun getMarketItems(
        item: CoinOverviewItem,
    ): MutableList<CoinDataItem> {
        val overview = item.marketInfoOverview
        val items = mutableListOf<CoinDataItem>()
        overview.marketCap?.let {
            val marketCapString = formatFiatShortened(it, currency.symbol)
            val marketCapRankString = overview.marketCapRank?.let { "#$it" }
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_MarketCap),
                marketCapString,
                rankLabel = marketCapRankString))
        }
        overview.totalSupply?.let {
            val totalSupplyString = numberFormatter.formatCoinShort(it,
                item.coinCode,
                8)
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_TotalSupply),
                totalSupplyString))
        }
        overview.circulatingSupply?.let {
            val supplyString = numberFormatter.formatCoinShort(it,
                item.coinCode,
                8)
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_inCirculation),
                supplyString))
        }

        overview.volume24h?.let {
            val volumeString = formatFiatShortened(it, currency.symbol)
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_TradingVolume),
                volumeString))
        }

        overview.tvl?.let {
            val tvlString = formatFiatShortened(it, currency.symbol)
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_Tvl), tvlString))
        }

        overview.dilutedMarketCap?.let {
            val dilutedMarketCapString = formatFiatShortened(it, currency.symbol)
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_DilutedMarketCap),
                dilutedMarketCapString))
        }

        overview.genesisDate?.let {
            val genesisDateString = DateHelper.formatDate(it, "MMM d, yyyy")
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_LaunchDate),
                genesisDateString))
        }

        return items
    }

    fun getLinks(coinMarketDetails: MarketInfoOverview, guideUrl: String?): List<CoinLink> {
        val linkTypes = listOf(
            LinkType.Guide,
            LinkType.Website,
            LinkType.Whitepaper,
            LinkType.Reddit,
            LinkType.Twitter,
            LinkType.Telegram,
            LinkType.Github,
        )

        val links = linkTypes.mapNotNull { linkType ->
            if (linkType == LinkType.Guide) {
                guideUrl?.let {
                    CoinLink(guideUrl, linkType, getTitle(linkType, guideUrl), getIcon(linkType))
                }
            } else {
                coinMarketDetails.links[linkType]?.let { link ->
                    val trimmed = link.trim()
                    if (trimmed.isNotBlank()) {
                        CoinLink(trimmed, linkType, getTitle(linkType, trimmed), getIcon(linkType))
                    } else {
                        null
                    }
                }
            }
        }

        return links
    }

    private fun getIcon(linkType: LinkType): Int {
        return when (linkType) {
            LinkType.Guide -> R.drawable.ic_academy_20
            LinkType.Website -> R.drawable.ic_globe_20
            LinkType.Whitepaper -> R.drawable.ic_clipboard_20
            LinkType.Twitter -> R.drawable.ic_twitter_20
            LinkType.Telegram -> R.drawable.ic_telegram_20
            LinkType.Reddit -> R.drawable.ic_reddit_20
            LinkType.Github -> R.drawable.ic_github_20
        }
    }

    private fun getTitle(linkType: LinkType, link: String? = null): String {
        return when (linkType) {
            LinkType.Guide -> Translator.getString(R.string.CoinPage_Guide)
            LinkType.Website -> {
                link?.let { URI(it).host.replaceFirst("www.", "") }
                    ?: Translator.getString(R.string.CoinPage_Website)
            }
            LinkType.Whitepaper -> Translator.getString(R.string.CoinPage_Whitepaper)
            LinkType.Twitter -> {
                link?.split("/")?.lastOrNull()?.replaceFirst("@", "")?.let { "@$it" }
                    ?: Translator.getString(R.string.CoinPage_Twitter)
            }
            LinkType.Telegram -> Translator.getString(R.string.CoinPage_Telegram)
            LinkType.Reddit -> Translator.getString(R.string.CoinPage_Reddit)
            LinkType.Github -> Translator.getString(R.string.CoinPage_Github)
//            LinkType.YOUTUBE -> Translator.getString(R.string.CoinPage_Youtube)
        }
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        return numberFormatter.formatFiatShort(value, symbol, 2)
    }

}
