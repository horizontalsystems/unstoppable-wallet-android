package com.quantum.wallet.bankwallet.modules.coin.majorholders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quantum.wallet.bankwallet.core.App
import com.quantum.wallet.bankwallet.entities.ViewState
import com.quantum.wallet.bankwallet.modules.coin.CoinViewFactory
import com.quantum.wallet.bankwallet.modules.coin.MajorHolderItem
import com.quantum.wallet.bankwallet.ui.compose.TranslatableString
import com.quantum.wallet.bankwallet.ui.compose.components.StackBarSlice
import io.horizontalsystems.marketkit.models.Blockchain

object CoinMajorHoldersModule {
    class Factory(private val coinUid: String, private val blockchain: Blockchain) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val factory = CoinViewFactory(
                App.currencyManager.baseCurrency,
                App.numberFormatter,
                App.roiManager
            )
            return CoinMajorHoldersViewModel(coinUid, blockchain, App.marketKit, factory) as T
        }
    }

    data class UiState(
        val viewState: ViewState,
        val top10Share: String,
        val totalHoldersCount: String,
        val seeAllUrl: String?,
        val chartData: List<StackBarSlice>,
        val topHolders: List<MajorHolderItem>,
        val error: TranslatableString?,
    )
}
