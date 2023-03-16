package cash.p.terminal.modules.coin.majorholders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cash.p.terminal.core.App
import cash.p.terminal.entities.ViewState
import cash.p.terminal.modules.coin.CoinViewFactory
import cash.p.terminal.modules.coin.MajorHolderItem
import cash.p.terminal.ui.compose.TranslatableString
import cash.p.terminal.ui.compose.components.StackBarSlice

object CoinMajorHoldersModule {
    class Factory(private val coinUid: String, private val blockchainUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val factory = CoinViewFactory(App.currencyManager.baseCurrency, App.numberFormatter)
            return CoinMajorHoldersViewModel(coinUid, blockchainUid, App.marketKit, factory) as T
        }
    }

    data class UiState(
        val viewState: ViewState,
        val top10Share: String = "",
        val totalHoldersCount: String = "",
        val seeAllUrl: String? = null,
        val chartData: List<StackBarSlice> = emptyList(),
        val topHolders: List<MajorHolderItem> = emptyList(),
        val error: TranslatableString? = null,
    )
}
