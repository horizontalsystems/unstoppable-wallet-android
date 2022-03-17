package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.hsnft.CollectionStats
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

    suspend fun getAssetRecord(accountId: String, tokenId: String, contractAddress: String): NftAssetRecord? {
        return nftDao.getAsset(accountId, tokenId, contractAddress)
    }

    fun getCollectionRecord(accountId: String, collectionSlug: String): NftCollectionRecord? {
        return nftDao.getCollection(accountId, collectionSlug)
    }

    fun nftAssetPriceToCoinValue(nftAssetPrice: NftAssetPrice?): CoinValue? {
        if (nftAssetPrice == null) return null

        return coinManager.getPlatformCoin(CoinType.fromId(nftAssetPrice.coinTypeId))
            ?.let { platformCoin ->
                CoinValue(
                    CoinValue.Kind.PlatformCoin(platformCoin),
                    nftAssetPrice.value
                )
            }
    }

    suspend fun collectionStats(collectionUid: String): CollectionStats {
        return apiProvider.collectionStats(collectionUid)
    }

    suspend fun assetOrders(contractAddress: String, tokenId: String): List<AssetOrder> {
        return apiProvider.assetOrders(contractAddress, tokenId)
    }
}
