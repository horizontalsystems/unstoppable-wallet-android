package io.horizontalsystems.bankwallet.modules.coin

import android.os.Parcelable
import androidx.annotation.DrawableRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.IAppNumberFormatter
import io.horizontalsystems.bankwallet.core.providers.Translator
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.coin.CoinDataClickType.SecurityInfo
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.chartview.ChartData
import io.horizontalsystems.chartview.ChartDataFactory
import io.horizontalsystems.chartview.ChartView
import io.horizontalsystems.chartview.models.ChartPoint
import io.horizontalsystems.chartview.models.MacdInfo
import io.horizontalsystems.coinkit.models.CoinType
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.views.ListPosition
import io.horizontalsystems.xrateskit.entities.*
import kotlinx.android.parcel.Parcelize
import java.lang.Long.max
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.util.*

data class ChartInfoData(
    val chartData: ChartData,
    val chartType: ChartView.ChartType,
    val maxValue: String?,
    val minValue: String?
)

data class ChartPointViewItem(
    val date: Long,
    val price: CurrencyValue,
    val volume: CurrencyValue?,
    val macdInfo: MacdInfo?
)

data class MarketTickerViewItem(
    val title: String,
    val subtitle: String,
    val value: String,
    val subvalue: String,
    val imageUrl: String?,
) {
    fun areItemsTheSame(other: MarketTickerViewItem): Boolean {
        return title == other.title && subtitle == other.subtitle
    }

    fun areContentsTheSame(other: MarketTickerViewItem): Boolean {
        return value == other.value && subvalue == other.subvalue && imageUrl == other.imageUrl
    }
}

sealed class RoiViewItem {
    class HeaderRowViewItem(
        val title: String,
        val periods: List<TimePeriod>,
        var listPosition: ListPosition? = null
    ) : RoiViewItem()

    class RowViewItem(
        val title: String,
        val values: List<BigDecimal?>,
        var listPosition: ListPosition? = null
    ) : RoiViewItem()
}

data class ContractInfo(val title: String, val value: String)

data class CoinDataItem(
    val title: String,
    val value: String? = null,
    val valueLabeled: String? = null,
    val valueLabeledBackground: Int? = null,
    val valueDecorated: Boolean = false,
    @DrawableRes val icon: Int? = null,
    var listPosition: ListPosition? = null,
    val clickType: CoinDataClickType? = null,
    val rankLabel: String? = null
)

sealed class CoinDataClickType: Parcelable {
    @Parcelize
    object MetricChart : CoinDataClickType()
    @Parcelize
    object Markets : CoinDataClickType()
    @Parcelize
    object TvlRank : CoinDataClickType()
    @Parcelize
    object TradingVolumeMetricChart : CoinDataClickType()
    @Parcelize
    object MajorHolders : CoinDataClickType()
    @Parcelize
    object FundsInvested : CoinDataClickType()
    @Parcelize
    object SecurityAudits : CoinDataClickType()

    @Parcelize
    class SecurityInfo(val title: Int, val items: List<Item>) : CoinDataClickType() {
        @Parcelize
        class Item(val title: Int, val color: Int, val info: Int): Parcelable
    }
}

sealed class InvestorItem {
    data class Header(val title: String) : InvestorItem()
    data class Fund(
        val name: String,
        val url: String,
        val cleanedUrl: String,
        val position: ListPosition
    ) : InvestorItem()
}

sealed class MajorHolderItem {
    object Header : MajorHolderItem()
    class Item(val address: String, val share: String, val position: ListPosition) : MajorHolderItem()

    object Description : MajorHolderItem()
}

sealed class CoinAuditItem {
    class Header(val name: String) : CoinAuditItem()
    class Report(
        val name: String,
        val date: Date,
        val issues: Int = 0,
        val link: String,
        val position: ListPosition
    ) : CoinAuditItem()
}

data class AboutText(val value: String, val type: CoinMeta.DescriptionType)

