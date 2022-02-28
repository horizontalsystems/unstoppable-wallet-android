package io.horizontalsystems.bankwallet.modules.nft.asset

import io.horizontalsystems.bankwallet.modules.nft.NftManager

class NftAssetService(
    private val accountId: String,
    private val tokenId: String,
    private val contractAddress: String,
    private val nftManager: NftManager
) {
    suspend fun fetchItem(): NftAssetModuleAssetItem {
        val assetRecord = nftManager.getAssetRecord(accountId, tokenId, contractAddress) ?: throw NftNotFoundException()
        val collectionRecord = nftManager.getCollectionRecord(accountId, assetRecord.collectionUid) ?: throw NftNotFoundException()

        return NftAssetModuleAssetItem(
            name = assetRecord.name,
            imageUrl = assetRecord.imageUrl,
            collectionName = collectionRecord.name,
            ownedCount = assetRecord.ownedCount,
            description = assetRecord.description,
            contract = assetRecord.contract,
            tokenId = assetRecord.tokenId,
            assetLinks = assetRecord.links,
            collectionLinks = collectionRecord.links,
            prices = NftAssetModuleAssetItem.Prices(
                average7d = nftManager.nftAssetPriceToCoinValue(collectionRecord.averagePrice7d),
                average30d = nftManager.nftAssetPriceToCoinValue(collectionRecord.averagePrice30d),
                last = nftManager.nftAssetPriceToCoinValue(assetRecord.lastSale),
                floor = nftManager.nftAssetPriceToCoinValue(collectionRecord.floorPrice),
            )
        )
    }
}

class NftNotFoundException: Exception()