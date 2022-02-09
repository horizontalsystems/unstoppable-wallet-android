package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.opensea.OpenSeaModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NftManager(private val nftDao: NftDao) {
    fun getCollections(accountId: String) = nftDao.getCollections(accountId)

    suspend fun refresh(account: Account, address: Address) = withContext(Dispatchers.IO) {
        val collections = OpenSeaModule.apiServiceV1.collections(address.hex)

        nftDao.insertCollections(collections.map { collectionRawData ->
            NftCollection(
                accountId = account.id,
                slug = collectionRawData["slug"].toString(),
                name = collectionRawData["name"].toString(),
                imageUrl = collectionRawData["image_url"].toString()
            )
        })
    }
}
