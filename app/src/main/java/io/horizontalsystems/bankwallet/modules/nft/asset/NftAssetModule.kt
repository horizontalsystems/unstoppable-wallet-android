package io.horizontalsystems.bankwallet.modules.nft.asset

import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.nft.NftAssetContract

object NftAssetModule {
    @Suppress("UNCHECKED_CAST")
    class Factory(private val accountId: String, private val tokenId: String) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val service = NftAssetService(accountId, tokenId, App.nftManager)
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

data class NftAssetModuleAssetItem(
    val name: String?,
    val imageUrl: String?,
    val collectionName: String,
    val ownedCount: Int,
    val description: String?,
    val contract: NftAssetContract,
    val tokenId: String,
    val prices: Prices
) {
    data class Prices(
        val average7d: CoinValue?,
        val average30d: CoinValue?,
        val last: CoinValue?,
        val floor: CoinValue?,
    )
}

