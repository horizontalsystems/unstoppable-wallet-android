package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.hsnft.CollectionStats

interface INftApiProvider {
    suspend fun getCollectionRecords(address: Address, account: Account): List<NftCollectionRecord>
    suspend fun getAssetRecords(address: Address, account: Account): List<NftAssetRecord>
    suspend fun collectionStats(collectionUid: String): CollectionStats
    suspend fun assetOrders(contractAddress: String, tokenId: String): List<AssetOrder>
}
