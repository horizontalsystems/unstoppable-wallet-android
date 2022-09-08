package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.bankwallet.core.adapters.nft.INftProvider
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.nft.NftAddressMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftAssetShortMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftCollectionShortMetadata
import io.horizontalsystems.bankwallet.entities.nft.NftUid
import io.horizontalsystems.marketkit.models.BlockchainType

class OpenSeaNftProvider(
    private val marketKitWrapper: MarketKitWrapper
) : INftProvider {

    override suspend fun addressMetadata(blockchainType: BlockchainType, address: String): NftAddressMetadata {
        val assetCollection = marketKitWrapper.nftAssetCollection(address)
        val collections = assetCollection.collections.map {
            NftCollectionShortMetadata(
                providerUid = it.uid,
                name = it.name,
                thumbnailImageUrl = it.imageUrl,
                averagePrice7d = it.stats.averagePrice7d,
                averagePrice30 = it.stats.averagePrice30d
            )
        }
        val assets = assetCollection.assets.map {
            NftAssetShortMetadata(
                nftUid = NftUid.Evm(blockchainType, it.contract.address, it.tokenId),
                providerCollectionUid = it.collectionUid,
                name = it.name,
                previewImageUrl = it.imagePreviewUrl,
                onSale = it.onSale,
                lastSalePrice = it.lastSalePrice
            )
        }

        return NftAddressMetadata(collections, assets)
    }

}