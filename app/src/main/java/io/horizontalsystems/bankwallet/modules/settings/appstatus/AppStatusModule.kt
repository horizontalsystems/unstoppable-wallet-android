package io.horizontalsystems.bankwallet.modules.settings.appstatus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App

object AppStatusModule {

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val viewModel = AppStatusViewModel(
                App.systemInfoManager,
                App.localStorage,
                App.accountManager,
                App.walletManager,
                App.adapterManager,
                App.marketKit,
                App.evmBlockchainManager,
                App.binanceKitManager,
                App.tronKitManager,
                App.tonKitManager,
                App.solanaKitManager,
                App.btcBlockchainManager,
            )
            return viewModel as T
        }
    }

    sealed class BlockContent {
        data class Header(val title: String) : BlockContent()
        data class Text(val text: String) : BlockContent()
        data class TitleValue(val title: String, val value: String) : BlockContent()
    }

    data class BlockData(val title: String?, val content: List<BlockContent>)

    data class UiState(
        val appStatusAsText: String?,
        val blockViewItems: List<BlockData>,
    )

}
