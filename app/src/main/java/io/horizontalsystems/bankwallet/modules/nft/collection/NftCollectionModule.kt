package io.horizontalsystems.bankwallet.modules.nft.collection

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.R

object NftCollectionModule {

    class Factory(private val collectionUid: String) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NftCollectionViewModel() as T
        }

    }

    enum class Tab(@StringRes val titleResId: Int) {
        Overview(R.string.NftCollection_Tab_Overview),
        Items(R.string.NftCollection_Tab_Items),
        Activity(R.string.NftCollection_Tab_Activity)
    }

}
