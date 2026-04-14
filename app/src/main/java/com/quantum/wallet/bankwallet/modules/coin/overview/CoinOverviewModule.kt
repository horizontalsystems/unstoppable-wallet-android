package com.quantum.wallet.bankwallet.modules.coin.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.modules.chart.ChartCurrencyValueFormatterSignificant
import com.quantum.wallet.bankwallet.modules.chart.ChartModule
import com.quantum.wallet.bankwallet.modules.chart.ChartViewModel
import com.quantum.wallet.bankwallet.modules.coin.CoinDataItem
import com.quantum.wallet.bankwallet.modules.coin.CoinLink
import com.quantum.wallet.bankwallet.modules.coin.CoinViewFactory
import com.quantum.wallet.bankwallet.modules.coin.RoiViewItem
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
                        App.languageManager,
                        App.roiManager
                    )

                    CoinOverviewViewModel(
                        service,
                        CoinViewFactory(currency, App.numberFormatter, App.roiManager),
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
    val marketData: MutableList<CoinDataItem>,
    val marketCapRank: Int?
)