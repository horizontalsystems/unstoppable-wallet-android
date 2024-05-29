package io.horizontalsystems.bankwallet.modules.market.etf

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.EtfPoint

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

