package io.horizontalsystems.bankwallet.modules.nft.asset

import io.horizontalsystems.bankwallet.modules.nft.NftManager
import io.horizontalsystems.bankwallet.modules.nft.collection.NftAssetItem
import io.horizontalsystems.bankwallet.modules.nft.collection.NftItemFactory

class NftAssetService(
    private val accountId: String,
    private val tokenId: String,
    private val nftManager: NftManager,
    private val nftItemFactory: NftItemFactory
) {
    suspend fun fetchAsset(): NftAssetItem? {
        val nftAssetItem = nftManager.getAsset(accountId, tokenId)?.let { asset ->
            nftManager.getCollection(accountId, asset.collectionUid)?.let { collection ->
                nftItemFactory.createNftAssetItem(asset, collection.stats)
            }
        }

        return nftAssetItem
    }
}
