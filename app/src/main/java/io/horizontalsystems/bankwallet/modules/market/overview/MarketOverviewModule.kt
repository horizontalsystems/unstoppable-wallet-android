package io.horizontalsystems.bankwallet.modules.market.overview

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.market.MarketModule
import io.horizontalsystems.bankwallet.modules.market.MarketViewItem
import io.horizontalsystems.bankwallet.ui.extensions.MarketListHeaderView

object MarketOverviewModule {

    class Factory : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            val service = MarketOverviewService(App.marketKit, App.backgroundManager, App.currencyManager)
            return MarketOverviewViewModel(service, listOf(service)) as T
        }

    }

    sealed class State {
        object Loading : State()
        object Error : State()
        data class Data(val boards: List<BoardItem>) : State()
    }

    @Immutable
    data class BoardItem(
        val boardHeader: BoardHeader,
        val boardContent: BoardContent,
        val type: MarketModule.ListType
    )
    data class BoardHeader(val title: Int, val iconRes: Int, val toggleButton: MarketListHeaderView.ToggleButton)
    data class BoardContent(val marketViewItems: List<MarketViewItem>, val showAllClick: MarketModule.ListType)

}
