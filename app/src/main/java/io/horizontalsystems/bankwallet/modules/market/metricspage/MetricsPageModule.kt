package io.horizontalsystems.bankwallet.modules.market.metricspage

import androidx.compose.runtime.Immutable
import io.horizontalsystems.bankwallet.entities.ViewState
import io.horizontalsystems.bankwallet.modules.market.MarketDataValue
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.marketkit.models.FullCoin
import java.math.BigDecimal

object MetricsPageModule {

    @Immutable
    data class CoinViewItem(
        val fullCoin: FullCoin,
        val subtitle: String,
        val coinRate: String,
        val marketDataValue: MarketDataValue?,
        val rank: String?,
        val sortField: BigDecimal?,
    )

    @Immutable
    data class UiState(
        val header: MarketModule.Header,
        val viewItems: List<CoinViewItem>,
        val viewState: ViewState,
        val isRefreshing: Boolean,
        val toggleButtonTitle: String,
        val sortDescending: Boolean,
    )
}
