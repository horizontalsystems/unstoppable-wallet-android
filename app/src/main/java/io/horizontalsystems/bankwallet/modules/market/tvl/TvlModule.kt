package io.horizontalsystems.bankwallet.modules.market.tvl

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CurrencyValue
import io.horizontalsystems.bankwallet.modules.coin.ChartInfoData
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
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val globalMarketRepository = GlobalMarketRepository(App.marketKit)
            val service = TvlService(App.currencyManager, globalMarketRepository)
            val factory = MetricChartFactory(App.numberFormatter)
            return TvlViewModel(service, factory, App.numberFormatter) as T
        }
    }

    data class CoinTvlItem(
        val fullCoin: FullCoin,
        val tvl: CurrencyValue,
        val diff: CurrencyValue,
        val diffPercent: BigDecimal,
        val rank: String
    )

    @Immutable
    data class ChartData(
        val subtitle: SubtitleViewItem,
        val currency: Currency,
        val chartInfoData: ChartInfoData
    )

    @Immutable
    data class TvlData(
        val menu: Menu,
        val coinTvlViewItems: List<CoinTvlViewItem>
    )

    @Immutable
    data class SubtitleViewItem(
        val value: String?,
        val diff: Diff?
    )

    @Immutable
    data class Menu(
        val chainSelect: Select<Chain>,
        val sortDescending: Boolean,
        val tvlDiffType: TvlDiffType
    )

    @Immutable
    data class CoinTvlViewItem(
        val fullCoin: FullCoin,
        val tvl: String,
        val tvlDiff: Diff,
        val rank: String
    )

    sealed class Diff(val value: String) {
        class NoDiff(value: String) : Diff(value)
        class Positive(value: String) : Diff(value)
        class Negative(value: String) : Diff(value)
    }

    enum class Chain : WithTranslatableTitle {
        All, Ethereum, Binance, Solana, Avalanche, Polygon;

        override val title: TranslatableString
            get() = TranslatableString.PlainString(name)
    }

    enum class TvlDiffType {
        Percent, Currency
    }

    sealed class SelectorDialogState {
        object Closed : SelectorDialogState()
        class Opened(val select: Select<Chain>) : SelectorDialogState()
    }

}
