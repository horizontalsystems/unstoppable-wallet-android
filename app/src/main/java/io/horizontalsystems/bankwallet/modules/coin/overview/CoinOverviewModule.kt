package io.horizontalsystems.bankwallet.modules.coin.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.chart.ChartModule
import io.horizontalsystems.bankwallet.modules.chart.ChartViewModel
import io.horizontalsystems.bankwallet.modules.coin.*
import io.horizontalsystems.marketkit.models.FullCoin
import io.horizontalsystems.marketkit.models.MarketInfoOverview

object CoinOverviewModule {

    class Factory(private val fullCoin: FullCoin) : ViewModelProvider.Factory {

        private val chartRepo2 by lazy {
            CoinOverviewChartRepo(App.marketKit, App.currencyManager, App.chartTypeStorage, fullCoin.coin.uid)
        }

        private val chartRepo by lazy {
            ChartRepo(App.marketKit, App.currencyManager, App.chartTypeStorage, fullCoin.coin.uid)
        }

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {

            return when (modelClass) {
                CoinOverviewViewModel::class.java -> {
                    val currency = App.currencyManager.baseCurrency


                    val service = CoinOverviewService(
                        fullCoin,
                        App.marketKit,
                        App.currencyManager,
                        App.appConfigProvider,
                        App.languageManager,
                        chartRepo
                    )

                    CoinOverviewViewModel(service, CoinViewFactory(currency, App.numberFormatter)) as T
                }
                ChartViewModel::class.java -> {
                    ChartModule.createViewModel(chartRepo2) as T
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

data class CoinOverviewViewItem(
    val roi: List<RoiViewItem>,
    val categories: List<String>,
    val contracts: List<ContractInfo>,
    val links: List<CoinLink>,
    val about: String,
    val marketData: MutableList<CoinDataItem>
)