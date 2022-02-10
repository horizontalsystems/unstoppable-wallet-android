package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.opensea.OpenSeaModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NftManager(private val nftDao: NftDao) {
    fun getCollections(accountId: String) = nftDao.getCollections(accountId)
    fun getAssets(accountId: String) = nftDao.getAssets(accountId)

    suspend fun refresh(account: Account, address: Address) = withContext(Dispatchers.IO) {
        val collections = fetchCollections(address).map { collectionRawData ->
            NftCollection(
                accountId = account.id,
                slug = collectionRawData["slug"].toString(),
                name = collectionRawData["name"].toString(),
                imageUrl = collectionRawData["image_url"].toString()
            )
        }

        val assets = fetchAssets(address).map { assetRawData ->
            NftAsset(
                accountId = account.id,
                collectionSlug = (assetRawData["collection"] as? Map<String, Any>)?.get("slug").toString(),
                tokenId = assetRawData["token_id"].toString(),
                name = assetRawData["name"].toString(),
                imageUrl = assetRawData["image_url"].toString(),
                imagePreviewUrl = assetRawData["image_preview_url"].toString(),
            )
        }

        nftDao.replaceCollectionAssets(account.id, collections, assets)
    }

    private suspend fun fetchAssets(address: Address): List<Map<String, Any>> {
        return fetchAllWithLimit(50) { offset, limit ->
            val response = OpenSeaModule.apiServiceV1.assets(address.hex, limit)
            (response.get("assets") as? List<Map<String, Any>>) ?: listOf()
        }
    }

    private suspend fun fetchCollections(address: Address): List<Map<String, Any>> {
        return fetchAllWithLimit(300) { offset, limit ->
            OpenSeaModule.apiServiceV1.collections(address.hex, offset, limit)
        }
    }

    private suspend fun fetchAllWithLimit(limit: Int, f: suspend (Int, Int) -> List<Map<String, Any>>): List<Map<String, Any>> {
        val assets = mutableListOf<Map<String, Any>>()
        var offset = 0
        do {
            val elements = f.invoke(offset, limit)
            assets.addAll(elements)
            offset += limit
        } while (elements.size >= limit)

        return assets
    }
}
