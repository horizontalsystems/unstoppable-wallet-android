package io.horizontalsystems.bankwallet.modules.nft
/*
import io.horizontalsystems.bankwallet.core.managers.MarketKitWrapper
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.bankwallet.modules.nft.asset.nftAssetAttribute
import io.horizontalsystems.marketkit.models.NftAsset
import io.horizontalsystems.marketkit.models.NftCollection
import io.horizontalsystems.marketkit.models.TokenQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class NftManager(
    private val nftDao: NftDao,
    private val marketKit: MarketKitWrapper
) {
    suspend fun getCollectionAndAssetsFromCache(accountId: String): Map<NftCollectionRecord, List<NftAssetRecord>> {
        val collections = nftDao.getCollections(accountId)
        val assets = nftDao.getAssets(accountId)

        return map(assets, collections)
    }

    suspend fun getCollectionAndAssetsFromApi(
        account: Account,
        address: Address
    ): Map<NftCollectionRecord, List<NftAssetRecord>> = withContext(Dispatchers.IO) {
        val assetCollections = marketKit.nftAssetCollection(address.hex)
        val assets = assetCollections.assets.map { collectionAsset(it, account) }
        val collections = assetCollections.collections.map { collectionRecord(it, account) }

        nftDao.replaceCollectionAssets(account.id, collections, assets)

        map(assets, collections)
    }

    fun nftAssetPriceToCoinValue(nftPriceRecord: NftPriceRecord?): CoinValue? {
        if (nftPriceRecord == null) return null
        val tokenQuery = TokenQuery.fromId(nftPriceRecord.tokenQueryId) ?: return null
        val token = marketKit.token(tokenQuery) ?: return null

        return CoinValue(token, nftPriceRecord.value)
    }

    fun assetItem(
        assetRecord: NftAssetRecord,
        collectionName: String,
        collectionLinks: CollectionLinks?,
        totalSupply: Int,
        averagePrice7d: NftPriceRecord? = null,
        averagePrice30d: NftPriceRecord? = null,
        floorPrice: NftPriceRecord? = null,
        bestOffer: NftPriceRecord? = null,
        sale: NftAssetModuleAssetItem.Sale? = null
    ) = NftAssetModuleAssetItem(
        name = assetRecord.name,
        imageUrl = assetRecord.imageUrl,
        collectionName = collectionName,
        collectionUid = assetRecord.collectionUid,
        description = assetRecord.description,
        contract = assetRecord.contract,
        tokenId = assetRecord.tokenId,
        assetLinks = assetRecord.links,
        collectionLinks = collectionLinks,
        stats = NftAssetModuleAssetItem.Stats(
            lastSale = priceItem(assetRecord.lastSale),
            average7d = priceItem(averagePrice7d),
            average30d = priceItem(averagePrice30d),
            collectionFloor = priceItem(floorPrice),
            bestOffer = priceItem(bestOffer),
            sale = sale
        ),
        onSale = assetRecord.onSale,
        attributes = assetRecord.attributes.map { attribute ->
            NftAssetModuleAssetItem.Attribute(
                attribute.type,
                attribute.value,
                getAttributePercentage(attribute, totalSupply)?.let { "$it%" },
                getAttributeSearchUrl(attribute, assetRecord.collectionUid)
            )
        }
    )

    fun assetItem(
        asset: NftAsset,
        collectionName: String,
        collectionLinks: CollectionLinks?,
        totalSupply: Int,
        averagePrice7d: NftPriceRecord? = null,
        averagePrice30d: NftPriceRecord? = null,
        floorPrice: NftPriceRecord? = null,
        bestOffer: NftPriceRecord? = null,
        sale: NftAssetModuleAssetItem.Sale? = null
    ) = NftAssetModuleAssetItem(
        name = asset.name,
        imageUrl = asset.imageUrl,
        collectionName = collectionName,
        collectionUid = asset.collectionUid,
        description = asset.description,
        contract = NftAssetContract(asset.contract.address, asset.contract.schemaName),
        tokenId = asset.tokenId,
        assetLinks = AssetLinks(
            asset.externalLink, asset.permalink ?: ""
        ),
        collectionLinks = collectionLinks,
        stats = NftAssetModuleAssetItem.Stats(
            lastSale = priceItem(asset.lastSalePrice?.nftPriceRecord),
            average7d = priceItem(averagePrice7d),
            average30d = priceItem(averagePrice30d),
            collectionFloor = priceItem(floorPrice),
            bestOffer = priceItem(bestOffer),
            sale = sale
        ),
        onSale = asset.onSale,
        attributes = asset.traits.map { it.nftAssetAttribute }.map { attribute ->
            NftAssetModuleAssetItem.Attribute(
                attribute.type,
                attribute.value,
                getAttributePercentage(attribute, totalSupply)?.let { "$it%" },
                getAttributeSearchUrl(attribute, asset.collectionUid)
            )
        }
    )

    private fun collectionRecord(nftCollection: NftCollection, account: Account): NftCollectionRecord =
        NftCollectionRecord(
            blockchainType = BlockchainType.Ethereum,
            accountId = account.id,
            uid = nftCollection.uid,
            name = nftCollection.name,
            imageUrl = nftCollection.imageUrl,
            totalSupply = nftCollection.stats.totalSupply,
            averagePrice7d = nftCollection.stats.averagePrice7d?.nftPriceRecord,
            averagePrice30d = nftCollection.stats.averagePrice30d?.nftPriceRecord,
            floorPrice = nftCollection.stats.floorPrice?.nftPriceRecord,
            links = CollectionLinks(nftCollection.externalUrl, nftCollection.discordUrl, nftCollection.twitterUsername)
        )

    private fun collectionAsset(asset: NftAsset, account: Account): NftAssetRecord =
        NftAssetRecord(
            account.id,
            asset.collectionUid,
            asset.tokenId,
            asset.name,
            asset.imageUrl,
            asset.imagePreviewUrl,
            asset.description,
            asset.onSale,
            asset.lastSalePrice?.nftPriceRecord,
            NftAssetContract(asset.contract.address, asset.contract.schemaName),
            AssetLinks(asset.externalLink, asset.permalink ?: ""),
            asset.traits.map { it.nftAssetAttribute }
        )

    private fun map(
        assets: List<NftAssetRecord>,
        collections: List<NftCollectionRecord>
    ): Map<NftCollectionRecord, List<NftAssetRecord>> {
        val assetsGroupByCollection = assets.groupBy { it.collectionUid }

        return collections.associateWith {
            val collectionAssets = assetsGroupByCollection[it.uid] ?: listOf()
            collectionAssets
        }.toSortedMap { o1, o2 -> o1.name.compareTo(o2.name, ignoreCase = true) }
    }

    private fun priceItem(price: NftPriceRecord?) =
        nftAssetPriceToCoinValue(price)?.let { NftAssetModuleAssetItem.Price(it) }

    private fun getAttributeSearchUrl(attribute: NftAssetAttribute, collectionUid: String): String {
        return "https://opensea.io/assets/${collectionUid}?search[stringTraits][0][name]=${attribute.type}" +
                "&search[stringTraits][0][values][0]=${attribute.value}" +
                "&search[sortAscending]=true&search[sortBy]=PRICE"
    }

    private fun getAttributePercentage(attribute: NftAssetAttribute, totalSupply: Int): Number? =
        if (attribute.count > 0 && totalSupply > 0) {
            val percent = (attribute.count * 100f / totalSupply)
            when {
                percent >= 10 -> percent.roundToInt()
                percent >= 1 -> (percent * 10).roundToInt() / 10f
                else -> (percent * 100).roundToInt() / 100f
            }
        } else {
            null
        }
}
*/
