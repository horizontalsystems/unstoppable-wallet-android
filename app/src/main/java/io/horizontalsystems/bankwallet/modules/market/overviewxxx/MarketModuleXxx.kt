package cash.p.terminal.modules.market.overviewxxx

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.metricchart.MetricsType

object MarketModuleXxx {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MarketViewModelXxx(
                App.marketStorage,
                App.marketKit,
                App.currencyManager,
                App.localStorage
            ) as T
        }

    }

    data class UiState(
        val selectedTab: MarketModule.Tab,
        val marketOverviewItems: List<MarketOverviewViewItem>
    )

    data class MarketOverviewViewItem(
        val title: String,
        val value: String,
        val change: String,
        val changePositive: Boolean,
        val metricsType: MetricsType,
    )

}
