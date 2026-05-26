package io.horizontalsystems.bankwallet.modules.coin.ranks

import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.TimeDuration
import io.horizontalsystems.bankwallet.ui.compose.Select
import io.horizontalsystems.marketkit.models.RankMultiValue
import io.horizontalsystems.marketkit.models.RankValue

object CoinRankModule {

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
