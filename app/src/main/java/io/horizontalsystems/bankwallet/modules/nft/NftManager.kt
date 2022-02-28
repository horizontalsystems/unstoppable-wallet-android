package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.core.ICoinManager
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.entities.CoinValue
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class NftManager(
    private val nftDao: NftDao,
    private val apiProvider: INftApiProvider,
    private val coinManager: ICoinManager
) {
    fun getCollectionAndAssets(accountId: String): Flow<Map<NftCollectionRecord, List<NftAssetRecord>>> =
        combine(
            nftDao.getCollections(accountId),
            nftDao.getAssets(accountId)
        ) { collections, assets ->
            val assetsGroupByCollection = assets.groupBy { it.collectionUid }

            collections.map {
                val collectionAssets = assetsGroupByCollection[it.uid] ?: listOf()
                it to collectionAssets
            }.toMap()
        }

    suspend fun refresh(account: Account, address: Address) = withContext(Dispatchers.IO) {
        val collections = apiProvider.getCollectionRecords(address, account)
        val assets = apiProvider.getAssetRecords(address, account)

        nftDao.replaceCollectionAssets(account.id, collections, assets)
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
}
