package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.hsnft.CollectionStats
import io.horizontalsystems.bankwallet.modules.hsnft.HsNftApiV1Response
import io.horizontalsystems.bankwallet.modules.nft.asset.NftAssetModuleAssetItem
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class NftManager(
    private val nftDao: NftDao,
    private val apiProvider: INftApiProvider,
    private val coinManager: ICoinManager
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
        val collections = apiProvider.getCollectionRecords(address, account)
        val assets = apiProvider.getAssetRecords(address, account)

        nftDao.replaceCollectionAssets(account.id, collections, assets)

        map(assets, collections)
    }

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

    fun nftAssetPriceToCoinValue(nftAssetPrice: NftAssetPrice?): CoinValue? {
        if (nftAssetPrice == null) return null

        return coinManager.getPlatformCoin(CoinType.fromId(nftAssetPrice.coinTypeId))
            ?.let { platformCoin ->
                CoinValue(platformCoin, nftAssetPrice.value)
            }
    }

    suspend fun collectionStats(collectionUid: String): CollectionStats {
        return apiProvider.collectionStats(collectionUid)
    }

    suspend fun assetOrders(contractAddress: String, tokenId: String): List<AssetOrder> {
        return apiProvider.assetOrders(contractAddress, tokenId)
    }

    fun assetItem(
        assetRecord: NftAssetRecord,
        collectionName: String,
        collectionLinks: HsNftApiV1Response.Collection.Links?,
        averagePrice7d: NftAssetPrice?,
        averagePrice30d: NftAssetPrice?,
        totalSupply: Int
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
            lastSale = nftAssetPriceToCoinValue(assetRecord.lastSale)?.let { NftAssetModuleAssetItem.Price(it) },
            average7d = nftAssetPriceToCoinValue(averagePrice7d)?.let { NftAssetModuleAssetItem.Price(it) },
            average30d = nftAssetPriceToCoinValue(averagePrice30d)?.let { NftAssetModuleAssetItem.Price(it) },
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
