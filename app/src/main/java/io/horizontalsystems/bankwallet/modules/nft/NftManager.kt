package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.opensea.OpenSeaModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NftManager(private val nftDao: NftDao) {
    fun getCollections(accountId: String) = nftDao.getCollections(accountId)

    suspend fun refresh(account: Account, address: Address) = withContext(Dispatchers.IO) {
        nftDao.replaceCollections(account.id, fetchCollections(address).map { collectionRawData ->
            NftCollection(
                accountId = account.id,
                slug = collectionRawData["slug"].toString(),
                name = collectionRawData["name"].toString(),
                imageUrl = collectionRawData["image_url"].toString()
            )
        })
    }

    private suspend fun fetchCollections(address: Address): List<Map<String, Any>> {
        val collections = mutableListOf<Map<String, Any>>()

        var offset = 0
        val limit = 300
        do {
            val elements = OpenSeaModule.apiServiceV1.collections(address.hex, offset, limit)
            collections.addAll(elements)
            offset += limit
        } while (elements.size >= limit)

        return collections
    }
}
