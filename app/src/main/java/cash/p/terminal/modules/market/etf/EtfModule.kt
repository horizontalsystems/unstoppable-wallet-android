package cash.p.terminal.modules.market.etf

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.R
import cash.p.terminal.core.App
import io.horizontalsystems.core.entities.Currency
import io.horizontalsystems.core.entities.ViewState
import cash.p.terminal.modules.market.MarketDataValue
import cash.p.terminal.modules.market.TimeDuration
import cash.p.terminal.strings.helpers.TranslatableString
import cash.p.terminal.strings.helpers.WithTranslatableTitle
import cash.p.terminal.wallet.models.Etf
import cash.p.terminal.wallet.models.EtfPoint

object EtfModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return EtfViewModel(App.currencyManager, App.marketKit) as T
        }
    }

    @Immutable
    data class EtfViewItem(
        val title: String,
        val iconUrl: String,
        val subtitle: String,
        val value: String?,
        val subvalue: MarketDataValue?,
        val rank: String?,
    )

    data class RankedEtf(
        val etf: Etf,
        val rank: Int
    )

    @Immutable
    data class UiState(
        val viewItems: List<EtfViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean,
        val timeDuration: TimeDuration,
        val sortBy: SortBy,
        val chartDataLoading: Boolean,
        val etfPoints: List<EtfPoint>,
        val currency: Currency,
    )

    enum class SortBy(@StringRes val titleResId: Int): WithTranslatableTitle {
        HighestAssets(R.string.MarketEtf_HighestAssets),
        LowestAssets(R.string.MarketEtf_LowestAssets),
        Inflow(R.string.MarketEtf_Inflow),
        Outflow(R.string.MarketEtf_Outflow);

        override val title = TranslatableString.ResString(titleResId)
    }
}

