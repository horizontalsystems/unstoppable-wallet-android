package io.horizontalsystems.bankwallet.modules.hsnft

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nft.*
import io.horizontalsystems.marketkit.models.CoinType
import retrofit2.http.GET
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger

object HsNftModule {
    private val apiURL = App.appConfigProvider.marketApiBaseUrl + "/v1/nft/"

    val apiServiceV1 = APIClient.retrofit(apiURL, 60)
        .create(HsNftApiV1::class.java)
}

object HsNftApiV1Response {
    data class Collection(
        val uid: String,
        val name: String,
        val description: String?,
        val asset_contracts: List<Asset.Contract>,
        val image_data: ImageData?,
        val links: Links,
        val stats: Stats,
    ) {
        data class ImageData(
            val image_url: String?,
            val featured_image_url: String?,
        )

        data class Links(
            val external_url: String?,
            val discord_url: String?,
            val telegram_url: String?,
            val twitter_username: String?,
            val instagram_username: String?,
            val wiki_url: String?,
        )

        data class Stats(
            val seven_day_average_price: BigDecimal,
            val thirty_day_average_price: BigDecimal,
            val total_supply: Int,
            val floor_price: BigDecimal,
        )
    }

    data class Asset(
        val token_id: String,
        val name: String?,
        val symbol: String,
        val contract: Contract,
        val collection_uid: String,
        val description: String?,
        val image_data: ImageData?,
        val links: Links?,
        val attributes: List<Attribute>,
        val markets_data: MarketsData,
    ) {
        data class Contract(
            val address: String,
            val type: String,
        )

        data class ImageData(
            val image_url: String?,
            val image_preview_url: String?,
        )

        data class Links(
            val external_link: String?,
            val permalink: String?,
        )

        data class Attribute(
            val trait_type: String,
            val value: String,
            val display_type: String?,
            val max_value: Any?,
            val trait_count: Int,
            val order: Any?,
        )

        data class MarketsData(
            val last_sale: LastSale?,
            val sell_orders: List<SellOrder>?,
        ) {
            data class LastSale(
                val total_price: BigInteger,
                val payment_token: PaymentToken,
            ) {
                data class PaymentToken(
                    val address: String,
                    val decimals: Int
                )
            }

            data class SellOrder(
                val closing_date: String,
                val current_price: BigDecimal,
                val payment_token_contract: PaymentTokenContract,
                val taker: Taker,
                val side: Int,
                val v: Int?,
            ) {
                data class Taker(
                    val address: String
                )

                data class PaymentTokenContract(
                    val address: String,
                    val decimals: Int,
                    val eth_price: BigDecimal,

                )
            }
        }
    }
}

class HsNftApiProvider : INftApiProvider {
    override suspend fun getCollectionRecords(
        address: Address,
        account: Account
    ) = fetchCollections(address).map { collectionResponse ->
        NftCollectionRecord(
            accountId = account.id,
            uid = collectionResponse.uid,
            name = collectionResponse.name,
            imageUrl = collectionResponse.image_data?.image_url,
            stats = NftCollectionStats(
                averagePrice7d = collectionResponse.stats.seven_day_average_price,
                averagePrice30d = collectionResponse.stats.thirty_day_average_price
            )
        )
    }

    override suspend fun getAssetRecords(
        address: Address,
        account: Account
    ) = fetchAssets(address).map { assetResponse ->
        NftAssetRecord(
            accountId = account.id,
            collectionUid = assetResponse.collection_uid,
            tokenId = assetResponse.token_id,
            name = assetResponse.name,
            imageUrl = assetResponse.image_data?.image_url,
            imagePreviewUrl = assetResponse.image_data?.image_preview_url,
            description = assetResponse.description,
            onSale = assetResponse.markets_data.sell_orders?.isNotEmpty() ?: false,
            lastSale = assetResponse.markets_data.last_sale?.let { last_sale ->
                NftAssetLastSale(
                    getCoinTypeId(last_sale.payment_token.address),
                    BigDecimal(last_sale.total_price).movePointLeft(last_sale.payment_token.decimals)
                )
            },
            contract = NftAssetContract(
                assetResponse.contract.address,
                assetResponse.contract.type
            )
        )
    }

    private suspend fun fetchAssets(address: Address): List<HsNftApiV1Response.Asset> {
        return fetchAllWithLimit(50) { offset, limit ->
            HsNftModule.apiServiceV1.assets(address.hex, offset, limit)
        }
    }

    private suspend fun fetchCollections(address: Address): List<HsNftApiV1Response.Collection> {
        return fetchAllWithLimit(300) { offset, limit ->
            HsNftModule.apiServiceV1.collections(address.hex, offset, limit)
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

    private fun getCoinTypeId(paymentTokenAddress: String): String {
        val coinType = when (paymentTokenAddress) {
            "0x0000000000000000000000000000000000000000" -> CoinType.Ethereum
            else -> CoinType.Erc20(paymentTokenAddress.lowercase())
        }

        return coinType.id
    }
}

interface HsNftApiV1 {

    @GET("collections")
    suspend fun collections(
        @Query("asset_owner") assetOwner: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int
    ) : List<HsNftApiV1Response.Collection>

    @GET("assets")
    suspend fun assets(
        @Query("owner") owner: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ) : List<HsNftApiV1Response.Asset>

}
