package com.quantum.wallet.bankwallet.modules.market.tvl

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.R
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.CurrencyValue
import com.quantum.wallet.bankwallet.modules.chart.ChartCurrencyValueFormatterShortened
import com.quantum.wallet.bankwallet.ui.compose.Select
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

object TvlModule {

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.Factory {
        private val globalMarketRepository: GlobalMarketRepository by lazy {
            GlobalMarketRepository(App.marketKit)
        }

        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return when (modelClass) {
                TvlViewModel::class.java -> {
                    val service = TvlService(App.currencyManager, globalMarketRepository)
                    val tvlViewItemFactory = TvlViewItemFactory()
                    TvlViewModel(service, tvlViewItemFactory) as T
                }
                TvlChartViewModel::class.java -> {
                    val chartService = TvlChartService(App.currencyManager, globalMarketRepository)
                    val chartNumberFormatter = ChartCurrencyValueFormatterShortened()
                    TvlChartViewModel(chartService, chartNumberFormatter) as T
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
