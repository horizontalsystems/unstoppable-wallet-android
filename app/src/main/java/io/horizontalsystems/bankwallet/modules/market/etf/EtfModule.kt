package io.horizontalsystems.bankwallet.modules.market.etf

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.stats.StatPeriod
import io.horizontalsystems.bankwallet.entities.Currency
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.bankwallet.uiv3.components.tabs.TabItem
import io.horizontalsystems.marketkit.models.Etf
import io.horizontalsystems.marketkit.models.EtfPoint
import io.horizontalsystems.marketkit.models.HsTimePeriod

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
        val sortBy: SortBy,
        val chartDataLoading: Boolean,
        val etfPoints: List<EtfPoint>,
        val currency: Currency,
        val chartTabs: List<TabItem<HsTimePeriod?>>,
        val selectedChartInterval: HsTimePeriod?,
        val listTimePeriod: EtfListTimePeriod
    )

    enum class SortBy(@StringRes val titleResId: Int) : WithTranslatableTitle {
        HighestAssets(R.string.MarketEtf_HighestAssets),
        LowestAssets(R.string.MarketEtf_LowestAssets),
        Inflow(R.string.MarketEtf_Inflow),
        Outflow(R.string.MarketEtf_Outflow);

        override val title = TranslatableString.ResString(titleResId)
    }

    enum class EtfTab(
        @StringRes val titleResId: Int,
        val key: String,
        val headerImage: String,
        val chainName: String
    ) {
        BtcTab(
            R.string.MarketEtf_BitcoinEtf,
            "btc",
            "https://cdn.blocksdecoded.com/header-images/ETF_bitcoin@3x.png",
            "Bitcoin"
        ),
        EthTab(
            R.string.MarketEtf_EthereumEtf,
            "eth",
            "https://cdn.blocksdecoded.com/header-images/ETF_ethereum@3x.png",
            "Ethereum"
        );
    }
}

enum class EtfListTimePeriod(val titleResId: Int) : WithTranslatableTitle {
    OneDay(R.string.Market_Filter_TimePeriod_1D),
    SevenDay(R.string.Market_Filter_TimePeriod_1W),
    ThirtyDay(R.string.Market_Filter_TimePeriod_1M),
    ThreeMonths(R.string.Market_Filter_TimePeriod_3M),
    All(R.string.Market_All);

    override val title = TranslatableString.ResString(titleResId)
}

val EtfListTimePeriod.statPeriod: StatPeriod
    get() = when(this) {
        EtfListTimePeriod.OneDay -> StatPeriod.Day1
        EtfListTimePeriod.SevenDay -> StatPeriod.Week1
        EtfListTimePeriod.ThirtyDay -> StatPeriod.Month1
        EtfListTimePeriod.ThreeMonths -> StatPeriod.Month3
        EtfListTimePeriod.All -> StatPeriod.All
    }