data class CoinLink(
    val url: String,
    val linkType: LinkType,
    val title: String,
    val icon: Int,
    var listPosition: ListPosition? = null
)

data class LastPoint(
    val rate: BigDecimal,
    val timestamp: Long,
    val rateDiff24h: BigDecimal
)

class CoinViewFactory(
    private val currency: Currency,
    private val numberFormatter: IAppNumberFormatter
) {
    fun createChartInfoData(
        type: ChartType,
        chartInfo: ChartInfo,
        lastPoint: LastPoint?
    ): ChartInfoData {
        val chartType = when (type) {
            ChartType.TODAY -> ChartView.ChartType.TODAY
            ChartType.DAILY -> ChartView.ChartType.DAILY
            ChartType.WEEKLY -> ChartView.ChartType.WEEKLY
            ChartType.WEEKLY2 -> ChartView.ChartType.WEEKLY2
            ChartType.MONTHLY -> ChartView.ChartType.MONTHLY
            ChartType.MONTHLY3 -> ChartView.ChartType.MONTHLY3
            ChartType.MONTHLY6 -> ChartView.ChartType.MONTHLY6
            ChartType.MONTHLY12 -> ChartView.ChartType.MONTHLY12
            ChartType.MONTHLY24 -> ChartView.ChartType.MONTHLY24
        }
        val chartData = createChartData(chartInfo, lastPoint, chartType)
        val maxValue = numberFormatter.formatFiat(chartData.valueRange.upper, currency.symbol, 0, 2)
        val minValue = numberFormatter.formatFiat(chartData.valueRange.lower, currency.symbol, 0, 2)

        return ChartInfoData(chartData, chartType, maxValue, minValue)
    }

    fun getRoi(
        rateDiffs: Map<TimePeriod, Map<String, BigDecimal>>,
        roiCoinCodes: List<String>,
        roiPeriods: List<TimePeriod>
    ): List<RoiViewItem> {
        if (rateDiffs.isEmpty()) {
            return listOf()
        }

        val rows = mutableListOf<RoiViewItem>()
        rows.add(RoiViewItem.HeaderRowViewItem("ROI", roiPeriods))

        roiCoinCodes.forEachIndexed { index, coinCode ->
            val values = roiPeriods.map { period ->
                rateDiffs[period]?.get(coinCode)
            }
            rows.add(RoiViewItem.RowViewItem("vs $coinCode", values))
        }

        rows.forEachIndexed { index, item ->
            when (item) {
                is RoiViewItem.RowViewItem -> item.listPosition =
                    ListPosition.Companion.getListPosition(rows.size, index)
                is RoiViewItem.HeaderRowViewItem -> item.listPosition =
                    ListPosition.Companion.getListPosition(rows.size, index)
            }
        }

        return rows
    }

    fun getTvlInfo(coinMarket: CoinMarketDetails, currency: Currency): List<CoinDataItem> {
        val tvlInfoList = mutableListOf<CoinDataItem>()

        coinMarket.defiTvlInfo?.let { defiTvlInfo ->
            tvlInfoList.add(
                CoinDataItem(
                    Translator.getString(R.string.CoinPage_Tvl),
                    formatFiatShortened(defiTvlInfo.tvl, currency.symbol),
                    icon = R.drawable.ic_chart_20,
                    clickType = CoinDataClickType.MetricChart
                )
            )
            tvlInfoList.add(
                CoinDataItem(
                    title = Translator.getString(R.string.CoinPage_TvlRank),
                    value = "#${defiTvlInfo.tvlRank}",
                    icon = R.drawable.ic_arrow_right,
                    clickType = CoinDataClickType.TvlRank
                )
            )
            tvlInfoList.add(
                CoinDataItem(
                    Translator.getString(R.string.CoinPage_TvlMCapRatio),
                    numberFormatter.format(defiTvlInfo.marketCapTvlRatio, 0, 2)
                )
            )
        }

        setListPosition(tvlInfoList)

        return tvlInfoList
    }

    fun getMarketData(
        coinMarket: CoinMarketDetails,
        currency: Currency,
        coinCode: String
    ): MutableList<CoinDataItem> {
        val marketData = mutableListOf<CoinDataItem>()
        if (coinMarket.marketCap > BigDecimal.ZERO) {
            marketData.add(
                CoinDataItem(
                    Translator.getString(R.string.CoinPage_MarketCap),
                    formatFiatShortened(coinMarket.marketCap, currency.symbol),
                    rankLabel = coinMarket.marketCapRank?.let { "#$it" })
            )
        }
        if (coinMarket.circulatingSupply > BigDecimal.ZERO) {
            val (shortenValue, suffix) = numberFormatter.shortenValue(coinMarket.circulatingSupply)
            val value = "$shortenValue $suffix $coinCode"
            marketData.add(
                CoinDataItem(
                    Translator.getString(R.string.CoinPage_inCirculation),
                    value
                )
            )
        }
        if (coinMarket.totalSupply > BigDecimal.ZERO) {
            val (shortenValue, suffix) = numberFormatter.shortenValue(coinMarket.totalSupply)
            val value = "$shortenValue $suffix $coinCode"
            marketData.add(CoinDataItem(Translator.getString(R.string.CoinPage_TotalSupply), value))
        }
        if (coinMarket.marketCap > BigDecimal.ZERO && coinMarket.circulatingSupply > BigDecimal.ZERO && coinMarket.totalSupply > BigDecimal.ZERO) {
            val rate = coinMarket.marketCap.divide(
                coinMarket.circulatingSupply,
                SCALE_UP_TO_BILLIONTH,
                RoundingMode.HALF_EVEN
            )
            val dilutedMarketCap = coinMarket.totalSupply.multiply(rate)
            marketData.add(
                CoinDataItem(
                    Translator.getString(R.string.CoinPage_DilutedMarketCap),
                    formatFiatShortened(dilutedMarketCap, currency.symbol)
                )
            )
        }
        coinMarket.meta.launchDate?.let { date ->
            val formattedDate = DateHelper.formatDate(date, "MMM d, yyyy")
            marketData.add(
                CoinDataItem(
                    Translator.getString(R.string.CoinPage_LaunchDate),
                    formattedDate
                )
            )
        }

        //set List position by total list size
        setListPosition(marketData)

        return marketData
    }

    fun getLinks(coinMarketDetails: CoinMarketDetails, guideUrl: String?): List<CoinLink> {
        val links = mutableListOf<CoinLink>()
        guideUrl?.let {
            links.add(
                CoinLink(
                    it,
                    LinkType.GUIDE,
                    getTitle(LinkType.GUIDE),
                    getIcon(LinkType.GUIDE)
                )
            )
        }

        coinMarketDetails.meta.links.forEach { (linkType, link) ->
            links.add(CoinLink(link, linkType, getTitle(linkType, link), getIcon(linkType)))
        }

        links.forEachIndexed { index, link ->
            link.listPosition = ListPosition.getListPosition(links.size, index)
        }

        return links
    }

    fun getTradingVolume(coinDetails: CoinMarketDetails, currency: Currency): List<CoinDataItem> {
        val items = mutableListOf<CoinDataItem>()

        if (coinDetails.volume24h > BigDecimal.ZERO) {
            val volume = formatFiatShortened(coinDetails.volume24h, currency.symbol)
            items.add(
                CoinDataItem(
                    title = Translator.getString(R.string.CoinPage_TradingVolume),
                    value = volume,
                    icon = R.drawable.ic_chart_20,
                    clickType = CoinDataClickType.TradingVolumeMetricChart
                )
            )
        }

        if (coinDetails.tickers.isNotEmpty()) {
            items.add(
                CoinDataItem(
                    title = Translator.getString(R.string.CoinPage_Markets),
                    icon = R.drawable.ic_arrow_right,
                    clickType = CoinDataClickType.Markets
                )
            )
        }

        setListPosition(items)

        return items
    }

    fun getInvestorData(coinDetails: CoinMarketDetails, topTokenHolders: List<TokenHolder>): List<CoinDataItem> {
        val items = mutableListOf<CoinDataItem>()

        if (topTokenHolders.isNotEmpty()) {
            items.add(
                CoinDataItem(
                    title = Translator.getString(R.string.CoinPage_MajorHolders),
                    icon = R.drawable.ic_arrow_right,
                    clickType = CoinDataClickType.MajorHolders
                )
            )
        }

        if (coinDetails.meta.fundCategories.isNotEmpty()) {
            items.add(
                CoinDataItem(
                    title = Translator.getString(R.string.CoinPage_FundsInvested),
                    icon = R.drawable.ic_arrow_right,
                    clickType = CoinDataClickType.FundsInvested
                )
            )
        }

        setListPosition(items)

        return items
    }

    fun getSecurityParams(security: SecurityParameter): MutableList<CoinDataItem> {
        val bgColorGreen = R.drawable.label_green_background
        val bgColorRed = R.drawable.label_red_background
        val bgColorBlue = R.drawable.label_blue_background

        val items = mutableListOf<CoinDataItem>()

        val (securityText, securityBgColor) = when (security.privacy) {
            Level.LOW -> Pair(R.string.CoinPage_SecurityParams_Low, bgColorRed)
            Level.MEDIUM -> Pair(R.string.CoinPage_SecurityParams_Medium, bgColorBlue)
            Level.HIGH -> Pair(R.string.CoinPage_SecurityParams_High, bgColorGreen)
        }

        items.add(
            CoinDataItem(
                title = Translator.getString(R.string.CoinPage_SecurityParams_Privacy),
                valueLabeled = Translator.getString(securityText),
                valueLabeledBackground = securityBgColor,
                icon = R.drawable.ic_info_20,
                clickType = SecurityInfo(
                    title = R.string.CoinPage_SecurityParams_Privacy,
                    items = listOf(
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_Low, R.color.lucian, R.string.CoinPage_SecurityParams_Privacy_Low),
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_Medium, R.color.issyk_blue, R.string.CoinPage_SecurityParams_Privacy_Medium),
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_High, R.color.remus, R.string.CoinPage_SecurityParams_Privacy_High)

                    )
                )
            )
        )

        val (issuance, issuanceColor) = if (security.decentralized) {
            Pair(R.string.CoinPage_SecurityParams_Decentralized, bgColorGreen)
        } else {
            Pair(R.string.CoinPage_SecurityParams_Centralized, bgColorRed)
        }

        items.add(
            CoinDataItem(
                title = Translator.getString(R.string.CoinPage_SecurityParams_Issuance),
                valueLabeled = Translator.getString(issuance),
                valueLabeledBackground = issuanceColor,
                icon = R.drawable.ic_info_20,
                clickType = SecurityInfo(
                    title = R.string.CoinPage_SecurityParams_Issuance,
                    items = listOf(
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_Decentralized, R.color.remus, R.string.CoinPage_SecurityParams_Issuance_Decentralized),
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_Centralized, R.color.lucian, R.string.CoinPage_SecurityParams_Issuance_Centralized)
                    )
                )
            )
        )

        val (confiscationResistance, confiscationColor) = if (security.confiscationResistance) {
            Pair(R.string.CoinPage_SecurityParams_Yes, bgColorGreen)
        } else {
            Pair(R.string.CoinPage_SecurityParams_No, bgColorRed)
        }

        items.add(
            CoinDataItem(
                title = Translator.getString(R.string.CoinPage_SecurityParams_ConfiscationResistance),
                valueLabeled = Translator.getString(confiscationResistance),
                valueLabeledBackground = confiscationColor,
                icon = R.drawable.ic_info_20,
                clickType = SecurityInfo(
                    title = R.string.CoinPage_SecurityParams_ConfiscationResistance,
                    items = listOf(
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_Yes, R.color.remus, R.string.CoinPage_SecurityParams_ConfiscationResistance_Yes),
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_No, R.color.lucian, R.string.CoinPage_SecurityParams_ConfiscationResistance_No)
                    )
                )
            )
        )

        val (censorshipResistance, censorshipColor) = if (security.censorshipResistance) {
            Pair(R.string.CoinPage_SecurityParams_Yes, bgColorGreen)
        } else {
            Pair(R.string.CoinPage_SecurityParams_No, bgColorRed)
        }

        items.add(
            CoinDataItem(
                title = Translator.getString(R.string.CoinPage_SecurityParams_CensorshipResistance),
                valueLabeled = Translator.getString(censorshipResistance),
                valueLabeledBackground = censorshipColor,
                icon = R.drawable.ic_info_20,
                clickType = SecurityInfo(
                    title = R.string.CoinPage_SecurityParams_CensorshipResistance,
                    items = listOf(
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_Yes, R.color.remus, R.string.CoinPage_SecurityParams_CensorshipResistance_Yes),
                        SecurityInfo.Item(R.string.CoinPage_SecurityParams_No, R.color.lucian, R.string.CoinPage_SecurityParams_CensorshipResistance_No)
                    )
                )
            )
        )

        return items
    }

    fun getCoinAudits(coinAudits: List<Auditor>, coinType: CoinType): CoinDataItem? {
        if (coinAudits.isEmpty()) {
            return null
        }

        if (coinType !is CoinType.Erc20 && coinType !is CoinType.Bep20) {
            return null
        }

        return CoinDataItem(
            title = Translator.getString(R.string.CoinPage_SecurityParams_Audits),
            icon = R.drawable.ic_arrow_right,
            clickType = CoinDataClickType.SecurityAudits
        )
    }

    fun getCoinInvestorItems(fundCategories: List<CoinFundCategory>): List<InvestorItem> {
        val items = mutableListOf<InvestorItem>()
        fundCategories.forEach { category ->
            items.add(InvestorItem.Header(category.name))
            category.funds.forEachIndexed { index, fund ->
                items.add(
                    InvestorItem.Fund(
                        fund.name,
                        fund.url,
                        TextHelper.getCleanedUrl(fund.url),
                        ListPosition.getListPosition(category.funds.size, index)
                    )
                )
            }
        }
        return items
    }

    fun getCoinMajorHolders(topTokenHolders: List<TokenHolder>): List<MajorHolderItem> {
        val list = mutableListOf<MajorHolderItem>()
        if (topTokenHolders.isEmpty()) {
            return list
        }

        list.add(MajorHolderItem.Header)
        topTokenHolders
            .sortedByDescending { it.share }
            .forEachIndexed { index, holder ->
                val shareFormatted = numberFormatter.format(holder.share, 0, 2, suffix = "%")
                list.add(
                    MajorHolderItem.Item(
                        holder.address,
                        shareFormatted,
                        ListPosition.Companion.getListPosition(topTokenHolders.size, index)
                    )
                )
            }
        list.add(MajorHolderItem.Description)

        return list
    }

    fun getCoinAudits(audits: List<Auditor>): List<CoinAuditItem> {
        if (audits.isEmpty()) {
            return emptyList()
        }

        val list = mutableListOf<CoinAuditItem>(    )
        audits.forEach { auditor ->
            list.add(CoinAuditItem.Header(auditor.name))

            auditor.reports.forEachIndexed { index, report ->
                list.add(
                    CoinAuditItem.Report(
                        report.name,
                        Date(report.timestamp * 1000),
                        report.issues,
                        report.link,
                        ListPosition.Companion.getListPosition(auditor.reports.size, index)
                    )
                )
            }
        }

        return list
    }

    fun getFormattedLatestRate(currencyValue: CurrencyValue): String {
        return numberFormatter.formatFiat(currencyValue.value, currencyValue.currency.symbol, 2, 4)
    }

    fun setListPosition(list: MutableList<CoinDataItem>) {
        list.forEachIndexed { index, coinDataItem ->
            coinDataItem.listPosition = ListPosition.getListPosition(list.size, index)
        }
    }

    private fun getIcon(linkType: LinkType): Int {
        return when (linkType) {
            LinkType.GUIDE -> R.drawable.ic_academy_20
            LinkType.WEBSITE -> R.drawable.ic_globe
            LinkType.WHITEPAPER -> R.drawable.ic_clipboard
            LinkType.TWITTER -> R.drawable.ic_twitter
            LinkType.TELEGRAM -> R.drawable.ic_telegram
            LinkType.REDDIT -> R.drawable.ic_reddit
            LinkType.GITHUB -> R.drawable.ic_github
            LinkType.YOUTUBE -> R.drawable.ic_globe
        }
    }

    private fun getTitle(linkType: LinkType, link: String? = null): String {
        return when (linkType) {
            LinkType.GUIDE -> Translator.getString(R.string.CoinPage_Guide)
            LinkType.WEBSITE -> {
                link?.let { URI(it).host.replaceFirst("www.", "") }
                    ?: Translator.getString(R.string.CoinPage_Website)
            }
            LinkType.WHITEPAPER -> Translator.getString(R.string.CoinPage_Whitepaper)
            LinkType.TWITTER -> {
                link?.split("/")?.lastOrNull()?.replaceFirst("@", "")?.let { "@$it" }
                    ?: Translator.getString(R.string.CoinPage_Twitter)
            }
            LinkType.TELEGRAM -> Translator.getString(R.string.CoinPage_Telegram)
            LinkType.REDDIT -> Translator.getString(R.string.CoinPage_Reddit)
            LinkType.GITHUB -> Translator.getString(R.string.CoinPage_Github)
            LinkType.YOUTUBE -> Translator.getString(R.string.CoinPage_Youtube)
        }
    }

    private fun createChartData(
        chartInfo: ChartInfo,
        lastPoint: LastPoint?,
        chartType: ChartView.ChartType
    ): ChartData {
        val points = chartInfo.points.map {
            ChartPoint(
                it.value.toFloat(),
                it.volume?.toFloat(),
                it.timestamp
            )
        }.toMutableList()
        val chartInfoLastPoint = chartInfo.points.lastOrNull()
        var endTimestamp = chartInfo.endTimestamp

        if (lastPoint != null && chartInfoLastPoint?.timestamp != null && lastPoint.timestamp > chartInfoLastPoint.timestamp) {
            endTimestamp = max(lastPoint.timestamp, endTimestamp)
            points.add(ChartPoint(lastPoint.rate.toFloat(), null, lastPoint.timestamp))

            if (chartType == ChartView.ChartType.DAILY) {
                val startTimestamp = lastPoint.timestamp - 24 * 60 * 60
                val startValue =
                    lastPoint.rate.multiply(100.toBigDecimal()) / lastPoint.rateDiff24h.add(100.toBigDecimal())
                val startPoint = ChartPoint(startValue.toFloat(), null, startTimestamp)

                points.removeIf { it.timestamp <= startTimestamp }
                points.add(0, startPoint)
            }
        }

        return ChartDataFactory.build(
            points,
            chartInfo.startTimestamp,
            endTimestamp,
            chartInfo.isExpired
        )
    }

    private fun formatFiatShortened(value: BigDecimal, symbol: String): String {
        val shortCapValue = numberFormatter.shortenValue(value)
        return numberFormatter.formatFiat(
            shortCapValue.first,
            symbol,
            0,
            2
        ) + " " + shortCapValue.second
    }

    companion object {
        const val SCALE_UP_TO_BILLIONTH = 9
    }

}
