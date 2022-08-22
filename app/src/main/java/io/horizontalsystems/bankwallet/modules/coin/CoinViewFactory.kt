package io.horizontalsystems.bankwallet.modules.coin

import androidx.annotation.DrawableRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.order
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.core.shorten
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewItem
import io.horizontalsystems.bankwallet.modules.coin.overview.CoinOverviewViewItem
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.marketkit.models.*
import java.math.BigDecimal
import java.net.URI

data class ChartInfoData(
    val chartData: ChartData,
    val chartInterval: HsTimePeriod,
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
    @DrawableRes val logoResId: Int,
    val explorerUrl: String?
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
    val share: BigDecimal,
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
            categories = overview.categories.map { it.name },
            contracts = getContractInfo(overview.fullCoin.tokens),
            links = getLinks(overview, item.guideUrl),
            about = overview.description,
            marketData = getMarketItems(item)
        )
    }

    fun getCoinMajorHolders(topTokenHolders: List<TokenHolder>): List<MajorHolderItem> {
        val list = mutableListOf<MajorHolderItem>()
        if (topTokenHolders.isEmpty()) {
            return list
        }

        topTokenHolders
            .sortedByDescending { it.share }
            .forEachIndexed { index, holder ->
                val shareFormatted = numberFormatter.format(holder.share, 0, 2, suffix = "%")
                list.add(
                    MajorHolderItem(
                        index + 1,
                        holder.address,
                        holder.share,
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

        overview.genesisDate?.let {
            val genesisDateString = DateHelper.formatDate(it, "MMM d, yyyy")
            items.add(CoinDataItem(Translator.getString(R.string.CoinPage_LaunchDate),
                genesisDateString))
        }

        return items
    }

    private fun getContractInfo(tokens: List<Token>) = tokens
        .sortedBy { it.blockchainType.order }
        .mapNotNull { token ->
            when (val tokenType = token.type) {
                is TokenType.Eip20 -> when (token.blockchainType) {
                    is BlockchainType.Ethereum -> ContractInfo(tokenType.address, R.drawable.logo_ethereum_24, explorerUrl(token, tokenType.address))
                    is BlockchainType.BinanceSmartChain -> ContractInfo(tokenType.address, R.drawable.logo_binance_smart_chain_24, explorerUrl(token, tokenType.address))
                    is BlockchainType.Polygon -> ContractInfo(tokenType.address, R.drawable.logo_polygon_24, explorerUrl(token, tokenType.address))
                    is BlockchainType.Avalanche -> ContractInfo(tokenType.address, R.drawable.logo_avalanche_24, explorerUrl(token, tokenType.address))
                    is BlockchainType.Optimism -> ContractInfo(tokenType.address, R.drawable.logo_optimism_24, explorerUrl(token, tokenType.address))
                    is BlockchainType.ArbitrumOne -> ContractInfo(tokenType.address, R.drawable.logo_arbitrum_24, explorerUrl(token, tokenType.address))
                    else -> null
                }
                is TokenType.Bep2 -> ContractInfo(tokenType.symbol, R.drawable.logo_binancecoin_24,explorerUrl(token, tokenType.symbol))
                else -> null
            }
    }

    private fun explorerUrl(token: Token, reference: String) : String? {
        return token.blockchain.explorerUrl?.let{
            it.replace("\$ref", reference)
        }
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
