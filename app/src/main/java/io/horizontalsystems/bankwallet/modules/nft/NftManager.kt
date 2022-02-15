package io.horizontalsystems.bankwallet.modules.nft

import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.opensea.OpenSeaApiV1Response
import io.horizontalsystems.bankwallet.modules.opensea.OpenSeaModule
import io.horizontalsystems.marketkit.models.CoinType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.math.BigDecimal

class NftManager(private val nftDao: NftDao) {

    fun getCollectionAndAssets(accountId: String): Flow<Map<NftCollection, List<NftAsset>>> =
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
        val collections = fetchCollections(address).map { collectionResponse ->
            NftCollection(
                accountId = account.id,
                slug = collectionResponse.slug,
                name = collectionResponse.name,
                imageUrl = collectionResponse.image_url,
                stats = NftCollectionStats(
                    averagePrice7d = collectionResponse.stats.seven_day_average_price,
                    averagePrice30d = collectionResponse.stats.thirty_day_average_price
                )
            )
        }

        val assets = fetchAssets(address).map { assetResponse ->
            NftAsset(
                accountId = account.id,
                collectionSlug = assetResponse.collection.slug,
                tokenId = assetResponse.token_id,
                name = assetResponse.name,
                imageUrl = assetResponse.image_url,
                imagePreviewUrl = assetResponse.image_preview_url,
                lastSale = assetResponse.last_sale?.let { last_sale ->
                    NftAssetLastSale(
                        getCoinTypeId(last_sale.payment_token.address),
                        BigDecimal(last_sale.total_price).movePointLeft(last_sale.payment_token.decimals)
                    )
                }
            )
        }

        nftDao.replaceCollectionAssets(account.id, collections, assets)
    }

    private fun getCoinTypeId(paymentTokenAddress: String): String {
        val coinType = when (paymentTokenAddress) {
            "0x0000000000000000000000000000000000000000" -> CoinType.Ethereum
            else -> CoinType.Erc20(paymentTokenAddress.lowercase())
        }

        return coinType.id
    }

    private suspend fun fetchAssets(address: Address): List<OpenSeaApiV1Response.Asset> {
        return fetchAllWithLimit(50) { offset, limit ->
            OpenSeaModule.apiServiceV1.assets(address.hex, limit).assets
        }
    }

    private suspend fun fetchCollections(address: Address): List<OpenSeaApiV1Response.Collection> {
        return fetchAllWithLimit(300) { offset, limit ->
            OpenSeaModule.apiServiceV1.collections(address.hex, offset, limit)
        }
    }

    private suspend fun <T> fetchAllWithLimit(limit: Int, f: suspend (Int, Int) -> List<T>): List<T> {
        val assets = mutableListOf<T>()
        var offset = 0
        do {
            val elements = f.invoke(offset, limit)
            assets.addAll(elements)
            offset += limit
        } while (elements.size >= limit)

        return assets
    }
}
