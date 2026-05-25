package io.horizontalsystems.bankwallet.modules.market.favorites

import androidx.annotation.StringRes
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketItem
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.ui.compose.TranslatableString
import io.horizontalsystems.bankwallet.ui.compose.WithTranslatableTitle
import io.horizontalsystems.marketkit.models.Analytics

object MarketFavoritesModule {

    data class UiState(
        val viewItems: List<MarketViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean,
        val sortingField: WatchlistSorting,
        val period: TimeDuration,
        val showSignal: Boolean,
    )

}

enum class WatchlistSorting(@StringRes val titleResId: Int): WithTranslatableTitle {
    Manual(R.string.Market_Sorting_Manual),
    HighestCap(R.string.Market_Sorting_HighestCap),
    LowestCap(R.string.Market_Sorting_LowestCap),
    Gainers(R.string.Market_Sorting_Gainers),
    Losers(R.string.Market_Sorting_Losers);

    override val title = TranslatableString.ResString(titleResId)
}

data class MarketItemWrapper(
    val marketItem: MarketItem,
    val favorited: Boolean,
    val signal: Analytics.TechnicalAdvice.Advice? =  null
)