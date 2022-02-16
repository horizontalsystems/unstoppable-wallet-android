package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.modules.nft.collection.NftItemFactory

object NftAssetModule {
    @Suppress("UNCHECKED_CAST")
    class Factory(private val accountId: String, private val tokenId: String) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val nftItemFactory = NftItemFactory(App.coinManager)
            val service = NftAssetService(accountId, tokenId, App.nftManager, nftItemFactory)
            return NftAssetViewModel(service) as T
        }

    }

    internal const val accountIdKey = "accountIdKey"
    internal const val tokenIdKey = "tokenIdKey"

    fun prepareParams(accountId: String, tokenId: String) = bundleOf(
        accountIdKey to accountId,
        tokenIdKey to tokenId
    )
}
