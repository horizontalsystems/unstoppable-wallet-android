package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.hsnft.AssetOrder
import io.horizontalsystems.bankwallet.modules.hsnft.CollectionStats
import java.math.BigDecimal

interface INftApiProvider {
    suspend fun getCollectionRecords(address: Address, account: Account): List<NftCollectionRecord>
    suspend fun getAssetRecords(address: Address, account: Account): List<NftAssetRecord>
    suspend fun collectionStats(collectionUid: String): CollectionStats
    suspend fun assetOrders(contractAddress: String, tokenId: String): List<AssetOrder>

    suspend fun topCollections(count: Int): List<TopNftCollection>
}

data class TopNftCollection(
    val uid: String,
    val name: String,
    val imageUrl: String?,
    val floorPrice: BigDecimal?,
    val totalVolume: BigDecimal,
    val oneDayVolume: BigDecimal,
    val oneDayVolumeDiff: BigDecimal,
    val sevenDayVolume: BigDecimal,
    val sevenDayVolumeDiff: BigDecimal,
    val thirtyDayVolume: BigDecimal,
    val thirtyDayVolumeDiff: BigDecimal
)
