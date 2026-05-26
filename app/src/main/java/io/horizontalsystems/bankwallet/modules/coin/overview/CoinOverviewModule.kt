package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterSignificant
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.CoinDataItem
import io.horizontalsystems.bankwallet.modules.coin.CoinLink
import io.horizontalsystems.bankwallet.modules.coin.RoiViewItem
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketInfoOverview
import io.horizontalsystems.marketkit.models.Token

object CoinOverviewModule {

    class ChartFactory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val chartService = CoinOverviewChartService(
                App.marketKit,
                App.currencyManager,
                fullCoin.coin.uid,
                App.chartIndicatorManager
            )
            val chartNumberFormatter = ChartCurrencyValueFormatterSignificant()
            return ChartModule.createViewModel(chartService, chartNumberFormatter) as T
        }
    }
}

data class CoinOverviewItem(
    val coinCode: String,
    val marketInfoOverview: MarketInfoOverview,
    val guideUrl: String?,
)

data class TokenVariant(
    val value: String,
    val copyValue: String?,
    val imgUrl: String,
    val explorerUrl: String?,
    val name: String?,
    val token: Token,
    val canAddToWallet: Boolean,
    val inWallet: Boolean,
) {
}

data class HudMessage(
    val text: Int,
    val type: HudMessageType,
    val iconRes: Int? = null
)

enum class HudMessageType{
    Success, Error
}

data class CoinOverviewViewItem(
    val roi: List<RoiViewItem>,
    val links: List<CoinLink>,
    val about: String,
    val marketData: MutableList<CoinDataItem>,
    val marketCapRank: Int?
)