package io.horizontalsystems.bankwallet.modules.nft.collection.assets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.balance.BalanceXRateRepository

object NftCollectionAssetsModule {

    class Factory(private val collectionUid: String) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = NftCollectionAssetsService(
                collectionUid,
                App.marketKit,
                BalanceXRateRepository(App.currencyManager, App.marketKit)
            )
            return NftCollectionAssetsViewModel(service) as T
        }
    }

}
