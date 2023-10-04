package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartCurrencyValueFormatterSignificant
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.*
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketInfoOverview
import io.horizontalsystems.marketkit.models.Token

object CoinOverviewModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return when (modelClass) {
                CoinOverviewViewModel::class.java -> {
                    val currency = App.currencyManager.baseCurrency
                    val service = CoinOverviewService(
                        fullCoin,
                        App.marketKit,
                        App.currencyManager,
                        App.appConfigProvider,
                        App.languageManager
                    )

                    CoinOverviewViewModel(
                        service,
                        CoinViewFactory(currency, App.numberFormatter),
                        App.walletManager,
                        App.accountManager,
                        App.chartIndicatorManager
                    ) as T
                }
                ChartViewModel::class.java -> {
                    val chartService = CoinOverviewChartService(
                        App.marketKit,
                        App.currencyManager,
                        fullCoin.coin.uid,
                        App.chartIndicatorManager
                    )
                    val chartNumberFormatter = ChartCurrencyValueFormatterSignificant()
                    ChartModule.createViewModel(chartService, chartNumberFormatter) as T
                }
                else -> throw IllegalArgumentException()
            }
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
    val marketData: MutableList<CoinDataItem>
)