package io.horizontalsystems.bankwallet.modules.nft.collection

import io.horizontalsystems.bankwallet.modules.nft.NftAssetRecord
import io.horizontalsystems.bankwallet.modules.nft.NftCollectionRecord
import io.horizontalsystems.bankwallet.modules.nft.NftManager

class NftItemFactory(private val nftManager: NftManager) {

    fun createNftAssetItem(
        assetRecord: NftAssetRecord,
        collection: NftCollectionRecord
    ): NftAssetItem {
        val assetItemPrices = NftAssetItem.Prices(
            average7d = nftManager.nftAssetPriceToCoinValue(collection.averagePrice7d),
            average30d = nftManager.nftAssetPriceToCoinValue(collection.averagePrice30d),
            last = nftManager.nftAssetPriceToCoinValue(assetRecord.lastSale)
        )

        return NftAssetItem(
            accountId = assetRecord.accountId,
            tokenId = assetRecord.tokenId,
            name = assetRecord.name,
            imageUrl = assetRecord.imageUrl,
            imagePreviewUrl = assetRecord.imagePreviewUrl,
            description = assetRecord.description,
            contract = assetRecord.contract,
            onSale = assetRecord.onSale,
            prices = assetItemPrices
        )
    }

}