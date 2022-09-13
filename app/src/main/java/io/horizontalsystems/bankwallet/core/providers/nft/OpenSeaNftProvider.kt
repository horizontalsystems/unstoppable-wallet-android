package io.horizontalsystems.bankwallet.core.providers.nft

import io.horizontalsystems.bankwallet.core.adapters.nft.INftProvider
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.nft.*
import io.horizontalsystems.marketkit.models.BlockchainType
import io.horizontalsystems.marketkit.models.HsTimePeriod
import io.horizontalsystems.marketkit.models.NftAsset
import io.horizontalsystems.marketkit.models.NftCollection

class OpenSeaNftProvider(
    private val marketKitWrapper: MarketKitWrapper
) : INftProvider {

    override val title = "OpenSea"

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

    override suspend fun extendedAssetMetadata(nftUid: NftUid, providerCollectionUid: String): Pair<NftAssetMetadata, NftCollectionMetadata> {
        val asset = marketKitWrapper.nftAsset(nftUid.contractAddress, nftUid.tokenId)
        val collection = marketKitWrapper.nftCollection(providerCollectionUid)

        return Pair(assetMetadata(asset, nftUid.blockchainType, providerCollectionUid), collectionMetadata(collection, nftUid.blockchainType))
    }

    private fun assetMetadata(asset: NftAsset, blockchainType: BlockchainType, providerCollectionUid: String): NftAssetMetadata {
        return NftAssetMetadata(
            nftUid = NftUid.Evm(blockchainType, asset.contract.address, asset.tokenId),
            providerCollectionUid = asset.collectionUid,
            name = asset.name,
            imageUrl = asset.imageUrl,
            previewImageUrl = asset.imagePreviewUrl,
            description = asset.description,
            nftType = asset.contract.schemaName,
            externalLink = asset.externalLink,
            providerLink = asset.permalink,
            traits = asset.traits.map { Trait(it.traitType, it.value, it.count, traitSearchUrl(it.traitType, it.value, providerCollectionUid)) },
            lastSalePrice = asset.lastSalePrice,
            offers = listOf(), // TODO
            saleInfo = null // TODO
        )
    }

    private fun traitSearchUrl(type: String, value: String, collectionUid: String): String {
        return "https://opensea.io/assets/${collectionUid}?search[stringTraits][0][name]=${type}" +
                "&search[stringTraits][0][values][0]=${value}" +
                "&search[sortAscending]=true&search[sortBy]=PRICE"
    }

    private fun collectionMetadata(collection: NftCollection, blockchainType: BlockchainType): NftCollectionMetadata {
        return NftCollectionMetadata(
            blockchainType = blockchainType,
            providerUid = collection.uid,
            contracts = collection.asset_contracts?.map { it.address } ?: listOf(),
            name = collection.name,
            description = collection.description,
            imageUrl = collection.featuredImageUrl,
            thumbnailImageUrl = collection.imageUrl,
            externalUrl = collection.externalUrl,
            discordUrl = collection.discordUrl,
            twitterUserName = collection.twitterUsername,
            count = collection.stats.count,
            ownerCount = collection.stats.ownersCount,
            totalSupply = collection.stats.totalSupply,
            totalVolume = collection.stats.totalVolume,
            floorPrice = collection.stats.floorPrice,
            marketCap = collection.stats.marketCap,
            stats1d = NftCollectionMetadata.Stats(
                volume = collection.stats.volumes[HsTimePeriod.Day1],
                change = collection.stats.changes[HsTimePeriod.Day1],
                sales = collection.stats.sales[HsTimePeriod.Day1],
                averagePrice = collection.stats.averagePrice1d
            ),
            stats7d = NftCollectionMetadata.Stats(
                volume = collection.stats.volumes[HsTimePeriod.Week1],
                change = collection.stats.changes[HsTimePeriod.Week1],
                sales = collection.stats.sales[HsTimePeriod.Week1],
                averagePrice = collection.stats.averagePrice7d
            ),
            stats30d = NftCollectionMetadata.Stats(
                volume = collection.stats.volumes[HsTimePeriod.Month1],
                change = collection.stats.changes[HsTimePeriod.Month1],
                sales = collection.stats.sales[HsTimePeriod.Month1],
                averagePrice = collection.stats.averagePrice30d
            )
        )
    }

}