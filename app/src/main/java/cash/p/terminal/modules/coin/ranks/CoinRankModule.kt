package cash.p.terminal.modules.coin.ranks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.modules.coin.analytics.CoinAnalyticsModule
import cash.p.terminal.modules.market.MarketModule
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.ui.compose.Select
import cash.p.terminal.wallet.models.RankMultiValue
import cash.p.terminal.wallet.models.RankValue

object CoinRankModule {
    class Factory(private val rankType: CoinAnalyticsModule.RankType) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CoinRankViewModel(rankType, App.currencyManager.baseCurrency, App.marketKit, App.numberFormatter) as T
        }
    }

    sealed class RankAnyValue {
        class SingleValue(val rankValue: RankValue) : RankAnyValue()
        class MultiValue(val rankMultiValue: RankMultiValue) : RankAnyValue()
    }

    data class RankViewItem(
        val coinUid: String,
        val rank: String,
        val title: String,
        val subTitle: String,
        val iconUrl: String?,
        val value: String?,
    )

    data class UiState(
        val viewState: ViewState,
        val rankViewItems: List<RankViewItem>,
        val periodSelect: Select<TimeDuration>?,
        val sortDescending: Boolean,
        val header: MarketModule.Header
    )
}
