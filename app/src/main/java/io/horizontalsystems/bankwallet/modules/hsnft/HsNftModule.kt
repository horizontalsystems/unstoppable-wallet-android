package io.horizontalsystems.bankwallet.modules.hsnft

import io.horizontalsystems.bankwallet.core.App
import io.horizontalsystems.bankwallet.core.managers.APIClient
import io.horizontalsystems.bankwallet.entities.Account
import io.horizontalsystems.bankwallet.entities.Address
import io.horizontalsystems.bankwallet.modules.nft.*
import io.horizontalsystems.marketkit.models.CoinType
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

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
        val links: Links?,
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
            val floor_price: BigDecimal?,
            val total_supply: Int,
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
            val permalink: String,
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
            val sell_orders: List<Order>?,
            val orders: List<Order>?
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

            data class Order(
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
                    val eth_price: BigDecimal
                )
            }
        }
    }
}

data class CollectionStats(
    val averagePrice7d: NftAssetPrice?,
    val averagePrice30d: NftAssetPrice?,
    val floorPrice: NftAssetPrice?,
)

data class AssetOrder(
    val closingDate: Date?,
    val price: NftAssetPrice?,
    val emptyTaker: Boolean,
    val side: Int,
    val v: Int?,
    val ethValue: BigDecimal
)

class HsNftApiProvider : INftApiProvider {
    private val zeroAddress = "0x0000000000000000000000000000000000000000"

    override suspend fun getCollectionRecords(
        address: Address,
        account: Account
    ) = fetchCollections(address).map { collectionResponse ->
        NftCollectionRecord(
            accountId = account.id,
            uid = collectionResponse.uid,
            name = collectionResponse.name,
            imageUrl = collectionResponse.image_data?.image_url,
            totalSupply = collectionResponse.stats.total_supply,
            averagePrice7d = NftAssetPrice(
                getCoinTypeId(zeroAddress),
                collectionResponse.stats.seven_day_average_price
            ),
            averagePrice30d = NftAssetPrice(
                getCoinTypeId(zeroAddress),
                collectionResponse.stats.thirty_day_average_price
            ),
            floorPrice = collectionResponse.stats.floor_price?.let {
                NftAssetPrice(getCoinTypeId(zeroAddress), it)
            },
            links = collectionResponse.links
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
                NftAssetPrice(
                    getCoinTypeId(last_sale.payment_token.address),
                    BigDecimal(last_sale.total_price).movePointLeft(last_sale.payment_token.decimals)
                )
            },
            contract = NftAssetContract(
                assetResponse.contract.address,
                assetResponse.contract.type
            ),
            links = assetResponse.links,
            attributes = assetResponse.attributes.map { NftAssetAttribute(it.trait_type, it.value, it.trait_count) }
        )
    }

    override suspend fun collectionStats(collectionUid: String): CollectionStats {
        return HsNftModule.apiServiceV1.collectionStats(collectionUid).let { stats ->
            CollectionStats(
                averagePrice7d = NftAssetPrice(
                    getCoinTypeId(zeroAddress),
                    stats.seven_day_average_price
                ),
                averagePrice30d = NftAssetPrice(
                    getCoinTypeId(zeroAddress),
                    stats.thirty_day_average_price
                ),
                floorPrice = stats.floor_price?.let {
                    NftAssetPrice(getCoinTypeId(zeroAddress), it)
                }
            )
        }
    }

    override suspend fun assetOrders(contractAddress: String, tokenId: String): List<AssetOrder> {
        return HsNftModule.apiServiceV1.asset(contractAddress, tokenId).let { asset ->
            asset.markets_data.orders?.let { orders ->
                orders.map { order ->
                    val price = order.current_price.movePointLeft(order.payment_token_contract.decimals)

                    val closingDate = try {
                        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                            timeZone = TimeZone.getTimeZone("GMT")
                        }
                        sdf.parse(order.closing_date)
                    } catch (ex: Exception) {
                        null
                    }
                    AssetOrder(
                        closingDate = closingDate,
                        price = NftAssetPrice(getCoinTypeId(order.payment_token_contract.address), price),
                        emptyTaker = order.taker.address == zeroAddress,
                        side = order.side,
                        v = order.v,
                        ethValue = price.divide(order.payment_token_contract.eth_price, RoundingMode.HALF_EVEN)
                    )
                }
            } ?: listOf()
        }
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
            zeroAddress -> CoinType.Ethereum
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
    ): List<HsNftApiV1Response.Collection>

    @GET("assets")
    suspend fun assets(
        @Query("owner") owner: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
    ): List<HsNftApiV1Response.Asset>

    @GET("collection/{uid}/stats")
    suspend fun collectionStats(
        @Path("uid") collectionUid: String
    ): HsNftApiV1Response.Collection.Stats

    @GET("asset/{contractAddress}/{tokenId}")
    suspend fun asset(
        @Path("contractAddress") contractAddress: String,
        @Path("tokenId") tokenId: String
    ): HsNftApiV1Response.Asset

}
