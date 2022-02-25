package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class NftManager(
    private val nftDao: NftDao,
    private val apiProvider: INftApiProvider
) {
    fun getCollectionAndAssets(accountId: String): Flow<Map<NftCollectionRecord, List<NftAssetRecord>>> =
        combine(
            nftDao.getCollections(accountId),
            nftDao.getAssets(accountId)
        ) { collections, assets ->
            val assetsGroupByCollection = assets.groupBy { it.collectionSlug }

            collections.map {
                val collectionAssets = assetsGroupByCollection[it.slug] ?: listOf()
                it to collectionAssets
            }.toMap()
        }

    suspend fun refresh(account: Account, address: Address) = withContext(Dispatchers.IO) {
        val collections = apiProvider.getCollectionRecords(address, account)
        val assets = apiProvider.getAssetRecords(address, account)

        nftDao.replaceCollectionAssets(account.id, collections, assets)
    }

    suspend fun getAsset(accountId: String, tokenId: String): NftAssetRecord? {
        return nftDao.getAsset(accountId, tokenId)
    }

    fun getCollection(accountId: String, collectionSlug: String): NftCollectionRecord? {
        return nftDao.getCollection(accountId, collectionSlug)
    }
}
