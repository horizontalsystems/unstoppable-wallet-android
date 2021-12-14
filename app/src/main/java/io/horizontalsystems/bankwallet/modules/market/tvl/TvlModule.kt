package io.horizontalsystems.bankwallet.modules.market.tvl

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.chart.XxxChartService
import io.horizontalsystems.bankwallet.modules.chart.XxxChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
import io.horizontalsystems.bankwallet.modules.coin.overview.ui.ChartInfoHeaderItem
import io.horizontalsystems.bankwallet.modules.metricchart.MetricChartFactory
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

object TvlModule {

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {
        private val globalMarketRepository: GlobalMarketRepository by lazy {
            GlobalMarketRepository(App.marketKit)
        }

        private val chartServiceRepo by lazy {
            TvlChartServiceRepo(globalMarketRepository)
        }

        private val xxxChartService by lazy {
            XxxChartService(App.currencyManager, chartServiceRepo)
        }

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return when (modelClass) {
                TvlViewModel::class.java -> {
                    val service = TvlService(App.currencyManager, globalMarketRepository, chartServiceRepo)
                    val tvlViewItemFactory = TvlViewItemFactory()
                    // todo: how to not depend on xxxChartService?
                    TvlViewModel(service, xxxChartService, tvlViewItemFactory) as T
                }
                XxxChartViewModel::class.java -> {
                    val factory = MetricChartFactory(App.numberFormatter)
                    XxxChartViewModel(xxxChartService, factory) as T
                }

                else -> throw IllegalArgumentException()
            }
        }
    }

    data class MarketTvlItem(
        val fullCoin: FullCoin?,
        val name: String,
        val chains: List<String>,
        val iconUrl: String,
        val tvl: CurrencyValue,
        val diff: CurrencyValue?,
        val diffPercent: BigDecimal?,
        val rank: String
    )

    @Immutable
    data class ChartData(
        val subtitle: ChartInfoHeaderItem,
        val currency: Currency,
        val chartInfoData: ChartInfoData
    )

    @Immutable
    data class TvlData(
        val chainSelect: Select<Chain>,
        val sortDescending: Boolean,
        val coinTvlViewItems: List<CoinTvlViewItem>
    )

    @Immutable
    data class CoinTvlViewItem(
        val coinUid: String?,
        val name: String,
        val chain: TranslatableString,
        val iconUrl: String,
        @DrawableRes
        val iconPlaceholder: Int?,
        val tvl: CurrencyValue,
        val tvlChangePercent: BigDecimal?,
        val tvlChangeAmount: CurrencyValue?,
        val rank: String
    )

    enum class Chain : WithTranslatableTitle {
        All, Ethereum, Solana, Binance, Avalanche, Terra, Fantom, Arbitrum, Polygon;

        override val title: TranslatableString
            get() = when (this) {
                All -> TranslatableString.ResString(R.string.MarketGlobalMetrics_ChainSelectorAll)
                else -> TranslatableString.PlainString(name)
            }
    }

    enum class TvlDiffType {
        Percent, Currency
    }

    sealed class SelectorDialogState {
        object Closed : SelectorDialogState()
        class Opened(val select: Select<Chain>) : SelectorDialogState()
    }

}
